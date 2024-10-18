package cc.reconnected.discordbridge.discord;

import cc.reconnected.discordbridge.Colors;
import cc.reconnected.discordbridge.events.DiscordMessageEvents;
import cc.reconnected.discordbridge.Bridge;
import cc.reconnected.discordbridge.ChatComponents;
import cc.reconnected.discordbridge.parser.MarkdownParser;
import cc.reconnected.discordbridge.parser.MentionNodeParser;
import cc.reconnected.server.database.PlayerData;
import eu.pb4.placeholders.api.parsers.NodeParser;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Events {
    private final HashMap<String, String> messageCache = new HashMap<>();

    private boolean isActuallyEdited(String id, String content) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Bridge.LOGGER.error("sha256 no longer exists :(", e);
            return true;
        }

        var digest = new String(messageDigest.digest(content.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

        if (!messageCache.containsKey(id)) {
            messageCache.put(id, digest);
            return false;
        }

        if (!messageCache.get(id).equals(content)) {
            messageCache.put(id, content);
            return true;
        }

        return false;
    }

    public void onMessageCreate(MessageReceivedEvent event) {
        var message = event.getMessage();
        var channel = message.getChannel();
        if (!channel.getId().equals(Bridge.CONFIG.channelId()))
            return;

        var member = event.getMember();
        if (member == null)
            return;

        if (member.getUser().isBot())
            return;

        buildMessage(message, member, false);
    }

    public void onMessageEdit(MessageUpdateEvent event) {
        var message = event.getMessage();
        var channel = message.getChannel();
        if (!channel.getId().equals(Bridge.CONFIG.channelId()))
            return;

        var member = event.getMember();
        if (member == null)
            return;

        if (member.getUser().isBot())
            return;

        buildMessage(message, member, true);
    }

    public void buildMessage(Message message, Member member, boolean isEdited) {
        var isActuallyEdited = isActuallyEdited(message.getId(), message.getContentRaw());
        if (isEdited && !isActuallyEdited) {
            return;
        }
        isEdited = isActuallyEdited;

        if (isEdited) {
            DiscordMessageEvents.MESSAGE_EDIT.invoker().onEdit(message, member);
        } else {
            DiscordMessageEvents.MESSAGE_CREATE.invoker().onCreate(message, member);
        }

        int memberColor = NamedTextColor.WHITE.value();

        var nullableMemberColor = member.getColor();
        if (nullableMemberColor != null) {
            memberColor = nullableMemberColor.getRGB();
        }
        var memberComponent = ChatComponents.makeUser(member.getEffectiveName(), member.getAsMention() + ": ", memberColor, Component.empty());
        Component replyComponent = null;

        if (message.getType() == MessageType.INLINE_REPLY && message.getReferencedMessage() != null) {
            var referencedMessage = message.getReferencedMessage();
            Component referenceMemberComponent;
            var referenceMember = referencedMessage.getMember();
            if (referenceMember != null) {
                var referenceMemberColor = NamedTextColor.WHITE.value();
                var nullableReferenceMemberColor = referenceMember.getColor();
                if (nullableReferenceMemberColor != null) {
                    referenceMemberColor = nullableReferenceMemberColor.getRGB();
                }
                referenceMemberComponent = ChatComponents.makeUser(referenceMember.getEffectiveName(), referenceMember.getAsMention() + ": ", referenceMemberColor, Component.empty());
            } else if (referencedMessage.getMember() != null) {
                var referenceAuthor = referencedMessage.getAuthor();
                referenceMemberComponent = ChatComponents.makeUser(referenceAuthor.getName(), referenceAuthor.getAsMention() + ": ", NamedTextColor.WHITE.value(), Component.empty());
            } else {
                //var referenceData = referencedMessage();
                var referenceAuthor = referencedMessage.getAuthor();
                referenceMemberComponent = ChatComponents.makeUser(referenceAuthor.getName(), referenceAuthor.getName() + ": ", NamedTextColor.WHITE.value(), Component.empty());
            }

            replyComponent = ChatComponents.makeReplyHeader(referenceMemberComponent, Component.text(referencedMessage.getContentDisplay()));
        }

        var messageContent = message.getContentRaw();
        Component messageComponent = Component.empty();

        var parser = NodeParser.merge(new MentionNodeParser(message), MarkdownParser.contentParser);
        var mdContentVan = parser.parseNode(messageContent).toText();

        var json = Text.Serializer.toJson(mdContentVan);
        var mdContent = JSONComponentSerializer.json().deserialize(json);

        messageComponent = messageComponent.append(mdContent);

        //messageComponent = MentionNodeParser.parseMentions(message, messageContent, messageComponent);

        var attachments = message.getAttachments();
        if (!messageContent.isEmpty()) {
            messageComponent = messageComponent.appendSpace();
        }
        for (var attachment : attachments) {
            messageComponent = messageComponent.append(ChatComponents.makeAttachment(attachment.getFileName(), attachment.getUrl()));
            messageComponent = messageComponent.appendSpace();
        }

        var outputComponent = ChatComponents.makeMessage(memberComponent, replyComponent, messageComponent);

        if (isEdited) {
            outputComponent = outputComponent.append(Component.text("(edited)", NamedTextColor.GRAY));
        }

        Bridge.enqueueMessage(outputComponent);
    }


    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("link")) {
            return;
        }
        var codeOption = event.getOption("code");
        if (codeOption == null) {
            event.reply("Please provide a link code via the `/discord link` command in-game.")
                    .setEphemeral(true).queue();
            return;
        }

        var code = codeOption.getAsString();

        if (!Bridge.linkCodes.containsKey(code)) {
            event.reply("Code not found! Run the `/discord link` command in-game to obtain a link code.")
                    .setEphemeral(true).queue();
            return;
        }

        var player = Bridge.linkCodes.get(code);
        var playerData = PlayerData.getPlayer(player);

        Bridge.discordLinks.put(event.getUser().getId(), player.getUuid());
        playerData.set(PlayerData.KEYS.discordId, event.getUser().getId()).join();

        Bridge.getInstance().saveData();

        var client = Bridge.getInstance().getClient();
        var member = event.getMember();
        if (client.role() != null) {
            try {
                client.guild().addRoleToMember(member, client.role()).reason("Linked via link code").queue();
            } catch (InsufficientPermissionException e) {
                Bridge.LOGGER.error("Could not add role to player", e);
            }
        }

        Bridge.linkCodes.remove(code);

        event.reply("Your Discord profile is now linked with **" + playerData.getUsername() + "**!")
                .setEphemeral(true).queue();


        var text = Component.empty()
                .append(Component.text("You linked your profile to "))
                .append(Component.text(member.getEffectiveName())
                        .color(Colors.BLURPLE))
                .append(Component.text(" on Discord!"))
                .color(NamedTextColor.GREEN);

        player.sendMessage(text);
    }
}

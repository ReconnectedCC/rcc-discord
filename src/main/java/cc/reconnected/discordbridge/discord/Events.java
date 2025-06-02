package cc.reconnected.discordbridge.discord;

import cc.reconnected.discordbridge.Colors;
import cc.reconnected.discordbridge.RccDiscord;
import cc.reconnected.discordbridge.events.DiscordMessageEvents;
import cc.reconnected.discordbridge.ChatComponents;
import cc.reconnected.discordbridge.parser.MentionNodeParser;
import cc.reconnected.library.RccLibrary;
import cc.reconnected.library.data.PlayerMeta;
import cc.reconnected.library.text.parser.MarkdownParser;
import eu.pb4.placeholders.api.parsers.NodeParser;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.luckperms.api.node.Node;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class Events {
    private final HashMap<String, String> messageCache = new HashMap<>();

    private boolean isActuallyEdited(String id, String content) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            RccDiscord.LOGGER.error("sha256 no longer exists :(", e);
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
        if (!channel.getId().equals(RccDiscord.CONFIG.channelId))
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
        if (!channel.getId().equals(RccDiscord.CONFIG.channelId))
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

        var parser = NodeParser.merge(new MentionNodeParser(message), MarkdownParser.defaultParser);
        var mdContentVan = parser.parseNode(messageContent).toText();

        var json = Text.Serializer.toJson(mdContentVan);
        var mdContent = JSONComponentSerializer.json().deserialize(json);

        messageComponent = messageComponent.append(mdContent);

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

        RccDiscord.enqueueMessage(outputComponent);
    }


    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "link" -> onLinkCommand(event);
            case "list" -> onListCommand(event);
        }

    }

    private void onLinkCommand(SlashCommandInteractionEvent event) {
        var codeOption = event.getOption("code");
        if (codeOption == null) {
            event.reply("Please provide a link code via the `/discord link` command in-game.")
                    .setEphemeral(true).queue();
            return;
        }

        var code = codeOption.getAsString();

        if (!RccDiscord.linkCodes.containsKey(code)) {
            event.reply("Code not found! Run the `/discord link` command in-game to obtain a link code.")
                    .setEphemeral(true).queue();
            return;
        }

        var playerUuid = RccDiscord.linkCodes.get(code);
        var playerOpt = RccDiscord.getInstance().getPlayer(playerUuid);
        if (playerOpt.isEmpty()) {
            event.reply("You must be online to link your Discord profile!")
                    .setEphemeral(true).queue();
            return;
        }

        var player = playerOpt.get();

        var playerData = PlayerMeta.getPlayer(player);

        RccDiscord.discordLinks.put(event.getUser().getId(), playerUuid);
        playerData.set(PlayerMeta.KEYS.discordId, event.getUser().getId()).join();

        RccDiscord.getInstance().saveData();

        var client = RccDiscord.getInstance().getClient();
        var member = event.getMember();

        // Add the role
        if (client.role() != null) {
            try {
                client.guild().addRoleToMember(member, client.role()).reason("Linked via link code").queue();
            } catch (Exception e) {
                RccDiscord.LOGGER.error("Could not add role to player", e);
            }
        }

        // Modify the username
        try {
            member.modifyNickname(playerData.getUsername()).reason("Linked via link code").queue();
        } catch(Exception e) {
            RccDiscord.LOGGER.error("Could not modify nickname", e);
        }

        // Give the permission node to the MC player
        var luckperms = RccLibrary.getInstance().luckPerms();
        luckperms.getUserManager().modifyUser(playerUuid, user -> {
            user.data().add(Node.builder(RccDiscord.CONFIG.linkedPermissionNode).build());
        });

        RccDiscord.linkCodes.remove(code);

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

    private void onListCommand(SlashCommandInteractionEvent event) {
        var list = RccDiscord.getInstance().getPlayerNames();
        String players;
        if (list.length == 0) {
            players = "*There are no players online*";
        } else {
            players = "**Online players**: " + String.join(", ", list);
        }
        event.reply(players).setEphemeral(true).queue();
    }
}

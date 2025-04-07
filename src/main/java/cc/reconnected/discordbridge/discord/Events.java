package cc.reconnected.discordbridge.discord;

import cc.reconnected.discordbridge.Colors;
import cc.reconnected.discordbridge.RccDiscord;
import cc.reconnected.discordbridge.events.DiscordMessageEvents;
import cc.reconnected.discordbridge.ChatComponents;
import cc.reconnected.discordbridge.parser.MentionNodeParser;
import cc.reconnected.library.data.PlayerMeta;
import cc.reconnected.library.text.parser.MarkdownParser;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.GuildMemberEditSpec;
import discord4j.discordjson.possible.Possible;
import eu.pb4.placeholders.api.parsers.NodeParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Optional;

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

    public void onMessageCreate(MessageCreateEvent event) {
        var message = event.getMessage();
        var channel = message.getChannel().block();
        if (!channel.getId().equals(RccDiscord.CONFIG.channelId))
            return;

        var memberOpt = event.getMember();
        if (memberOpt.isEmpty())
            return;

        var member = memberOpt.get();
        if (member.isBot())
            return;

        buildMessage(message, member, false);
    }

    public void onMessageEdit(MessageUpdateEvent event) {
        var message = event.getMessage().block();
        var channel = message.getChannel().block();
        if (!channel.getId().equals(RccDiscord.CONFIG.channelId))
            return;

        var memberOpt = message.getAuthorAsMember().blockOptional();
        if (memberOpt.isEmpty())
            return;

        var member = memberOpt.get();
        if (member.isBot())
            return;

        buildMessage(message, member, true);
    }

    public void buildMessage(Message message, Member member, boolean isEdited) {
        var isActuallyEdited = isActuallyEdited(message.getId().asString(), message.getContent());
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

        var nullableMemberColor = member.getColor().block();
        if (nullableMemberColor.getRGB() != 0) {
            memberColor = nullableMemberColor.getRGB();
        }
        var memberComponent = ChatComponents.makeUser(member.getDisplayName(), member.getMention() + ": ", memberColor, Component.empty());
        Component replyComponent = null;

        if (message.getType() == Message.Type.REPLY && message.getReferencedMessage().isPresent()) {
            var referencedMessage = message.getReferencedMessage();
            Component referenceMemberComponent;
            var referenceMember = referencedMessage.get().getAuthorAsMember().blockOptional();
            if (referenceMember.isPresent()) {
                var referenceMemberColor = NamedTextColor.WHITE.value();
                var nullableReferenceMemberColor = referenceMember.get().getColor().block();
                if (nullableReferenceMemberColor.getRGB() != 0) {
                    referenceMemberColor = nullableReferenceMemberColor.getRGB();
                }
                referenceMemberComponent = ChatComponents.makeUser(referenceMember.get().getDisplayName(), referenceMember.get().getMention() + ": ", referenceMemberColor, Component.empty());
            } else if (referencedMessage.get().getAuthorAsMember().blockOptional().isPresent()) {
                var referenceAuthor = referencedMessage.get().getAuthorAsMember().block();
                referenceMemberComponent = ChatComponents.makeUser(referenceAuthor.getDisplayName(), referenceAuthor.getMention() + ": ", NamedTextColor.WHITE.value(), Component.empty());
            } else {
                //var referenceData = referencedMessage();
                var referenceAuthor = referencedMessage.get().getAuthor().get();
                referenceMemberComponent = ChatComponents.makeUser(referenceAuthor.getUsername(), referenceAuthor.getUsername() + ": ", NamedTextColor.WHITE.value(), Component.empty());
            }

            replyComponent = ChatComponents.makeReplyHeader(referenceMemberComponent, Component.text(referencedMessage.get().getContent()));
        }

        var messageContent = message.getContent();
        Component messageComponent = Component.empty();

        var parser = NodeParser.merge(new MentionNodeParser(message), MarkdownParser.defaultParser);
        var mdContentVan = parser.parseNode(messageContent).toText();

        var json = Text.Serialization.toJsonString(mdContentVan, RccDiscord.getInstance().getServer().getRegistryManager());
        var mdContent = JSONComponentSerializer.json().deserialize(json);

        messageComponent = messageComponent.append(mdContent);

        var attachments = message.getAttachments();
        if (!messageContent.isEmpty()) {
            messageComponent = messageComponent.appendSpace();
        }
        for (var attachment : attachments) {
            messageComponent = messageComponent.append(ChatComponents.makeAttachment(attachment.getFilename(), attachment.getUrl()));
            messageComponent = messageComponent.appendSpace();
        }

        var outputComponent = ChatComponents.makeMessage(memberComponent, replyComponent, messageComponent);

        if (isEdited) {
            outputComponent = outputComponent.append(Component.text("(edited)", NamedTextColor.GRAY));
        }

        RccDiscord.enqueueMessage(outputComponent);
    }


    public void onChatInputInteraction(ChatInputInteractionEvent event) {
        switch (event.getCommandName()) {
            case "link" -> onLinkCommand(event);
            case "list" -> onListCommand(event);
        }

    }

    private void onLinkCommand(ChatInputInteractionEvent event) {
        var codeOption = event.getOption("code");
        if (codeOption.isEmpty()) {
            event.reply("Please provide a link code via the `/discord link` command in-game.")
                    .withEphemeral(true).subscribe();
            return;
        }

        var code = codeOption.get().getValue().get().asString();

        if (!RccDiscord.linkCodes.containsKey(code)) {
            event.reply("Code not found! Run the `/discord link` command in-game to obtain a link code.")
                    .withEphemeral(true).subscribe();
            return;
        }

        var player = RccDiscord.linkCodes.get(code);
        var playerData = PlayerMeta.getPlayer(player);

        var interaction = event.getInteraction();
        RccDiscord.discordLinks.put(interaction.getUser().getId().asString(), player.getUuid());
        playerData.set(PlayerMeta.KEYS.discordId, interaction.getUser().getId().asString()).join();

        RccDiscord.getInstance().saveData();

        var client = RccDiscord.getInstance().getClient();
        var member = interaction.getMember().get();
        var service = client.client().getRestClient().getGuildService();
        try {
            var request = GuildMemberEditSpec.builder()
                    .addRole(client.role().getId())
                    .nickname(Possible.of(Optional.ofNullable(playerData.getUsername())))
                    .reason("Linked via link code")
                    .build();

            service.modifyGuildMember(member.getGuildId().asLong(), member.getId().asLong(), request.asRequest(), "Linked via link code")
                    .subscribe();
        } catch(Exception e) {
            RccDiscord.LOGGER.error("Could not update member", e);
        }

        RccDiscord.linkCodes.remove(code);

        event.reply("Your Discord profile is now linked with **" + playerData.getUsername() + "**!")
                .withEphemeral(true).subscribe();


        var text = Component.empty()
                .append(Component.text("You linked your profile to "))
                .append(Component.text(member.getDisplayName())
                        .color(Colors.BLURPLE))
                .append(Component.text(" on Discord!"))
                .color(NamedTextColor.GREEN);

        player.sendMessage(text);
    }

    private void onListCommand(ChatInputInteractionEvent event) {
        var list = RccDiscord.getInstance().getPlayerNames();
        String players;
        if (list.length == 0) {
            players = "*There are no players online*";
        } else {
            players = "**Online players**: " + String.join(", ", list);
        }
        event.reply(players).withEphemeral(true).subscribe();
    }
}

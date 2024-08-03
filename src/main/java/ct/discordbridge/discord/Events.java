package ct.discordbridge.discord;

import ct.discordbridge.Bridge;
import ct.discordbridge.ChatComponents;
import ct.discordbridge.Colors;
import ct.discordbridge.events.DiscordMessage;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Events {
    private static final Pattern mentionPattern = Pattern.compile("(<@[!&]?\\d+>|<#\\d+>)");
    private static final Pattern integerPattern = Pattern.compile("\\d+");
    public Events(Client client) {
        client.client().on(MessageCreateEvent.class).subscribe(this::onMessageCreate);
    }

    public static List<String> splitMessage(String message) {
        List<String> parts = new ArrayList<>();
        Matcher matcher = mentionPattern.matcher(message);

        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                parts.add(message.substring(lastEnd, matcher.start()));
            }
            parts.add(matcher.group(1));
            lastEnd = matcher.end();
        }

        if (lastEnd < message.length()) {
            parts.add(message.substring(lastEnd));
        }

        return parts;
    }

    public void onMessageCreate(MessageCreateEvent event) {
        var message = event.getMessage();
        var channel = message.getChannel().block();
        if (channel == null)
            return;

        if (!channel.getId().equals(Snowflake.of(Bridge.CONFIG.channelId())))
            return;

        if (event.getMember().isEmpty())
            return;

        var member = event.getMember().get();
        if (member.isBot())
            return;

        DiscordMessage.MESSAGE_CREATE.invoker().messageCreate(event);

        int memberColor = NamedTextColor.WHITE.value();

        var nullableMemberColor = member.getColor().block();
        if (nullableMemberColor != null) {
            memberColor = nullableMemberColor.getRGB();
        }
        var memberComponent = ChatComponents.makeUser(member.getDisplayName(), member.getMention() + ": ", memberColor, Component.empty());
        Component replyComponent = null;

        if (message.getType() == Message.Type.REPLY && message.getReferencedMessage().isPresent()) {
            var referencedMessage = message.getReferencedMessage().get();
            Component referenceMemberComponent;
            var referenceMember = referencedMessage.getAuthorAsMember().block();
            if (referenceMember != null) {
                var referenceMemberColor = NamedTextColor.WHITE.value();
                var nullableReferenceMemberColor = member.getColor().block();
                if (nullableReferenceMemberColor != null) {
                    referenceMemberColor = nullableReferenceMemberColor.getRGB();
                }
                referenceMemberComponent = ChatComponents.makeUser(referenceMember.getDisplayName(), referenceMember.getMention() + ": ", referenceMemberColor, Component.empty());
            } else if (referencedMessage.getAuthor().isPresent()) {
                var referenceAuthor = referencedMessage.getAuthor().get();
                referenceMemberComponent = ChatComponents.makeUser(referenceAuthor.getUsername(), referenceAuthor.getMention() + ": ", NamedTextColor.WHITE.value(), Component.empty());
            } else {
                var referenceData = referencedMessage.getData();
                var referenceAuthor = referenceData.author();
                referenceMemberComponent = ChatComponents.makeUser(referenceAuthor.username(), referenceAuthor.username() + ": ", NamedTextColor.WHITE.value(), Component.empty());
            }

            replyComponent = ChatComponents.makeReplyHeader(referenceMemberComponent, Component.text(referencedMessage.getContent()));
        }

        var messageContent = message.getContent();
        Component messageComponent = Component.empty();

        var splitContent = splitMessage(messageContent);
        var memberMentions = message.getMemberMentions();
        var roleMentions = message.getRoleMentions();
        for (var part : splitContent) {
            if (part.matches(mentionPattern.pattern())) {
                var matcher = integerPattern.matcher(part);
                if(matcher.find()) {
                    var snowflakeId = Snowflake.of(matcher.group());
                    if (part.startsWith("<@&")) { // Role mention
                        var mentionedRole = roleMentions.filter(p -> p.getId().equals(snowflakeId)).blockFirst();
                        if (mentionedRole != null) {
                            int color = mentionedRole.getColor().getRGB();
                            if(color == 0) {
                                color = 0x99aab5;
                            }
                            messageComponent = messageComponent.append(ChatComponents.makeUser(
                                    mentionedRole.getName(),
                                    mentionedRole.getMention() + ": ",
                                    color,
                                    ChatComponents.mentionIcon
                            ));
                        } else {
                            messageComponent = messageComponent.append(ChatComponents.makeUser(
                                    "unknown-role",
                                    String.format("<@&%s>: ", snowflakeId.asString()),
                                    NamedTextColor.WHITE.value(),
                                    ChatComponents.mentionIcon
                            ));
                        }
                    } else if (part.startsWith("<@") || part.startsWith("<@!")) { // Member mention
                        var mentionedOpt = memberMentions.stream().filter(p -> p.getId().equals(snowflakeId)).findFirst();
                        if (mentionedOpt.isPresent()) {
                            var mentioned = mentionedOpt.get();
                            messageComponent = messageComponent.append(ChatComponents.makeUser(
                                    mentioned.getDisplayName(),
                                    mentioned.getMention() + ": ",
                                    Colors.MENTION.value(),
                                    ChatComponents.mentionIcon
                            ));
                        } else {
                            messageComponent = messageComponent.append(ChatComponents.makeUser(
                                    "unknown-user",
                                    String.format("<@%s>: ", snowflakeId.asString()),
                                    Colors.MENTION.value(),
                                    ChatComponents.mentionIcon
                            ));
                        }
                    } else if (part.startsWith("<#")) { // Channel mention
                        var mentionedChannel = message.getClient().getChannelById(snowflakeId).block();
                        if (mentionedChannel != null
                                && (mentionedChannel.getType() == Channel.Type.GUILD_TEXT
                                || mentionedChannel.getType() == Channel.Type.GUILD_VOICE
                                || mentionedChannel.getType() == Channel.Type.GUILD_NEWS)) {

                            var guildChannel = (GuildChannel) mentionedChannel;
                            messageComponent = messageComponent.append(ChatComponents.makeUser(
                                    guildChannel.getName(),
                                    guildChannel.getMention() + ": ",
                                    Colors.MENTION.value(),
                                    ChatComponents.channelIcon
                            ));
                        } else {
                            messageComponent = messageComponent.append(ChatComponents.makeUser(
                                    "unknown",
                                    String.format("<#%s>: ", snowflakeId.asString()),
                                    Colors.MENTION.value(),
                                    ChatComponents.channelIcon
                            ));
                        }
                    }
                } else {
                    messageComponent = messageComponent.append(Component.text(part));
                }

            } else {
                messageComponent = messageComponent.append(Component.text(part));
            }
        }

        var attachments = message.getAttachments();
        if (!messageContent.isEmpty()) {
            messageComponent = messageComponent.appendSpace();
        }
        for (var attachment : attachments) {
            messageComponent = messageComponent.append(ChatComponents.makeAttachment(attachment.getFilename(), attachment.getUrl()));
        }

        var outputComponent = ChatComponents.makeMessage(memberComponent, replyComponent, messageComponent);

        Bridge.enqueueMessage(outputComponent);
    }
}

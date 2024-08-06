package ct.discordbridge.discord;

import ct.discordbridge.Bridge;
import ct.discordbridge.ChatComponents;
import ct.discordbridge.Colors;
import ct.discordbridge.events.DiscordMessage;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Events {
    private static final Pattern mentionPattern = Pattern.compile("(<@[!&]?\\d+>|<#\\d+>)");
    private static final Pattern integerPattern = Pattern.compile("\\d+");

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

        if (messageCache.containsKey(id)) {
            messageCache.put(id, digest);
            return false;
        }

        if(messageCache.get(id).equals(content)) {
            return false;
        }

        messageCache.put(id, content);

        return true;
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
        if(isEdited && !isActuallyEdited) {
            return;
        }
        isEdited = isActuallyEdited;

        DiscordMessage.MESSAGE_CREATE.invoker().messageCreate(message, member, isEdited);

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
                var nullableReferenceMemberColor = member.getColor();
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

        var splitContent = splitMessage(messageContent);
        var memberMentions = message.getMentions().getMembers();
        var roleMentions = message.getMentions().getRoles();
        for (var part : splitContent) {
            if (part.matches(mentionPattern.pattern())) {
                var matcher = integerPattern.matcher(part);
                if (matcher.find()) {
                    var snowflakeId = matcher.group();
                    if (part.startsWith("<@&")) { // Role mention
                        var mentionedRoleOpt = roleMentions.stream().filter(p -> p.getId().equals(snowflakeId)).findFirst();
                        if (mentionedRoleOpt.isPresent()) {
                            var mentionedRole = mentionedRoleOpt.get();
                            int color = mentionedRole.getColorRaw();
                            if (color == 0) {
                                color = 0x99aab5;
                            }
                            messageComponent = messageComponent.append(ChatComponents.makeUser(
                                    mentionedRole.getName(),
                                    mentionedRole.getAsMention() + ": ",
                                    color,
                                    ChatComponents.mentionIcon
                            ));
                        } else {
                            messageComponent = messageComponent.append(ChatComponents.makeUser(
                                    "unknown-role",
                                    String.format("<@&%s>: ", snowflakeId),
                                    NamedTextColor.WHITE.value(),
                                    ChatComponents.mentionIcon
                            ));
                        }
                    } else if (part.startsWith("<@") || part.startsWith("<@!")) { // Member mention
                        var mentionedOpt = memberMentions.stream().filter(p -> p.getId().equals(snowflakeId)).findFirst();
                        if (mentionedOpt.isPresent()) {
                            var mentioned = mentionedOpt.get();
                            messageComponent = messageComponent.append(ChatComponents.makeUser(
                                    mentioned.getEffectiveName(),
                                    mentioned.getAsMention() + ": ",
                                    Colors.MENTION.value(),
                                    ChatComponents.mentionIcon
                            ));
                        } else {
                            messageComponent = messageComponent.append(ChatComponents.makeUser(
                                    "unknown-user",
                                    String.format("<@%s>: ", snowflakeId),
                                    Colors.MENTION.value(),
                                    ChatComponents.mentionIcon
                            ));
                        }
                    } else if (part.startsWith("<#")) { // Channel mention
                        var mentionedChannel = message.getJDA().getGuildChannelById(snowflakeId);
                        if (mentionedChannel != null
                                && ChannelType.guildTypes().contains(mentionedChannel.getType())) {

                            messageComponent = messageComponent.append(ChatComponents.makeUser(
                                    mentionedChannel.getName(),
                                    mentionedChannel.getAsMention() + ": ",
                                    Colors.MENTION.value(),
                                    ChatComponents.channelIcon
                            ));
                        } else {
                            messageComponent = messageComponent.append(ChatComponents.makeUser(
                                    "unknown",
                                    String.format("<#%s>: ", snowflakeId),
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
            messageComponent = messageComponent.append(ChatComponents.makeAttachment(attachment.getFileName(), attachment.getUrl()));
            messageComponent = messageComponent.appendSpace();
        }

        var outputComponent = ChatComponents.makeMessage(memberComponent, replyComponent, messageComponent);

        if (isEdited) {
            outputComponent = outputComponent.append(Component.text("(edited)", NamedTextColor.GRAY));
        }

        Bridge.enqueueMessage(outputComponent);
    }
}

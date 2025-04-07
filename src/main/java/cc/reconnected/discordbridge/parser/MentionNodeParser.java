package cc.reconnected.discordbridge.parser;

import cc.reconnected.discordbridge.ChatComponents;
import cc.reconnected.discordbridge.Colors;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionNodeParser implements NodeParser {
    public static final Pattern mentionPattern = Pattern.compile("(<@[!&]?\\d+>|<#\\d+>)");
    public static final Pattern integerPattern = Pattern.compile("\\d+");

    private Message message;

    public MentionNodeParser(Message message) {
        this.message = message;
    }

    public List<TextNode> parseMentions() {
        var list = new ArrayList<TextNode>();

        var messageContent = message.getContent();

        var splitContent = splitMessage(messageContent);
        var memberMentions = message.getMemberMentions();
        var roleMentions = message.getRoleMentions().buffer().blockLast();
        for (var part : splitContent) {
            if (part.matches(mentionPattern.pattern())) {
                var matcher = integerPattern.matcher(part);
                if (matcher.find()) {
                    var snowflakeId = matcher.group();
                    var snowflake = Snowflake.of(snowflakeId);
                    if (part.startsWith("<@&")) { // Role mention
                        var mentionedRoleOpt = roleMentions.stream().filter(p -> p.getId().equals(snowflake)).findFirst();
                        if (mentionedRoleOpt.isPresent()) {
                            var mentionedRole = mentionedRoleOpt.get();
                            int color = mentionedRole.getColor().getRGB();
                            if (color == 0) {
                                color = 0x99aab5;
                            }

                            list.add(toTextNode(ChatComponents.makeUser(
                                    mentionedRole.getName(),
                                    mentionedRole.getMention() + ": ",
                                    color,
                                    ChatComponents.mentionIcon
                            )));
                        } else {
                            list.add(toTextNode(ChatComponents.makeUser(
                                    "unknown-role",
                                    String.format("<@&%s>: ", snowflakeId),
                                    NamedTextColor.WHITE.value(),
                                    ChatComponents.mentionIcon
                            )));
                        }
                    } else if (part.startsWith("<@") || part.startsWith("<@!")) { // Member mention
                        var mentionedOpt = memberMentions.stream().filter(p -> p.getId().equals(snowflakeId)).findFirst();
                        if (mentionedOpt.isPresent()) {
                            var mentioned = mentionedOpt.get();
                            list.add(toTextNode(ChatComponents.makeUser(
                                    mentioned.getDisplayName(),
                                    mentioned.getMention() + ": ",
                                    Colors.MENTION.value(),
                                    ChatComponents.mentionIcon
                            )));
                        } else {
                            list.add(toTextNode(ChatComponents.makeUser(
                                    "unknown-user",
                                    String.format("<@%s>: ", snowflakeId),
                                    Colors.MENTION.value(),
                                    ChatComponents.mentionIcon
                            )));
                        }
                    } else if (part.startsWith("<#")) { // Channel mention
                        var mentionedChannel = message.getGuild().block().getChannelById(Snowflake.of(snowflakeId)).blockOptional();
                        if (mentionedChannel.isPresent()) {
                            list.add(toTextNode(ChatComponents.makeUser(
                                    mentionedChannel.get().getName(),
                                    mentionedChannel.get().getMention() + ": ",
                                    Colors.MENTION.value(),
                                    ChatComponents.channelIcon
                            )));
                        } else {
                            list.add(toTextNode(ChatComponents.makeUser(
                                    "unknown",
                                    String.format("<#%s>: ", snowflakeId),
                                    Colors.MENTION.value(),
                                    ChatComponents.channelIcon
                            )));
                        }
                    }
                } else {
                    list.add(TextNode.of(part));
                }
            } else {
                list.add(TextNode.of(part));
            }
        }
        return list;
    }

    @Override
    public TextNode[] parseNodes(TextNode input) {
        return parseMentions().toArray(new TextNode[0]);
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

    private static TextNode toTextNode(Text text) {
        return TextNode.convert(text);
    }

    private static TextNode toTextNode(Component component) {
        return TextNode.convert(ChatComponents.toText(component));
    }
}

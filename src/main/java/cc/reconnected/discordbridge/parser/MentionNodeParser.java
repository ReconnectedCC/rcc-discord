package cc.reconnected.discordbridge.parser;

import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;

public class MentionNodeParser implements NodeParser {
    public static final MentionNodeParser DEFAULT = new MentionNodeParser();
    @Override
    public TextNode[] parseNodes(TextNode input) {

        return new TextNode[0];
    }
}

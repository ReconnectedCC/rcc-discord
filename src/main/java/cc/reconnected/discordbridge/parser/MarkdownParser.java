package cc.reconnected.discordbridge.parser;

import eu.pb4.placeholders.api.parsers.MarkdownLiteParserV1;
import eu.pb4.placeholders.api.parsers.NodeParser;

import static eu.pb4.placeholders.api.parsers.MarkdownLiteParserV1.MarkdownFormat;

public class MarkdownParser {
    public static final MarkdownFormat[] ALL = new MarkdownFormat[] {
            MarkdownFormat.QUOTE,
            MarkdownFormat.BOLD,
            MarkdownFormat.ITALIC,
            MarkdownFormat.UNDERLINE,
            MarkdownFormat.STRIKETHROUGH,
            MarkdownFormat.SPOILER,
            MarkdownFormat.URL
    };

    public static final NodeParser contentParser = createParser(ALL);

    public static NodeParser createParser(MarkdownFormat[] capabilities) {
        return new MarkdownLiteParserV1(
                MarkdownComponentParser::spoilerFormatting,
                MarkdownComponentParser::quoteFormatting,
                MarkdownComponentParser::urlFormatting,
                capabilities
        );
    }
}

package ct.discordbridge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

public class ChatComponents {
    public static final Component discordMessagePrefix = Component
            .text("D")
            .color(TextColor.color(Colors.BLURPLE))
            .hoverEvent(HoverEvent.showText(Component.text("Message from the Discord server")))
            .clickEvent(ClickEvent.openUrl(DiscordBridge.CONFIG.inviteLink()));

    public static final Component mentionIcon = Component
            .text("@")
            .color(TextColor.color(Colors.BLURPLE));

    public static final Component channelIcon = Component
            .text("#")
            .color(TextColor.color(Colors.BLURPLE));

    public static Component makeUser(String name, String suggest, int color, @Nullable Component prefix) {
        var comp = Component.text(name)
                .color(TextColor.color(color))
                .hoverEvent(HoverEvent.showText(Component.text("Click to mention")))
                .clickEvent(ClickEvent.suggestCommand(suggest));

        if (prefix != null)
            return Component.empty()
                    .append(prefix)
                    .append(comp);

        return comp;
    }

    public static Component makeMessageHeader(Component content) {
        return Component.empty()
                .append(Component.text("<"))
                .append(content)
                .append(Component.text(">"));
    }

    public static Component makeReplyHeader(Component user, Component referenceUser, Component referenceMessage) {
        return Component.empty()
                .append(user)
                .append(Component
                        .text(" replied to ")
                        .color(NamedTextColor.GRAY)
                        .hoverEvent(HoverEvent.showText(Component.text("Message: ").append(referenceMessage)))
                )
                .append(referenceUser);
    }

    public static Component makeMessage(Component headerContent, Component message) {
        return Component.empty()
                .append(discordMessagePrefix)
                .appendSpace()
                .append(makeMessageHeader(headerContent))
                .appendSpace()
                .append(message);
    }

    public static Component makeAttachment(String fileName, String url) {
        return Component
                .text(String.format("[%s]", fileName))
                .color(TextColor.color(NamedTextColor.BLUE))
                .hoverEvent(HoverEvent.showText(Component.text("Click to open attachment")))
                .clickEvent(ClickEvent.openUrl(url));
    }
}

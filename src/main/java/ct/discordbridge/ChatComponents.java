package ct.discordbridge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.Nullable;

public class ChatComponents {
    public static Component discordMessagePrefix = MiniMessage.miniMessage().deserialize(DiscordBridge.CONFIG.prefix());

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

    public static Component makeReplyHeader(Component referenceUser, Component referenceMessage) {
        return MiniMessage.miniMessage().deserialize(DiscordBridge.CONFIG.reply(),
                Placeholder.component("reference_username", referenceUser),
                Placeholder.component("reference_message", referenceMessage)
        );
    }

    public static Component makeMessage(Component username, @Nullable Component reply, Component message) {
        if (reply == null)
            reply = Component.empty();
        return MiniMessage.miniMessage().deserialize(DiscordBridge.CONFIG.messageFormat(),
                Placeholder.component("prefix", discordMessagePrefix),
                Placeholder.component("username", username),
                Placeholder.component("reply", reply),
                Placeholder.component("message", message)
        );
        //return Component.empty();
    }

    public static Component makeAttachment(String fileName, String url) {
        return Component
                .text(String.format("[%s]", fileName))
                .color(TextColor.color(NamedTextColor.BLUE))
                .hoverEvent(HoverEvent.showText(Component.text("Click to open attachment")))
                .clickEvent(ClickEvent.openUrl(url));
    }
}

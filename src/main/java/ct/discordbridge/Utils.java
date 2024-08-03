package ct.discordbridge;

import discord4j.core.object.entity.Member;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.server.network.ServerPlayerEntity;

public class Utils {
    public static String getAvatarUrl(ServerPlayerEntity player) {
        var avatarApiUrl = Bridge.CONFIG.avatarApiUrl();
        return avatarApiUrl.replaceAll("\\{\\{uuid}}", player.getUuidAsString());
    }

    public static String getAvatarThumbnailUrl(ServerPlayerEntity player) {
        var avatarApiUrl = Bridge.CONFIG.avatarApiThumbnailUrl();
        return avatarApiUrl.replaceAll("\\{\\{uuid}}", player.getUuidAsString());
    }

    public static Component getMemberNameComponent(Member member) {
        return getMemberNameComponent(member.getDisplayName(), TextColor.color(member.getColor().block().getRGB()), member.getMention());
    }

    public static Component getMemberNameComponent(String name, TextColor color, String asMention) {
        return Component
                .text(name)
                .color(color)
                .hoverEvent(HoverEvent.showText(Component.text("Click to mention")))
                .clickEvent(ClickEvent.suggestCommand(asMention + ":"));
    }

    public static String getMemberName(Member member) {
        if (member == null) {
            return "Unknown User";
        }
        return member.getDisplayName();
    }
}

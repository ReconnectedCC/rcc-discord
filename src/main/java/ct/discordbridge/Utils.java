package ct.discordbridge;

import net.dv8tion.jda.api.entities.Member;
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
}

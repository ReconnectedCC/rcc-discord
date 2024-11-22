package cc.reconnected.discordbridge;

import net.minecraft.server.network.ServerPlayerEntity;

public class Utils {
    public static String getAvatarUrl(ServerPlayerEntity player) {
        var avatarApiUrl = RccDiscord.CONFIG.avatarApiUrl;
        return avatarApiUrl.replaceAll("\\{\\{uuid}}", player.getUuidAsString());
    }

    public static String getAvatarThumbnailUrl(ServerPlayerEntity player) {
        var avatarApiUrl = RccDiscord.CONFIG.avatarApiThumbnailUrl;
        return avatarApiUrl.replaceAll("\\{\\{uuid}}", player.getUuidAsString());
    }
}

package ct.discordbridge;

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

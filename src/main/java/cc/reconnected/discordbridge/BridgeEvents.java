package cc.reconnected.discordbridge;

import cc.reconnected.server.RccServer;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.network.ServerPlayerEntity;

public class BridgeEvents {
    public static void register(Bridge bridge) {
        var client = bridge.getClient();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (!client.isReady())
                return;
            bridge.sendServerStatus(":hourglass: **Server is starting...**", NamedTextColor.YELLOW.value());
            bridge.setStatus(OnlineStatus.DO_NOT_DISTURB, Activity.watching("the server starting"));
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!client.isReady())
                return;
            bridge.sendServerStatus(":up: **Server started!**", NamedTextColor.GREEN.value());
            updatePlayerCount(0);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (!client.isReady())
                return;
            bridge.sendServerStatus(":electric_plug: **Server is stopping!**", NamedTextColor.RED.value());
            bridge.setStatus(OnlineStatus.DO_NOT_DISTURB, Activity.watching("the server stopping"));
            bridge.getClient().client().shutdown();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            RccServer.LOGGER.info("Attempting rcc-discord force shutdown...");
            try {
                var jda = bridge.getClient().client();
                jda.shutdownNow();
            } catch (Exception e) {
                RccServer.LOGGER.error("Force disconnect rcc-bridge failure", e);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!client.isReady())
                return;
            var playerName = handler.player.getDisplayName().getString();
            bridge.sendPlayerStatus(String.format("%s joined the server", playerName), NamedTextColor.GREEN.value(), Utils.getAvatarThumbnailUrl(handler.player));
            updatePlayerCount(server.getPlayerManager().getCurrentPlayerCount() + 1);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (!client.isReady())
                return;
            var playerName = handler.player.getDisplayName().getString();
            bridge.sendPlayerStatus(String.format("%s left the server", playerName), NamedTextColor.RED.value(), Utils.getAvatarThumbnailUrl(handler.player));
            updatePlayerCount(server.getPlayerManager().getCurrentPlayerCount() - 1);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!client.isReady())
                return;
            if (!(entity instanceof ServerPlayerEntity player))
                return;

            var message = damageSource.getDeathMessage(entity).getString();
            var avatarUrl = Utils.getAvatarThumbnailUrl(player);

            bridge.sendPlayerStatus(message, NamedTextColor.GRAY.value(), avatarUrl);
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (!client.isReady())
                return;
            var playerName = sender.getDisplayName().getString();
            var avatarUrl = Utils.getAvatarUrl(sender);
            bridge.sendPlayerMessage(message.message(), playerName, avatarUrl);
        });
    }

    private static void updatePlayerCount(int count) {
        if (!Bridge.CONFIG.usePresence())
            return;

        var text = "with " + count + " players!";
        if (count == 0) {
            text = "with no one :(";
        }
        Bridge.getInstance().setStatus(OnlineStatus.ONLINE, Activity.playing(text));
    }
}

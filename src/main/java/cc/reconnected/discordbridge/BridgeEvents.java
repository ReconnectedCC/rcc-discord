package cc.reconnected.discordbridge;

import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.Status;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Objects;

public class BridgeEvents {
    public static void register(RccDiscord bridge) {
        var client = bridge.getClient();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (!client.isReady())
                return;
            bridge.sendServerStatus(":hourglass: **Server is starting...**", NamedTextColor.YELLOW.value());
            bridge.setStatus(Status.DO_NOT_DISTURB, ClientActivity.watching("the server starting"));
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
            bridge.setStatus(Status.DO_NOT_DISTURB, ClientActivity.watching("the server stopping"));
            bridge.shutdownNow();
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
            bridge.sendPlayerMessage(message.getSignedContent(), playerName, avatarUrl);
        });

        ServerMessageEvents.COMMAND_MESSAGE.register((message, source, params) -> {
            String avatarUrl;
            if (source.isExecutedByPlayer()) {
                avatarUrl = Utils.getAvatarUrl(Objects.requireNonNull(source.getPlayer()));
            } else {
                avatarUrl = RccDiscord.CONFIG.serverAvatarUrl;
            }
            var name = source.getDisplayName().getString();

            bridge.sendPlayerMessage(message.getSignedContent(), name, avatarUrl);
        });
    }

    private static void updatePlayerCount(int count) {
        if (!RccDiscord.CONFIG.usePresence)
            return;

        var text = "with " + count + " players!";
        if (count == 0) {
            text = "with no one :(";
        }
        RccDiscord.getInstance().setStatus(text);
    }
}

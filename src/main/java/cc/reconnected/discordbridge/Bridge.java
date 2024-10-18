package cc.reconnected.discordbridge;

import cc.reconnected.discordbridge.commands.DiscordCommand;
import cc.reconnected.discordbridge.discord.Client;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Bridge implements ModInitializer {

    public static final String MOD_ID = "rcc-discord";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static Bridge INSTANCE;

    public static final cc.reconnected.discordbridge.DiscordConfig CONFIG = cc.reconnected.discordbridge.DiscordConfig.createAndLoad();
    private Client client;

    private static final Queue<Component> chatQueue = new ConcurrentLinkedQueue<>();

    public Bridge() {
        INSTANCE = this;
    }

    public static Bridge getInstance() {
        return INSTANCE;
    }

    public Client getClient() {
        return client;
    }

    public static final HashMap<String, ServerPlayerEntity> linkCodes = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Discord Bridge");

        try {
            this.client = new Client();
        } catch (Exception e) {
            LOGGER.error("Error creating Discord client", e);
            return;
        }

        CommandRegistrationCallback.EVENT.register(DiscordCommand::register);

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            while (!chatQueue.isEmpty()) {
                var message = chatQueue.poll();

                LOGGER.info(PlainTextComponentSerializer.plainText().serialize(message));

                var list = server.getPlayerManager().getPlayerList();
                for (ServerPlayerEntity player : list) {
                    player.sendMessage(message);
                }
            }
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if(!client.isReady())
                return;
            sendServerStatus(":hourglass: **Server is starting...**", NamedTextColor.YELLOW.value());
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if(!client.isReady())
                return;
            sendServerStatus(":up: **Server started!**", NamedTextColor.GREEN.value());
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if(!client.isReady())
                return;
            sendServerStatus(":electric_plug: **Server is stopping!**", NamedTextColor.RED.value());
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if(!client.isReady())
                return;
            var playerName = handler.player.getDisplayName().getString();
            sendPlayerStatus(String.format("%s joined the server", playerName), NamedTextColor.GREEN.value(), Utils.getAvatarThumbnailUrl(handler.player));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if(!client.isReady())
                return;
            var playerName = handler.player.getDisplayName().getString();
            sendPlayerStatus(String.format("%s left the server", playerName), NamedTextColor.RED.value(), Utils.getAvatarThumbnailUrl(handler.player));
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if(!client.isReady())
                return;
            if (!(entity instanceof ServerPlayerEntity player))
                return;

            var message = damageSource.getDeathMessage(entity).getString();
            var avatarUrl = Utils.getAvatarThumbnailUrl(player);

            sendPlayerStatus(message, NamedTextColor.GRAY.value(), avatarUrl);
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if(!client.isReady())
                return;
            var playerName = sender.getDisplayName().getString();
            var avatarUrl = Utils.getAvatarUrl(sender);
            sendPlayerMessage(message.message(), playerName, avatarUrl);
        });
    }

    public static void enqueueMessage(Component component) {
        chatQueue.offer(component);
    }

    public void sendServerStatus(String message, int color) {
        if(!client.isReady())
            return;
        var embed = new WebhookEmbedBuilder()
                .setDescription(message)
                .setColor(color)
                .build();
        client.webhookClient().send(embed);
    }

    public void sendPlayerStatus(String message, int color, String avatarUrl) {
        if(!client.isReady())
            return;
        var embed= new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor(message, avatarUrl, null))
                .setColor(color)
                .build();
        client.webhookClient().send(embed);
    }

    public void sendPlayerMessage(String message, String name, String avatarUrl) {
        if(!client.isReady())
            return;
        var webhookMessage = new WebhookMessageBuilder()
                .setAvatarUrl(avatarUrl)
                .setUsername(name)
                .setContent(message)
                .setAllowedMentions(
                        new AllowedMentions()
                                .withParseUsers(true)
                                .withParseRoles(true)
                                .withParseEveryone(false)
                )
                .build();
        client.webhookClient().send(webhookMessage);
    }
}

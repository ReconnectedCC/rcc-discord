package cc.reconnected.discordbridge;

import cc.reconnected.discordbridge.commands.DiscordCommand;
import cc.reconnected.discordbridge.discord.Client;
import cc.reconnected.library.config.ConfigManager;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RccDiscord implements ModInitializer {

    public static final String MOD_ID = "rcc-discord";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static RccDiscord INSTANCE;

    public static RccDiscordConfig CONFIG;
    private Client client;
    private MinecraftServer mcServer;

    private static final Queue<Component> chatQueue = new ConcurrentLinkedQueue<>();

    public RccDiscord() {
        INSTANCE = this;
    }

    public static RccDiscord getInstance() {
        return INSTANCE;
    }

    public Client getClient() {
        return client;
    }

    public static final HashMap<String, ServerPlayerEntity> linkCodes = new HashMap<>();

    /**
     * Discord snowflake ID -> Player UUID
     */
    public static ConcurrentHashMap<String, UUID> discordLinks = new ConcurrentHashMap<>();
    private static Path dataDirectory;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Discord Bridge");

        try {
            CONFIG = ConfigManager.load(RccDiscordConfig.class);
        } catch (Exception e) {
            LOGGER.error("Failed to load config. Refusing to continue.", e);
            return;
        }

        try {
            this.client = new Client();
        } catch (Exception e) {
            LOGGER.error("Error creating Discord client", e);
            return;
        }

        CommandRegistrationCallback.EVENT.register(DiscordCommand::register);

        BridgeEvents.register(this);

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
            dataDirectory = server.getSavePath(WorldSavePath.ROOT).resolve("data").resolve(MOD_ID);
            mcServer = server;
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // load discord id map
            if (!dataDirectory.toFile().isDirectory()) {
                if (!dataDirectory.toFile().mkdir()) {
                    LOGGER.error("Failed to create rcc-discord data directory");
                }
            }

            var mapPath = dataDirectory.resolve("links.json");
            if (mapPath.toFile().exists()) {
                try (var stream = new BufferedReader(new FileReader(mapPath.toFile(), StandardCharsets.UTF_8))) {
                    var type = new TypeToken<ConcurrentHashMap<String, UUID>>() {
                    }.getType();
                    discordLinks = new Gson().fromJson(stream, type);
                } catch (IOException e) {
                    LOGGER.error("Exception reading licenses data", e);
                }
            } else {
                discordLinks = new ConcurrentHashMap<>();
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // nuke the client
            shutdownNow();
        });
    }

    public void shutdown() {
        var httpClient = client.client().getHttpClient();
        client.client().shutdown();
        httpClient.connectionPool().evictAll();
        httpClient.dispatcher().executorService().shutdown();
    }

    public void shutdownNow() {
        var httpClient = client.client().getHttpClient();
        client.client().shutdownNow();
        httpClient.connectionPool().evictAll();
        httpClient.dispatcher().executorService().shutdownNow();
        try {
        client.client().awaitShutdown();
        } catch (InterruptedException e) {
            LOGGER.error("Error shutting down Discord client", e);
        }
    }

    public static void enqueueMessage(Component component) {
        chatQueue.offer(component);
    }

    public void sendServerStatus(String message, int color) {
        if (!client.isReady())
            return;
        var embed = new WebhookEmbedBuilder()
                .setDescription(message)
                .setColor(color)
                .build();
        client.webhookClient().send(embed);
    }

    public void sendPlayerStatus(String message, int color, String avatarUrl) {
        if (!client.isReady())
            return;
        var embed = new WebhookEmbedBuilder()
                .setAuthor(new WebhookEmbed.EmbedAuthor(message, avatarUrl, null))
                .setColor(color)
                .build();
        client.webhookClient().send(embed);
    }

    public void sendPlayerMessage(String message, String name, String avatarUrl) {
        if (!client.isReady())
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

    public void setStatus(String string) {
        setStatus(OnlineStatus.ONLINE, Activity.playing(string));
    }

    public void setStatus(OnlineStatus status, Activity activity) {
        client.client().getPresence().setPresence(status, activity);
    }

    public void saveData() {
        var output = new Gson().toJson(discordLinks);
        try (var stream = new FileWriter(dataDirectory.resolve("links.json").toFile(), StandardCharsets.UTF_8)) {
            stream.write(output);
        } catch (IOException e) {
            LOGGER.error("Exception Discord links map data", e);
        }
    }

    public String[] getPlayerNames() {
        return mcServer.getPlayerManager().getPlayerNames();
    }
}

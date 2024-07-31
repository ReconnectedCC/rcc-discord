package ct.discordbridge;

import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Color;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ct.discordbridge.DiscordConfig;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class DiscordBridge implements ModInitializer {

    public static final String MOD_ID = "ct-discord";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final DiscordConfig CONFIG = DiscordConfig.createAndLoad();
    private DiscordClient client;

    private static final Queue<Component> chatQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Discord Bridge");

        try {
            this.client = new DiscordClient();
        } catch (InterruptedException e) {
            LOGGER.error("Error creating Discord client", e);
            return;
        }

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            while(!chatQueue.isEmpty()) {
                var message = chatQueue.poll();
                var list = server.getPlayerManager().getPlayerList();
                for(ServerPlayerEntity player : list) {
                    player.sendMessage(message);
                }
            }
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            client.webhook().execute()
                    .withEmbeds(EmbedCreateSpec.create()
                            .withDescription(":hourglass: **Server is starting...**")
                            .withColor(Color.of(NamedTextColor.YELLOW.value()))
                    )
                    .subscribe();
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            client.webhook().execute()
                    .withEmbeds(EmbedCreateSpec.create()
                            .withDescription(":up: **Server started!**")
                            .withColor(Color.of(NamedTextColor.GREEN.value()))
                    )
                    .subscribe();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            client.webhook().execute()
                    .withEmbeds(EmbedCreateSpec.create()
                            .withDescription(":electric_plug: **Server is stopping!**")
                            .withColor(Color.of(NamedTextColor.RED.value()))
                    )
                    .subscribe();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var playerName = handler.player.getDisplayName().getString();

            client.webhook().execute()
                    .withEmbeds(EmbedCreateSpec.create()
                            .withAuthor(EmbedCreateFields.Author.of(String.format("%s joined the server", playerName), null, Utils.getAvatarThumbnailUrl(handler.player)))
                            .withColor(Color.of(NamedTextColor.GREEN.value())))
                    .subscribe();
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var playerName = handler.player.getDisplayName().getString();

            client.webhook().execute()
                    .withEmbeds(EmbedCreateSpec.create()
                            .withAuthor(EmbedCreateFields.Author.of(String.format("%s left the server", playerName), null, Utils.getAvatarThumbnailUrl(handler.player)))
                            .withColor(Color.of(NamedTextColor.RED.value())))
                    .subscribe();
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayerEntity player))
                return;

            var message = damageSource.getDeathMessage(entity).getString();
            var avatarUrl = Utils.getAvatarThumbnailUrl(player);

            client.webhook().execute()
                    .withEmbeds(EmbedCreateSpec.create()
                            .withAuthor(EmbedCreateFields.Author.of(String.format("%s", message), null, avatarUrl))
                            .withColor(Color.of(NamedTextColor.GRAY.value())))
                    .subscribe();
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            var playerName = sender.getDisplayName().getString();
            var avatarUrl = Utils.getAvatarThumbnailUrl(sender);
            client.webhook().execute()
                    .withAvatarUrl(avatarUrl)
                    .withUsername(playerName)
                    .withContent(message.getContent().getString())
                    .withAllowedMentions(AllowedMentions.suppressEveryone())
                    .subscribe();
        });
    }

    public static void enqueueMessage(Component component) {
        chatQueue.offer(component);
    }
}

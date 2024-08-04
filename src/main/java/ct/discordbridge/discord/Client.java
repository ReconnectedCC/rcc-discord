package ct.discordbridge.discord;


import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import ct.discordbridge.Bridge;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.EnumSet;

public class Client {
    private TextChannel chatChannel;
    private Webhook webhook;
    //private GatewayDiscordClient client;
    private JDA client;
    private final Events events = new Events();
    private Guild guild;
    private WebhookClient webhookClient;

    public Client() throws Exception {
        initialize();
    }

    public TextChannel chatChannel() {
        return chatChannel;
    }

    public Webhook webhook() {
        return webhook;
    }

    public WebhookClient webhookClient() {
        return webhookClient;
    }

    public JDA client() {
        return client;
    }

    public Events events() {
        return events;
    }

    public Guild guild() {
        return guild;
    }

    private void initialize() {
        client = JDABuilder
                .create(Bridge.CONFIG.token(), EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT))
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.SCHEDULED_EVENTS)
                .addEventListeners(new DiscordEvents())
                .build();
    }

    private class DiscordEvents extends ListenerAdapter {
        @Override
        public void onReady(ReadyEvent event) {
            final var self = client.getSelfUser();
            Bridge.LOGGER.info("Logged in as {}", self.getAsTag());

            chatChannel = client.getTextChannelById(Bridge.CONFIG.channelId());
            if (chatChannel == null) {
                Bridge.LOGGER.error("Channel not found! Set an existing channel ID that I can see!");
                client.shutdown();
                return;
            }

            guild = chatChannel.getGuild();

            var webhookName = Bridge.CONFIG.name();
            var webhooks = chatChannel.retrieveWebhooks().complete();

            webhooks.stream()
                    .filter((wh) -> wh.getName().equals(webhookName))
                    .findFirst()
                    .ifPresent((wh) -> {
                        webhook = wh;
                    });
            if (webhook == null) {
                webhook = chatChannel.createWebhook(webhookName).complete();
            }

            webhookClient = new WebhookClientBuilder(webhook.getUrl())
                    .setDaemon(true)
                    .buildJDA();
        }

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            events.onMessageCreate(event);
        }

        @Override
        public void onMessageUpdate(MessageUpdateEvent event) {
            events.onMessageEdit(event);
        }
    }
}

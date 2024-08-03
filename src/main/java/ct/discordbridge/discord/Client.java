package ct.discordbridge.discord;


import ct.discordbridge.Bridge;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import reactor.core.publisher.Mono;

public class Client {
    private TextChannel chatChannel;
    private Webhook webhook;
    private GatewayDiscordClient client;
    private Events events;

    public Client() throws Exception {
        initialize();
    }

    public TextChannel chatChannel() {
        return chatChannel;
    }

    public Webhook webhook() {
        return webhook;
    }

    public GatewayDiscordClient client() {
        return client;
    }

    public Events events() {
        return events;
    }

    private void initialize() {
        var clientMono = DiscordClientBuilder.create(Bridge.CONFIG.token())
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.of(
                        Intent.MESSAGE_CONTENT,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_MEMBERS
                ))
                .login();

        clientMono.flatMap(client -> {
            client.on(ReadyEvent.class).subscribe(event -> {
                this.client = client;
                final var self = event.getSelf();
                Bridge.LOGGER.info("Logged in as {}", self.getTag());

                chatChannel = (TextChannel) client.getChannelById(Snowflake.of(Bridge.CONFIG.channelId())).block();

                if(chatChannel == null){
                    Bridge.LOGGER.error("Channel not found! Set an existing channel ID that I can see!");
                    return;
                }

                var webhookName = Bridge.CONFIG.name();
                this.webhook = chatChannel.getWebhooks().filter(wh -> wh.getName().get().equals(webhookName)).singleOrEmpty().block();
                if (this.webhook == null) {
                    this.webhook = chatChannel.createWebhook(webhookName).block();
                }

                events = new Events(this);
            });
            return Mono.empty();
        }).block();
        clientMono.subscribe();
    }
}

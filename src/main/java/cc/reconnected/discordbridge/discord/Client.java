package cc.reconnected.discordbridge.discord;


import cc.reconnected.discordbridge.RccDiscord;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.interaction.GuildCommandRegistrar;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.List;

public class Client {
    public Snowflake channelId;
    private TextChannel chatChannel;
    private Webhook webhook;
    private GatewayDiscordClient client;
    private final Events events = new Events();
    private Guild guild;
    @Nullable
    private Role role = null;
    private boolean isReady = false;

    public Client() {
        channelId = Snowflake.of(RccDiscord.CONFIG.channelId);
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

    public Guild guild() {
        return guild;
    }

    public Role role() {
        return role;
    }

    public boolean isReady() {
        return isReady;
    }

    private void initialize() {
        var publisher = DiscordClientBuilder.create(RccDiscord.CONFIG.token)
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.of(
                        Intent.MESSAGE_CONTENT,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_MEMBERS
                ))
                .login();

        publisher.flatMap(client -> {
            this.client = client;

            client.on(ReadyEvent.class).subscribe(event -> {
                final var self = event.getSelf();
                RccDiscord.LOGGER.info("Logged in as {}", self.getTag());

                var channel = client.getChannelById(channelId).block();
                if (channel.getType() != Channel.Type.GUILD_TEXT) {
                    throw new IllegalArgumentException("Channel is not a guild text");
                }

                chatChannel = (TextChannel) channel;
                guild = chatChannel.getGuild().block();
                webhook = chatChannel.getWebhooks()
                        .filter(wh -> wh.getName()
                                .get().equals(RccDiscord.CONFIG.name))
                        .singleOrEmpty().block();
                if (webhook == null) {
                    webhook = chatChannel.createWebhook(RccDiscord.CONFIG.name).block();
                }

                role = guild.getRoleById(Snowflake.of(RccDiscord.CONFIG.roleId)).block();

                var linkCommand = ApplicationCommandRequest.builder()
                        .name("link")
                        .description("Link your Minecraft profile with Discord.")
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("code")
                                .description("Linking code")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .build();

                var listCommand = ApplicationCommandRequest.builder()
                        .name("list")
                        .description("Get online players.")
                        .build();

                GuildCommandRegistrar.create(client.getRestClient(), List.of(linkCommand, listCommand))
                        .registerCommands(guild.getId())
                        .doOnError(e -> RccDiscord.LOGGER.error("Error registering guild commands", e))
                        .onErrorResume(e -> Mono.empty())
                        .blockLast();

                isReady = true;
            });

            client.on(MessageCreateEvent.class).subscribe(events::onMessageCreate);
            client.on(MessageUpdateEvent.class).subscribe(events::onMessageEdit);
            client.on(ChatInputInteractionEvent.class).subscribe(events::onChatInputInteraction);

            return Mono.empty();
        }).block();
    }
}

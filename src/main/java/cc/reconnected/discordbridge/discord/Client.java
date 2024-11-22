package cc.reconnected.discordbridge.discord;


import cc.reconnected.discordbridge.RccDiscord;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class Client {
    private TextChannel chatChannel;
    private Webhook webhook;
    //private GatewayDiscordClient client;
    private JDA client;
    private final Events events = new Events();
    private Guild guild;
    @Nullable
    private Role role = null;
    private WebhookClient webhookClient;
    private boolean isReady = false;

    public Client() {
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

    public Role role() {
        return role;
    }

    public boolean isReady() {
        return isReady;
    }

    private void initialize() {
        client = JDABuilder
                .create(RccDiscord.CONFIG.token, EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT))
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.SCHEDULED_EVENTS)
                .addEventListeners(new DiscordEvents())
                .build();
    }

    private class DiscordEvents extends ListenerAdapter {
        @Override
        public void onReady(@NotNull ReadyEvent event) {
            final var self = client.getSelfUser();
            RccDiscord.LOGGER.info("Logged in as {}", self.getAsTag());

            chatChannel = client.getTextChannelById(RccDiscord.CONFIG.channelId);
            if (chatChannel == null) {
                RccDiscord.LOGGER.error("Channel not found! Set an existing channel ID that I can see!");
                client.shutdown();
                return;
            }

            guild = chatChannel.getGuild();

            role = guild.getRoleById(RccDiscord.CONFIG.roleId);

            var webhookName = RccDiscord.CONFIG.name;
            var webhooks = chatChannel.retrieveWebhooks().complete();

            webhooks.stream()
                    .filter((wh) -> wh.getName().equals(webhookName))
                    .findFirst()
                    .ifPresent((wh) -> webhook = wh);
            if (webhook == null) {
                webhook = chatChannel.createWebhook(webhookName).complete();
            }

            if (webhook == null) {
                RccDiscord.LOGGER.error("Attempt to create a webhook failed! Please create a WebHook by the name {} and restart the server", webhookName);
                client.shutdown();
                return;
            }
            webhookClient = new WebhookClientBuilder(webhook.getUrl())
                    .setDaemon(true)
                    .buildJDA();

            guild.updateCommands().addCommands(
                    Commands.slash("link", "Link your Minecraft profile with Discord.")
                            .addOption(OptionType.STRING, "code", "Linking code"),
                    Commands.slash("list", "Get online players.")
            ).queue();

            isReady = true;
        }

        @Override
        public void onMessageReceived(@NotNull MessageReceivedEvent event) {
            events.onMessageCreate(event);
        }

        @Override
        public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
            events.onMessageEdit(event);
        }

        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
            if(RccDiscord.CONFIG.enableSlashCommands)
                events.onSlashCommandInteraction(event);
        }
    }
}

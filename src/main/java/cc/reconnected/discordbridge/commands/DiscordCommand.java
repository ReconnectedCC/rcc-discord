package cc.reconnected.discordbridge.commands;

import cc.reconnected.discordbridge.RccDiscord;
import cc.reconnected.library.data.PlayerMeta;
import com.mojang.brigadier.CommandDispatcher;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.*;

public class DiscordCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry, CommandManager.RegistrationEnvironment environment) {

        var commandBase = literal("discord")
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.empty()
                            .append(Text.literal("Join the Discord server via this invite link: "))
                            .append(Text.literal(RccDiscord.CONFIG.inviteLink)
                                    .setStyle(Style.EMPTY
                                            .withColor(Formatting.BLUE)
                                            .withUnderline(true)
                                            .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.of("Click to open invite")))
                                            .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_URL, RccDiscord.CONFIG.inviteLink))
                                    )
                            ).setStyle(Style.EMPTY.withColor(Formatting.GREEN)), false);
                    return 1;
                })
                .then(literal("link")
                        .executes(context -> {
                            if (!context.getSource().isExecutedByPlayer()) {
                                context.getSource().sendFeedback(() -> Text.of("This command can only be executed by players."), false);
                                return 1;
                            }
                            var player = context.getSource().getPlayer();
                            var playerData = PlayerMeta.getPlayer(player);
                            if (playerData.get(PlayerMeta.KEYS.discordId) != null) {
                                context.getSource().sendFeedback(() -> Text.literal("You already linked your Discord profile!").setStyle(Style.EMPTY.withColor(Formatting.RED)), false);
                                return 1;
                            }

                            var code = generateLinkCode();
                            RccDiscord.linkCodes.put(code, player);

                            var text = Component.empty()
                                    .append(Component.text("Your profile is ready to be linked!"))
                                    .appendNewline().appendNewline()
                                    .append(Component.text("Run "))
                                    .append(Component.text("/link code:" + code)
                                            .hoverEvent(HoverEvent.showText(Component.text("Click to copy")))
                                            .clickEvent(ClickEvent.copyToClipboard("/link code:" + code))
                                            .color(NamedTextColor.BLUE)
                                            .decorate(TextDecoration.UNDERLINED)
                                    )
                                    .append(Component.text(" on Discord to link your profile!"))
                                    .color(NamedTextColor.GREEN);

                            context.getSource().sendMessage(text);

                            return 1;
                        }))
                .then(literal("unlink")
                        .executes(context -> {
                            if (!context.getSource().isExecutedByPlayer()) {
                                context.getSource().sendFeedback(() -> Text.of("This command can only be executed by players."), false);
                                return 1;
                            }
                            var player = context.getSource().getPlayer();
                            var playerData = PlayerMeta.getPlayer(player);
                            var snowflake = playerData.get(PlayerMeta.KEYS.discordId);
                            if (snowflake == null) {
                                context.getSource().sendFeedback(() -> Text.literal("You did not link your profile yet").setStyle(Style.EMPTY.withColor(Formatting.RED)), false);
                                return 1;
                            }

                            var client = RccDiscord.getInstance().getClient();
                            if (client.role() != null) {
                                var guild = client.guild();
                                var member = guild.getMemberById(snowflake);
                                try {
                                    guild.removeRoleFromMember(member, client.role()).queue();
                                } catch (InsufficientPermissionException e) {
                                    RccDiscord.LOGGER.error("Could not remove role from player", e);
                                }
                            }

                            RccDiscord.discordLinks.remove(snowflake);
                            playerData.delete(PlayerMeta.KEYS.discordId).join();
                            RccDiscord.getInstance().saveData();

                            context.getSource().sendFeedback(() -> Text.literal("You have unlinked your Discord profile!").setStyle(Style.EMPTY.withColor(Formatting.GREEN)), false);

                            return 1;
                        }));

        dispatcher.register(commandBase);
    }

    public static String generateLinkCode() {
        String code;
        do {
            code = String.format("%06d", randomInt(0, 999999));
        } while (RccDiscord.linkCodes.containsKey(code));

        return code;
    }

    public static int randomInt(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }
}

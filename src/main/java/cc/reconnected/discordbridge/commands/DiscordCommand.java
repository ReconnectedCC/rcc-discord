package cc.reconnected.discordbridge.commands;

import cc.reconnected.discordbridge.Bridge;
import cc.reconnected.server.RccServer;
import cc.reconnected.server.database.PlayerData;
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
                            .append(Text.literal(Bridge.CONFIG.inviteLink())
                                    .setStyle(Style.EMPTY
                                            .withColor(Formatting.BLUE)
                                            .withUnderline(true)
                                            .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.of("Click to open invite")))
                                            .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_URL, Bridge.CONFIG.inviteLink()))
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
                            var playerData = PlayerData.getPlayer(player);
                            if (playerData.get(PlayerData.KEYS.discordId) != null) {
                                context.getSource().sendFeedback(() -> Text.literal("You already linked your Discord profile!").setStyle(Style.EMPTY.withColor(Formatting.RED)), false);
                                return 1;
                            }

                            var code = generateLinkCode();
                            Bridge.linkCodes.put(code, player);

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
                            var playerData = PlayerData.getPlayer(player);
                            if (playerData.get(PlayerData.KEYS.discordId) == null) {
                                context.getSource().sendFeedback(() -> Text.literal("You did not link your profile yet").setStyle(Style.EMPTY.withColor(Formatting.RED)), false);
                                return 1;
                            }

                            var client = Bridge.getInstance().getClient();
                            if (client.role() != null) {
                                var snowflake = playerData.get(PlayerData.KEYS.discordId);
                                var guild = client.guild();
                                var member = guild.getMemberById(snowflake);
                                try {
                                    guild.removeRoleFromMember(member, client.role()).queue();
                                } catch (InsufficientPermissionException e) {
                                    Bridge.LOGGER.error("Could not remove role from player", e);
                                }
                            }

                            playerData.delete(PlayerData.KEYS.discordId).join();

                            context.getSource().sendFeedback(() -> Text.literal("You have unlinked your Discord profile!").setStyle(Style.EMPTY.withColor(Formatting.GREEN)), false);

                            return 1;
                        }));

        dispatcher.register(commandBase);
    }

    public static String generateLinkCode() {
        String code;
        do {
            code = String.format("%06d", randomInt(0, 999999));
        } while (Bridge.linkCodes.containsKey(code));

        return code;
    }

    public static int randomInt(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }
}

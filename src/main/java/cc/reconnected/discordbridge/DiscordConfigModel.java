package cc.reconnected.discordbridge;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "rcc-discord-config", wrapperName = "DiscordConfig")
public class DiscordConfigModel {
    public String token = "";
    public String name = "rcc-bridge";
    public String channelId = "00000";
    public String avatarApiUrl = "https://mc-heads.net/head/{{uuid}}";
    public String avatarApiThumbnailUrl = "https://mc-heads.net/head/{{uuid}}/32";
    public String inviteLink = "https://discord.gg/myinvite";

    // https://docs.advntr.dev/minimessage/format.html

    public String prefix = "<#5865F2><hover:show_text:'This is a message from the Discord server'><click:open_url:'https://discord.gg/myserver'>D<reset>";
    public String reply = " <reference_username> <hover:show_text:'Message: <reference_message>'><gray>↵</gray>";

    public String messageFormat = "<prefix> <username><gray>:</gray><reply> <message>";
}

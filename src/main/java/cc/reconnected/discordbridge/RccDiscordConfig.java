package cc.reconnected.discordbridge;

import cc.reconnected.library.config.Config;

@Config(RccDiscord.MOD_ID)
public class RccDiscordConfig {
    public String token = "";
    public String name = "rcc-bridge";
    public String channelId = "00000";
    public String roleId = "00000";
    public String avatarApiUrl = "https://mc-heads.net/head/{{uuid}}";
    public String avatarApiThumbnailUrl = "https://mc-heads.net/head/{{uuid}}/32";
    public String inviteLink = "https://discord.gg/myinvite";
    public String serverAvatarUrl = "";

    // https://docs.advntr.dev/minimessage/format.html

    public String prefix = "<#5865F2><hover:show_text:'This is a message from the Discord server'><click:open_url:'https://discord.gg/myserver'>D<reset>";
    public String reply = " <reference_username> <hover:show_text:'Message: <reference_message>'><gray>↵</gray>";

    public String messageFormat = "<prefix> <username><gray>:</gray><reply> <message>";

    public boolean usePresence = true;
    public boolean enableSlashCommands = true;
    public String linkedPermissionNode = "rcc.chatbox.linked";
}

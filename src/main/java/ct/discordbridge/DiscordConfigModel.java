package ct.discordbridge;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "ct-discord-config", wrapperName = "DiscordConfig")
public class DiscordConfigModel {
    public String token = "";
    public String name = "ct-bridge";
    public String channelId = "00000";
    public String avatarApiUrl = "https://mc-heads.net/head/{{uuid}}";
    public String avatarApiThumbnailUrl = "https://mc-heads.net/head/{{uuid}}/32";
    public String inviteLink = "https://discord.gg/myinvite";
}

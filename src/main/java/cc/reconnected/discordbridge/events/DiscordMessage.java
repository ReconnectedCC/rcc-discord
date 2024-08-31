package cc.reconnected.discordbridge.events;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface DiscordMessage {
    Event<DiscordMessage> MESSAGE_CREATE = EventFactory.createArrayBacked(DiscordMessage.class,
            (listeners) -> (message, member, isEdited) -> {
                for (DiscordMessage listener : listeners) {
                    listener.messageCreate(message, member, isEdited);
                }
            });

    void messageCreate(Message message, Member member, boolean isEdited);
}

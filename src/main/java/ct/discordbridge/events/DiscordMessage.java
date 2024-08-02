package ct.discordbridge.events;

import discord4j.core.event.domain.message.MessageCreateEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface DiscordMessage {
    Event<DiscordMessage> MESSAGE_CREATE = EventFactory.createArrayBacked(DiscordMessage.class,
            (listeners) -> (event) -> {
                for (DiscordMessage listener : listeners) {
                    listener.messageCreate(event);
                }
            });

    void messageCreate(MessageCreateEvent event);
}

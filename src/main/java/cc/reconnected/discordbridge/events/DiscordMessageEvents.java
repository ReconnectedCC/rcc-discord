package cc.reconnected.discordbridge.events;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class DiscordMessageEvents {
    public static Event<MessageCreate> MESSAGE_CREATE = EventFactory.createArrayBacked(MessageCreate.class,
            (listeners) -> (message, member) -> {
                for (MessageCreate listener : listeners) {
                    listener.onCreate(message, member);
                }
            });

    public static Event<MessageEdit> MESSAGE_EDIT = EventFactory.createArrayBacked(MessageEdit.class,
            (listeners) -> (message, member) -> {
                for (MessageEdit listener : listeners) {
                    listener.onEdit(message, member);
                }
            });

    @FunctionalInterface
    public interface MessageCreate {
        void onCreate(Message message, Member member);
    }

    @FunctionalInterface
    public interface MessageEdit {
        void onEdit(Message message, Member member);
    }
}

package com.matyrobbrt.testframework;

import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.function.Consumer;

public interface Test {
    String id();
    List<String> groups();

    boolean enabledByDefault();
    void onEnabled(EventListenerCollector eventListenerCollector);
    void onDisabled();

    void init(TestFramework framework);

    Status status();

    void setStatus(Status newStatus);

    Visuals visuals();

    interface EventListenerCollector {
        ListenerAcceptor getFor(Mod.EventBusSubscriber.Bus bus);

        interface ListenerAcceptor {
            void register(Object object);

            <T extends Event> void addListener(EventPriority priority, boolean receiveCancelled, Consumer<T> consumer);
            default <T extends Event> void addListener(Consumer<T> consumer) {
                addListener(EventPriority.NORMAL, false, consumer);
            }
        }
    }

    record Status(Result result, String message) {}
    enum Result {
        PASSED, FAILED, NOT_PROCESSED;

        public boolean passed() {
            return this == PASSED;
        }
    }

    record Visuals(Component title, List<Component> description) {}
}

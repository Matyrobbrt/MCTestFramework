package com.matyrobbrt.testframework.impl;

import com.matyrobbrt.testframework.Test;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class EventListenerCollectorImpl implements Test.EventListenerCollector {
    private final Map<Mod.EventBusSubscriber.Bus, IEventBus> buses = new HashMap<>();
    private final Map<Mod.EventBusSubscriber.Bus, ListenerAcceptorImpl> acceptors = new HashMap<>();
    public EventListenerCollectorImpl add(Mod.EventBusSubscriber.Bus type, IEventBus bus) {
        buses.put(type, bus);
        return this;
    }

    @Override
    public ListenerAcceptor getFor(Mod.EventBusSubscriber.Bus bus) {
        return acceptors.computeIfAbsent(bus, it -> new ListenerAcceptorImpl(buses.get(bus)));
    }

    public void unregister() {
        acceptors.values().forEach(ListenerAcceptorImpl::unregister);
    }

    private static final class ListenerAcceptorImpl implements ListenerAcceptor {
        private final IEventBus bus;
        private final List<Object> subscribers = new ArrayList<>();

        private ListenerAcceptorImpl(IEventBus bus) {
            this.bus = bus;
        }

        @Override
        public void register(Object object) {
            subscribers.add(object);
            bus.register(object);
        }

        @Override
        public <T extends Event> void addListener(EventPriority priority, boolean receiveCancelled, Consumer<T> consumer) {
            bus.addListener(priority, receiveCancelled, consumer);
            subscribers.add(consumer);
        }

        private void unregister() {
            subscribers.forEach(bus::unregister);
        }
    }
}

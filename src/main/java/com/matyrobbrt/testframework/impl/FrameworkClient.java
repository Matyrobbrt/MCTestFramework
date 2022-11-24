package com.matyrobbrt.testframework.impl;

import com.matyrobbrt.testframework.conf.ClientConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;

import java.util.Optional;
import java.util.ServiceLoader;

public interface FrameworkClient {
    void init(IEventBus modBus, ModContainer container);
    interface Factory {
        FrameworkClient create(TestFrameworkInternal impl, ClientConfiguration clientConfiguration);
    }

    static Optional<Factory> factory() {
        return ServiceLoader.load(Factory.class).findFirst();
    }
}

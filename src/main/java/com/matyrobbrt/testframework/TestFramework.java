package com.matyrobbrt.testframework;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

public interface TestFramework {
    ResourceLocation id();

    Tests tests();

    void changeStatus(Test test, Test.Status newStatus, @Nullable Entity changer);
    void setEnabled(Test test, boolean enabled, @Nullable Entity changer);

    IEventBus modEventBus();

    interface Tests {
        Optional<Test> byId(String id);

        void enable(String id);
        void disable(String id);
        boolean isEnabled(String id);

        void register(Test test);

        Collection<Test> all();
    }

}

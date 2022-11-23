package com.matyrobbrt.testframework;

import com.matyrobbrt.testframework.group.Group;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TestFramework {
    ResourceLocation id();
    Logger logger();

    Tests tests();

    void changeStatus(Test test, Test.Status newStatus, @Nullable Entity changer);
    void setEnabled(Test test, boolean enabled, @Nullable Entity changer);

    IEventBus modEventBus();

    interface Tests {
        Optional<Test> byId(String id);
        Group getOrCreateGroup(String id);
        Collection<Group> allGroups();

        void enable(String id);
        void disable(String id);
        boolean isEnabled(String id);

        void register(Test test);

        Collection<Test> all();
    }

}

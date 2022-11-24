package com.matyrobbrt.testframework.impl;

import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.TestFramework;
import com.matyrobbrt.testframework.conf.FrameworkConfiguration;
import com.matyrobbrt.testframework.group.Group;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Interface with internal methods for {@link TestFramework TestFrameworks}.
 * @see FrameworkConfiguration#create()
 * @see TestFrameworkImpl
 */
@ApiStatus.Internal
public interface TestFrameworkInternal extends TestFramework {
    FrameworkConfiguration configuration();
    void init(final IEventBus modBus, final ModContainer container);
    void registerCommands(LiteralArgumentBuilder<CommandSourceStack> node);
    List<Test> collectTests(final ModContainer container);

    @Override
    TestsInternal tests();

    @ApiStatus.Internal
    interface TestsInternal extends Tests {
        void initialiseDefaultEnabledTests();
        Stream<Test> enabled();
        Optional<Group> maybeGetGroup(String id);
        void setStatus(String testId, Test.Status status);
    }
}

package com.matyrobbrt.testframework.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

import java.util.function.Consumer;

public record GameTestData(
        String structureName, boolean required, int maxAttempts,
        int requiredSuccesses, Consumer<GameTestHelper> function, int maxTicks,
        long setupTicks, Rotation rotation
) {
}

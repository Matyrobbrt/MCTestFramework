package com.matyrobbrt.testframework.impl;

import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.gametest.GameTestData;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public final class GameTestRegistration {

    public static final Method REGISTER_METHOD = LamdbaExceptionUtils.uncheck(() -> GameTestRegistration.class.getDeclaredMethod("register"));

    @GameTestGenerator
    public static List<TestFunction> register() {
        final List<TestFunction> tests = new ArrayList<>();
        for (final TestFrameworkImpl framework : TestFrameworkImpl.FRAMEWORKS) {
            for (final Test test : framework.tests().all()) {
                final GameTestData data = test.asGameTest();
                if (data != null) {
                    final String batchName = test.groups().size() > 0 ? test.groups().get(0) : "ungrouped";
                    tests.add(new TestFunction(
                            data.batchName() == null ? batchName : data.batchName(),
                            test.id(), data.structureName(),
                            data.rotation(), data.maxTicks(), data.setupTicks(),
                            data.required(), data.requiredSuccesses(), data.maxAttempts(),
                            helper -> {
                                framework.setEnabled(test, true, null);
                                framework.changeStatus(test, Test.Status.DEFAULT, null); // Reset the status, just in case

                                try {
                                    data.function().accept(helper);
                                } catch (GameTestAssertException assertion) {
                                    framework.tests().setStatus(test.id(), new Test.Status(Test.Result.FAILED, assertion.getMessage()));
                                    throw assertion;
                                }

                                final Test.Status status = framework.tests().getStatus(test.id());
                                if (status.result().passed()) helper.succeed();
                                else if (status.result().failed()) helper.fail(status.message());
                            }
                    ));
                }
            }
        }
        return tests;
    }
}

package com.matyrobbrt.testframework.conf;

/**
 * This class represents the different features a {@link com.matyrobbrt.testframework.TestFramework} may have.
 *
 * @see FrameworkConfiguration.Builder#enable(Feature...)
 * @see FrameworkConfiguration.Builder#disable(Feature...)
 */
public enum Feature {
    /**
     * This feature syncs the status of tests to clients.
     */
    CLIENT_SYNC(false),

    /**
     * This feature allows clients to modify the status of tests.
     */
    CLIENT_MODIFICATIONS(false),

    /**
     * This feature enables the GameTest integration.
     */
    GAMETEST(true),

    /**
     * When enabled, test summaries will be dumped when a server stops.
     */
    SUMMARY_DUMP(true),

    /**
     * When enabled, this feature will store the tests that existed when a player last logged on, using a {@linkplain net.minecraft.world.level.saveddata.SavedData}. <br>
     * When a player joins, they will get a message in chat containing all newly added tests.
     */
    TEST_STORE(false);

    private final boolean enabledByDefault;

    Feature(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }
}

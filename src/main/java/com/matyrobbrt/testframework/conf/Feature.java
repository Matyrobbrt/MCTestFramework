package com.matyrobbrt.testframework.conf;

public enum Feature {
    CLIENT_SYNC(false),
    CLIENT_MODIFICATIONS(false),
    GAMETEST(true),
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

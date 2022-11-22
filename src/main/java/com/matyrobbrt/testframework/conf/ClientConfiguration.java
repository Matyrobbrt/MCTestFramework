package com.matyrobbrt.testframework.conf;

public record ClientConfiguration(int toggleOverlayKey) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int toggleOverlayKey;

        public Builder toggleOverlayKey(int toggleOverlayKey) {
            this.toggleOverlayKey = toggleOverlayKey;
            return this;
        }

        public ClientConfiguration build() {
            return new ClientConfiguration(toggleOverlayKey);
        }
    }
}

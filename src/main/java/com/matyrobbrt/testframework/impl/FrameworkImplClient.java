package com.matyrobbrt.testframework.impl;

import com.matyrobbrt.testframework.client.GroupTestsScreen;
import com.matyrobbrt.testframework.client.TestsOverlay;
import com.matyrobbrt.testframework.conf.ClientConfiguration;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.List;
import java.util.function.BooleanSupplier;

final class FrameworkImplClient {
    private final TestFrameworkImpl impl;
    private final ClientConfiguration configuration;

    public FrameworkImplClient(TestFrameworkImpl impl, ClientConfiguration clientConfiguration) {
        this.impl = impl;
        this.configuration = clientConfiguration;
    }

    public void init(IEventBus modBus) {
        final String keyCategory = "key.categories." + impl.id().getNamespace() + "." + impl.id().getPath();

        final BooleanSupplier overlayEnabled;
        if (configuration.toggleOverlayKey() != 0) {
            final ToggleKeyMapping overlayKey = new ToggleKeyMapping("key.testframework.toggleoverlay", configuration.toggleOverlayKey(), keyCategory, () -> true);
            modBus.addListener((final RegisterKeyMappingsEvent event) -> event.register(overlayKey));
            overlayEnabled = () -> !overlayKey.isDown();
        } else {
            overlayEnabled = () -> true;
        }

        modBus.addListener((final RegisterGuiOverlaysEvent event) -> event.registerAboveAll(impl.id().getPath(), new TestsOverlay(impl, overlayEnabled)));

        if (configuration.openManagerKey() != 0) {
            final KeyMapping openManagerKey = new KeyMapping("key.testframework.openmanager", configuration.openManagerKey(), keyCategory) {
                @Override
                public void setDown(boolean pValue) {
                    if (pValue) {
                        Minecraft.getInstance().setScreen(new GroupTestsScreen(
                                Component.literal("All tests"), impl, List.copyOf(impl.tests().allGroups())
                        ));
                    }
                    super.setDown(pValue);
                }
            };
            modBus.addListener((final RegisterKeyMappingsEvent event) -> event.register(openManagerKey));
        }
    }
}

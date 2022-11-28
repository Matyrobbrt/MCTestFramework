package com.matyrobbrt.testframework.testing;

import com.matyrobbrt.testframework.ExampleMod;
import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.impl.test.AbstractTest;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;

@TestHolder(
        value = "use_string_on_air",
        title = "Use String on Air",
        description = {
                "Right click String in air.",
                "The test is completed on the client side."
        },
        enabledByDefault = true,
        groups = ExampleMod.EVENTS
)
public class UseStringOnAirTest extends AbstractTest {
    @Override
    public void onEnabled(EventListenerGroup buses) {
        buses.getFor(Mod.EventBusSubscriber.Bus.FORGE).addListener((final PlayerInteractEvent.RightClickItem event) -> {
            if (event.getLevel().isClientSide() && event.getItemStack().is(Tags.Items.STRING)) {
                pass();
            }
        });
    }

    @Override
    public void onDisabled() {

    }
}

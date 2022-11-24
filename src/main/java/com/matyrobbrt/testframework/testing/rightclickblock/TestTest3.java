package com.matyrobbrt.testframework.testing.rightclickblock;

import com.matyrobbrt.testframework.impl.AbstractTest;
import com.matyrobbrt.testframework.annotation.TestHolder;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;

@TestHolder(
        value = "hello_test_iron",
        title = "Iron Right Click",
        description = {
                "Right-click a block with iron.",
                "Sneaking will result in a fail!"
        },
        groups = "events.rightclickblock"
)
public class TestTest3 extends AbstractTest {
    @Override
    public void onEnabled(EventListenerGroup buses) {
        buses.getFor(Mod.EventBusSubscriber.Bus.FORGE).addListener((final PlayerInteractEvent.RightClickBlock event) -> {
            if (!event.getLevel().isClientSide && event.getItemStack().is(Tags.Items.INGOTS_IRON)) {
                if (event.getEntity().isShiftKeyDown()) {
                    fail("Do not click while sneaking");
                } else {
                    pass();
                }
            }
        });
    }

    @Override
    public void onDisabled() {

    }
}

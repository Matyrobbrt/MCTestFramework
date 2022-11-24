package com.matyrobbrt.testframework.testing.rightclickblock;

import com.matyrobbrt.testframework.impl.AbstractTest;
import com.matyrobbrt.testframework.annotation.TestHolder;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;

@TestHolder(
        value = "hello_test_bone",
        title = "Bone Right Click",
        description = {
                "Right-click a block with a bone.",
                "Sneaking will result in a fail!"
        },
        groups = "events.rightclickblock"
)
public class TestTest extends AbstractTest {
    @Override
    public void onEnabled(EventListenerGroup buses) {
        buses.getFor(Mod.EventBusSubscriber.Bus.FORGE).addListener((final PlayerInteractEvent.RightClickBlock event) -> {
            if (!event.getLevel().isClientSide && event.getItemStack().is(Tags.Items.BONES)) {
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

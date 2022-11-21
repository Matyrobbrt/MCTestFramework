package com.matyrobbrt.testframework.testing;

import com.matyrobbrt.testframework.impl.AbstractTest;
import com.matyrobbrt.testframework.TestHolder;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;

@TestHolder(
        value = "hello_test_carrot",
        title = "Carrot Right Click",
        description = {
                "Right-click a block with a carrot.",
                "Sneaking will result in a fail!"
        }
)
public class TestTest4 extends AbstractTest {
    @Override
    public void onEnabled(EventListenerCollector buses) {
        buses.getFor(Mod.EventBusSubscriber.Bus.FORGE).addListener((final PlayerInteractEvent.RightClickBlock event) -> {
            if (!event.getLevel().isClientSide && event.getItemStack().is(Items.CARROT)) {
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

package com.matyrobbrt.testframework.testing.rightclickblock;

import com.matyrobbrt.testframework.impl.test.AbstractTest;
import com.matyrobbrt.testframework.annotation.TestHolder;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@TestHolder(
        value = "hello_test_glowstone",
        title = "Glowstone Right Click",
        description = {
                "Right-click a block with glowstone.",
                "Sneaking will result in a fail!"
        },
        groups = "events.rightclickblock"
)
public class TestTest1 extends AbstractTest {
    @Override
    public void onEnabled(@NotNull EventListenerGroup buses) {
        buses.getFor(Mod.EventBusSubscriber.Bus.FORGE).addListener((final PlayerInteractEvent.RightClickBlock event) -> {
            if (!event.getLevel().isClientSide && event.getItemStack().is(Items.GLOWSTONE)) {
                if (event.getEntity().isShiftKeyDown()) {
                    fail("Do not click while sneaking");
                } else {
                    pass();
                }
            }
        });
    }
    @Override
    public void onDisabled() {}
}

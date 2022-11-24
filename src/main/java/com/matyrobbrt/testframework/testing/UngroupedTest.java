package com.matyrobbrt.testframework.testing;

import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.impl.AbstractTest;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;

@TestHolder(
        value = "ungrouped_test",
        title = "Ungrouped Test",
        description = {
                "A test which has no group in initialisation.",
                "The test will pass if a player jumps."
        }
)
public class UngroupedTest extends AbstractTest {
    @Override
    public void onEnabled(EventListenerGroup buses) {
        buses.getFor(Mod.EventBusSubscriber.Bus.FORGE).addListener((final LivingEvent.LivingJumpEvent event) -> {
            if (event.getEntity() instanceof Player && !event.getEntity().getLevel().isClientSide()) {
                pass();
            }
        });
    }

    @Override
    public void onDisabled() {}
}

package com.matyrobbrt.testframework.testing;

import com.matyrobbrt.testframework.ExampleMod;
import com.matyrobbrt.testframework.annotation.TestGroup;
import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.impl.AbstractTest;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.Mod;

@TestHolder(
        value = "entity_entering_section",
        title = "Entity Entering Section",
        description = "Tests if the EntityEvent.EnteringSection will be fired when a player moves to another chunk.",
        enabledByDefault = true,
        groups = ExampleMod.ENTITY_EVENTS
)
public class EnteringSectionEventTest extends AbstractTest {
    @Override
    public void onEnabled(EventListenerCollector events) {
        events.getFor(Mod.EventBusSubscriber.Bus.FORGE).addListener((final EntityEvent.EnteringSection event) -> {
            if (event.getEntity() instanceof Player && !event.getEntity().getLevel().isClientSide()) {
                pass();
            }
        });
    }

    @Override
    public void onDisabled() {}
}

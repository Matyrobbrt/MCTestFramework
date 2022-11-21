package com.matyrobbrt.testframework.testing;

import com.matyrobbrt.testframework.TestHolder;
import com.matyrobbrt.testframework.impl.AbstractTest;
import net.minecraft.world.entity.animal.Bee;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.common.Mod;

@TestHolder(
        value = "entity_join_level",
        title = "Entity Join Level",
        description = "Tests if the EntityJoinLevelEvent will be fired, on the client, when a bee is spawned."
)
public class EntityJoinedLevelTest extends AbstractTest {
    @Override
    public void onEnabled(EventListenerCollector buses) {
        buses.getFor(Mod.EventBusSubscriber.Bus.FORGE).addListener((final EntityJoinLevelEvent event) -> {
            if (event.getLevel().isClientSide && event.getEntity() instanceof Bee) {
                pass();
            }
        });
    }

    @Override
    public void onDisabled() {}
}

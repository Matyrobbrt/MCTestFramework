package com.matyrobbrt.testframework.testing;

import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.impl.test.AbstractTest;
import net.minecraft.world.entity.animal.Bee;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@TestHolder(
        value = "entity_join_level",
        title = "Entity Join Level",
        description = "Tests if the EntityJoinLevelEvent will be fired, on the client, when a bee is spawned.",
        groups = {ExampleMod.ENTITY_EVENTS, ExampleMod.LEVEL_RELATED_EVENTS}
)
public class EntityJoinedLevelTest extends AbstractTest {
    @Override
    public void onEnabled(@NotNull EventListenerGroup buses) {
        buses.getFor(Mod.EventBusSubscriber.Bus.FORGE).addListener((final EntityJoinLevelEvent event) -> {
            if (event.getLevel().isClientSide && event.getEntity() instanceof Bee) {
                pass();
            }
        });
    }

    @Override
    public void onDisabled() {}
}

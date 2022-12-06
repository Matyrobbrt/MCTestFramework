package com.matyrobbrt.testframework.testing;

import com.matyrobbrt.testframework.DynamicTest;
import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.TestFramework;
import com.matyrobbrt.testframework.TestListener;
import com.matyrobbrt.testframework.annotation.ForEachTest;
import com.matyrobbrt.testframework.annotation.TestGroup;
import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.annotation.WithListener;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This class contains player events. <br>
 * All events in this class will have the {@code player_} prefix applied, and will be in the {@value GROUP} group.
 */
@ForEachTest(
        idPrefix = "player_",
        groups = PlayerEventsTest.GROUP,
        listeners = PlayerEventsTest.ParentAddedListener.class
)
public class PlayerEventsTest {
    @TestGroup(name = "Player Events", parents = ExampleMod.ENTITY_EVENTS)
    public static final String GROUP = ExampleMod.EVENTS + ".player";

    /**
     * This is a method-based test. <br>
     * This method will be called in {@link com.matyrobbrt.testframework.Test#init(TestFramework)} to set up the test.
     */
    @TestHolder(
            value = "change_gamemode",
            description = "The test passes when a player attempts to change their gamemode to spectator, which will also be prevented."
    )
    @WithListener(TestAddedListener.class)
    static void onChangeGamemode(final DynamicTest test) {
        test.whenEnabled(buses -> buses.getFor(Bus.FORGE).addListener((final PlayerEvent.PlayerChangeGameModeEvent event) -> {
            if (event.getNewGameMode() == GameType.SPECTATOR) {
                event.setCanceled(true);
                test.pass();
            }
        }));
    }

    /**
     * This is a method-based test, which is package-private as the framework invokes it with trusted lookup. <br>
     * This method will be called in {@link com.matyrobbrt.testframework.Test#init(TestFramework)} to set up the test.
     */
    @TestHolder(
            value = "pickup_xp",
            description = "The event passes when a player picks up XP with a value of at least 2."
    )
    @GameTest(template = "examplemod:empty_1x1")
    static void onPickupXp(final DynamicTest test) {
        test.whenEnabled(buses -> buses.getFor(Bus.FORGE).addListener((final PlayerXpEvent.PickupXp event) -> {
            if (event.getOrb().value >= 2) {
                test.pass();
            }
        }));

        test.onGameTest(helper -> {
            final Player mockPlayer = helper.makeMockPlayer();
            new ExperienceOrb(helper.getLevel(), 0, 0, 0, 3).playerTouch(mockPlayer);
            helper.succeedIf(() -> helper.assertEntityProperty(mockPlayer, player -> player.totalExperience == 3 && test.status().result().passed(), "experience"));
        });
    }

    /**
     * This is an event test, which can <strong>only</strong> listen an event, and it can be configured only in the annotation. <br>
     * The method will be called with trusted lookup when the event is fired, alongside the test.
     *
     * @param event the type of the event to listen for
     * @param test  the test this method represents
     */
    @TestHolder(
            value = "earn_advancement",
            description = "The event passes when a player earns an advancement which should show a toast."
    )
    @WithListener(TestAddedListener.class)
    private static void onEarnAdvancement(final AdvancementEvent event, final DynamicTest test) {
        if (event.getAdvancement().getDisplay() != null && event.getAdvancement().getDisplay().shouldShowToast()) {
            test.pass();
        }
    }

    @ParametersAreNonnullByDefault
    protected static final class ParentAddedListener implements TestListener {
        @Override
        public void onEnabled(TestFramework framework, Test test, @Nullable Entity changer) {
            framework.logger().info("Parent added player events test listener detected test '{}' being enabled!", test.id());
        }
    }

    @ParametersAreNonnullByDefault
    protected static final class TestAddedListener implements TestListener {
        @Override
        public void onStatusChange(TestFramework framework, Test test, Test.Status oldStatus, Test.Status newStatus, @Nullable Entity changer) {
            if (newStatus.result().passed()) {
                framework.logger().info("Test added listener detected test '{}' passing!", test.id());
            }
        }
    }
}

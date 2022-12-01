package com.matyrobbrt.testframework.testing;

import com.matyrobbrt.testframework.DynamicTest;
import com.matyrobbrt.testframework.TestFramework;
import com.matyrobbrt.testframework.annotation.OnInit;
import com.matyrobbrt.testframework.annotation.RegisterStructureTemplate;
import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.gametest.StructureTemplateBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;
import java.util.function.Supplier;

public class PistonTests {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "examplemod");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "examplemod");

    public static final RegistryObject<Block> BLUE_BLOCK = BLOCKS.register("blue_block", () -> new Block(Block.Properties.of(Material.STONE)) {
        @Override
        public boolean isStickyBlock(BlockState state) {
            return true;
        }
    });

    public static final RegistryObject<Block> RED_BLOCK = BLOCKS.register("red_block", () -> new Block(Block.Properties.of(Material.STONE)) {
        @Override
        public boolean isStickyBlock(BlockState state) {
            return true;
        }

        @Override
        public boolean isSlimeBlock(BlockState state) {
            return true;
        }

        @Override
        public boolean canStickTo(BlockState state, BlockState other) {
            if (other.getBlock() == Blocks.SLIME_BLOCK) return false;
            return state.isStickyBlock() || other.isStickyBlock();
        }
    });

    private static final RegistryObject<Block> SHIFT_ON_PISTON_MOVE = BLOCKS.register("shift_on_piston_move", () -> new Block(Block.Properties.of(Material.STONE)));

    static {
        ITEMS.register("blue_block", () -> new BlockItem(BLUE_BLOCK.get(), new Item.Properties()));
        ITEMS.register("red_block", () -> new BlockItem(RED_BLOCK.get(), new Item.Properties()));

        ITEMS.register("shift_on_piston_move", () -> new BlockItem(SHIFT_ON_PISTON_MOVE.get(), new Item.Properties()));
    }

    @OnInit
    static void onInit(final TestFramework framework) {
        ITEMS.register(framework.modEventBus());
        BLOCKS.register(framework.modEventBus());
    }

    /*
     *  S
     *  R
     *
     *  B
     * LP
     */
    @RegisterStructureTemplate("examplemod:sticky_block_test")
    static final Supplier<StructureTemplate> SBT_TEMPLATE = StructureTemplateBuilder.lazy(2, 5, 1, builder -> builder
            .placeFloorLever(1, 1, 0, false)
            .set(0, 0, 0, Blocks.STICKY_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP))
            .set(0, 1, 0, BLUE_BLOCK.get().defaultBlockState())
            .set(0, 3, 0, RED_BLOCK.get().defaultBlockState())
            .set(0, 4, 0, Blocks.SLIME_BLOCK.defaultBlockState()));

    @TestHolder(
            value = "sticky_block",
            groups = ExampleMod.BLOCK_TESTS,
            description = {
                    "Tests if blue block is sticky, and red block is considered slime.",
                    "Tests if blue block does not stick to red block.",
                    "This test is GameTest-only!"
            }
    )
    @GameTest(template = "examplemod:sticky_block_test")
    static void stickyTest(final DynamicTest test) {
        test.onGameTest(helper -> helper.startSequence()
                .thenExecute(() -> helper.pullLever(1, 2, 0))
                .thenIdle(5)
                .thenWaitUntil(0, () -> helper.assertBlockPresent(BLUE_BLOCK.get(), 0, 3, 0))
                .thenExecute(() -> helper.pullLever(1, 2, 0))
                .thenIdle(5)
                .thenWaitUntil(0, () -> helper.assertBlockPresent(BLUE_BLOCK.get(), 0, 2, 0))
                .thenWaitUntil(0, () -> helper.assertBlockPresent(RED_BLOCK.get(), 0, 3, 0))
                .thenWaitUntil(0, () -> helper.assertBlockPresent(Blocks.SLIME_BLOCK, 0, 5, 0))
                .thenExecute(test::pass)
                .thenSucceed());
    }

    @RegisterStructureTemplate("examplemod:piston_event_test")
    static final Supplier<StructureTemplate> PISTON_EVENT_TEMPLATE = StructureTemplateBuilder.lazy(3, 5, 3, builder -> builder
            .placeFloorLever(1, 1, 1, false)
            .set(1, 0, 2, Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP))
            .set(1, 1, 2, Blocks.BLACK_WOOL.defaultBlockState())
            .set(1, 2, 2, SHIFT_ON_PISTON_MOVE.get().defaultBlockState())

            .set(2, 0, 1, Blocks.STICKY_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP))
            .set(2, 2, 1, Blocks.COBBLESTONE.defaultBlockState())

            .set(1, 0, 0, Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP))
            .set(1, 1, 0, Blocks.COBBLESTONE.defaultBlockState()));

    @TestHolder(
            value = "piston_event_test",
            groups = ExampleMod.BLOCK_TESTS,
            description = {
                    "This test blocks pistons from moving cobblestone at all except indirectly.",
                    "This test adds a block that moves upwards when pushed by a piston.",
                    "This test mod makes black wool pushed by a piston drop after being pushed.",
                    "This test is GameTest-only!"
            }
    )
    @GameTest(template = "examplemod:piston_event_test")
    static void pistonEventTest(final DynamicTest test) {
        test.whenEnabled(buses -> buses.getFor(Mod.EventBusSubscriber.Bus.FORGE).addListener((final PistonEvent.Pre event) -> {
            if (!(event.getLevel() instanceof Level level)) return;

            if (event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND) {
                final PistonStructureResolver pistonHelper = Objects.requireNonNull(event.getStructureHelper());

                if (pistonHelper.resolve()) {
                    for (BlockPos newPos : pistonHelper.getToPush()) {
                        final BlockState state = event.getLevel().getBlockState(newPos);
                        if (state.getBlock() == Blocks.BLACK_WOOL) {
                            Block.dropResources(state, level, newPos);
                            level.setBlockAndUpdate(newPos, Blocks.AIR.defaultBlockState());
                        }
                    }
                }

                // Make the block move up and out of the way so long as it won't replace the piston
                final BlockPos pushedBlockPos = event.getFaceOffsetPos().relative(event.getDirection());
                if (level.getBlockState(pushedBlockPos).is(SHIFT_ON_PISTON_MOVE.get()) && event.getDirection() != Direction.DOWN) {
                    level.setBlockAndUpdate(pushedBlockPos, Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(pushedBlockPos.above(), SHIFT_ON_PISTON_MOVE.get().defaultBlockState());
                }

                // Block pushing cobblestone (directly, indirectly works)
                event.setCanceled(event.getLevel().getBlockState(event.getFaceOffsetPos()).getBlock() == Blocks.COBBLESTONE);
            } else {
                final boolean isSticky = event.getLevel().getBlockState(event.getPos()).getBlock() == Blocks.STICKY_PISTON;

                // Offset twice to see if retraction will pull cobblestone
                event.setCanceled(event.getLevel().getBlockState(event.getFaceOffsetPos().relative(event.getDirection())).getBlock() == Blocks.COBBLESTONE && isSticky);
            }
        }));

        test.onGameTest(helper -> helper.startSequence()
                .thenExecute(() -> helper.pullLever(1, 2, 1))
                .thenIdle(10)

                .thenWaitUntil(0, () -> helper.assertBlockPresent(Blocks.PISTON_HEAD, 1, 2, 2)) // The piston should've extended
                .thenWaitUntil(0, () -> helper.assertBlockPresent(Blocks.AIR, 1, 3, 2)) // This is where the shift block WOULD be
                .thenWaitUntil(0, () -> helper.assertBlockPresent(SHIFT_ON_PISTON_MOVE.get(), 1, 4, 2)) // Shift block should move upwards

                .thenWaitUntil(0, () -> helper.assertBlockPresent(Blocks.COBBLESTONE, 1, 2, 0))

                .thenIdle(20)
                .thenExecute(() -> helper.pullLever(1, 2, 1))
                .thenIdle(10)
                .thenWaitUntil(0, () -> helper.assertBlockPresent(Blocks.COBBLESTONE, 2, 3, 1))
                .thenWaitUntil(0, () -> helper.assertBlockPresent(Blocks.PISTON_HEAD, 2, 2, 1))

                .thenExecute(test::pass)
                .thenSucceed());
    }
}

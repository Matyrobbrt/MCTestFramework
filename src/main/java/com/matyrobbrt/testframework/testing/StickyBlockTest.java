package com.matyrobbrt.testframework.testing;

import com.matyrobbrt.testframework.ExampleMod;
import com.matyrobbrt.testframework.TestFramework;
import com.matyrobbrt.testframework.annotation.RegisterStructureTemplate;
import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.gametest.StructureTemplateBuilder;
import com.matyrobbrt.testframework.impl.test.AbstractTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

@TestHolder(
        value = "sticky_block",
        groups = ExampleMod.BLOCK_TESTS,
        description = {
                "Tests if blue block is sticky, and red block is considered slime.",
                "Tests if blue block does not stick to red block.",
                "This test is GameTest-only."
        }
)
public class StickyBlockTest extends AbstractTest {
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

    private final RegistryObject<Item> BLUE_BLOCK_ITEM = ITEMS.register("blue_block", () -> new BlockItem(BLUE_BLOCK.get(), new Item.Properties()));
    private final RegistryObject<Item> RED_BLOCK_ITEM = ITEMS.register("red_block", () -> new BlockItem(RED_BLOCK.get(), new Item.Properties()));

    @Override
    public void init(@Nonnull TestFramework framework) {
        super.init(framework);
        BLOCKS.register(framework.modEventBus());
        ITEMS.register(framework.modEventBus());
    }

    /*
     *  S
     *  R
     *
     *  B
     * LP
     */
    @RegisterStructureTemplate("examplemod:sticky_block_test")
    static final Supplier<StructureTemplate> TEMPLATE = StructureTemplateBuilder.lazy(2, 5, 1, builder -> builder
            .placeFloorLever(1, 1, 0, false)
            .set(0, 0, 0, Blocks.STICKY_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP))
            .set(0, 1, 0, BLUE_BLOCK.get().defaultBlockState())
            .set(0, 3, 0, RED_BLOCK.get().defaultBlockState())
            .set(0, 4, 0, Blocks.SLIME_BLOCK.defaultBlockState()));

    @Override
    @GameTest(template = "examplemod:sticky_block_test")
    protected void onGameTest(@Nonnull GameTestHelper helper) {
        helper.startSequence()
                .thenExecute(() -> helper.pullLever(1, 2, 0))
                .thenIdle(5)
                .thenWaitUntil(0, () -> helper.assertBlockPresent(BLUE_BLOCK.get(), 0, 3, 0))
                .thenExecute(() -> helper.pullLever(1, 2, 0))
                .thenIdle(5)
                .thenWaitUntil(0, () -> helper.assertBlockPresent(BLUE_BLOCK.get(), 0, 2, 0))
                .thenWaitUntil(0, () -> helper.assertBlockPresent(RED_BLOCK.get(), 0, 3, 0))
                .thenWaitUntil(0, () -> helper.assertBlockPresent(Blocks.SLIME_BLOCK, 0, 5, 0))
                .thenExecute(this::pass)
                .thenSucceed();
    }
}

package com.matyrobbrt.testframework.testing;

import com.matyrobbrt.testframework.ExampleMod;
import com.matyrobbrt.testframework.TestFramework;
import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.impl.test.AbstractTest;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@TestHolder(
        value = "sticky_block",
        groups = ExampleMod.BLOCK_TESTS,
        description = {
                "Tests if blue block is sticky, and red block is considered slime.",
                "Tests if blue block does not stick to red block.",
        }
)
public class StickyBlockTest extends AbstractTest {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "examplemod");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "examplemod");

    private final RegistryObject<Block> blueBlock = BLOCKS.register("blue_block", () -> new Block(Block.Properties.of(Material.STONE)) {
        @Override
        public boolean isStickyBlock(BlockState state) {
            return true;
        }
    });

    private final RegistryObject<Block> redBlock = BLOCKS.register("red_block", () -> new Block(Block.Properties.of(Material.STONE)) {
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
            if (state.getBlock() == redBlock.get() && other.getBlock() == Blocks.SLIME_BLOCK) {
                pass();
                return false;
            }
            return state.isStickyBlock() || other.isStickyBlock();
        }
    });

    private final RegistryObject<Item> blueBlockItem = ITEMS.register("blue_block", () -> new BlockItem(blueBlock.get(), new Item.Properties()));
    private final RegistryObject<Item> redBlockItem = ITEMS.register("red_block", () -> new BlockItem(redBlock.get(), new Item.Properties()));

    @Override
    public void init(TestFramework framework) {
        super.init(framework);
        BLOCKS.register(framework.modEventBus());
        ITEMS.register(framework.modEventBus());
    }
}

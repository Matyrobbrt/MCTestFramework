package com.matyrobbrt.testframework.testing;

import com.matyrobbrt.testframework.annotation.RegisterStructureTemplate;
import com.matyrobbrt.testframework.annotation.TestGroup;
import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.collector.CollectorType;
import com.matyrobbrt.testframework.collector.Collectors;
import com.matyrobbrt.testframework.conf.ClientConfiguration;
import com.matyrobbrt.testframework.conf.Feature;
import com.matyrobbrt.testframework.conf.FrameworkConfiguration;
import com.matyrobbrt.testframework.gametest.StructureTemplateBuilder;
import com.matyrobbrt.testframework.impl.TestFrameworkInternal;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@Mod("examplemod")
public class ExampleMod {
    @TestGroup(name = "Ungrouped")
    public static final String UNGROUPED = "ungrouped";

    @TestGroup(name = "Events")
    public static final String EVENTS = "events";
    @TestGroup(name = "Entity Events")
    public static final String ENTITY_EVENTS = "events.entity";
    @TestGroup(name = "Level-Related Events", enabledByDefault = true)
    public static final String LEVEL_RELATED_EVENTS = "events.level_related";

    @TestGroup(name = "Client Tests")
    public static final String CLIENT_TESTS = "client";
    @TestGroup(name = "Blocks", parents = LEVEL_RELATED_EVENTS)
    public static final String BLOCK_TESTS = "blocks";

    @RegisterStructureTemplate("examplemod:test_template")
    static final StructureTemplate TEMPLATE = StructureTemplateBuilder.withSize(13, 13, 13)
            .set(7, 7, 7, Blocks.ACACIA_LOG.defaultBlockState())
            .build();

    @RegisterStructureTemplate("examplemod:empty_1x1")
    static final StructureTemplate EMPTY_1x1 = StructureTemplateBuilder.empty(1, 1, 1);

    public ExampleMod() {
        final TestFrameworkInternal framework = FrameworkConfiguration.builder(new ResourceLocation("examplemod:tests"))
                .clientConfiguration(() -> ClientConfiguration.builder()
                        .toggleOverlayKey(GLFW.GLFW_KEY_J)
                        .openManagerKey(GLFW.GLFW_KEY_N)
                        .build())

                .enable(Feature.CLIENT_SYNC, Feature.CLIENT_MODIFICATIONS, Feature.TEST_STORE)

                .withCollector(CollectorType.TESTS, Collectors.Tests.forMethodsWithAnnotation(TestHolder.class))
                .withCollector(CollectorType.TESTS, Collectors.Tests.forClassesWithAnnotation(TestHolder.class))
                .withCollector(CollectorType.TESTS, Collectors.Tests.eventTestMethodsWithAnnotation(TestHolder.class))

                .withCollector(CollectorType.INIT_LISTENERS, Collectors.defaultOnInitCollector())
                .withCollector(CollectorType.STRUCTURE_TEMPLATES, Collectors.defaultTemplateCollector())
                .withCollector(CollectorType.GROUP_DATA, Collectors.defaultGroupCollector())

                .build().create();
        framework.init(FMLJavaModLoadingContext.get().getModEventBus(), ModLoadingContext.get().getActiveContainer());

        MinecraftForge.EVENT_BUS.addListener((final RegisterCommandsEvent event) -> {
            final LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("tests");
            framework.registerCommands(node);
            event.getDispatcher().register(node);
        });
    }
}

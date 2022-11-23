package com.matyrobbrt.testframework;

import com.matyrobbrt.testframework.annotation.TestGroup;
import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.conf.ClientConfiguration;
import com.matyrobbrt.testframework.conf.FrameworkConfiguration;
import com.matyrobbrt.testframework.impl.TestFrameworkImpl;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    public ExampleMod() {
        final TestFrameworkImpl framework = new TestFrameworkImpl(FrameworkConfiguration.builder(new ResourceLocation("examplemod:tests"))
                .clientConfiguration(() -> ClientConfiguration.builder()
                        .toggleOverlayKey(GLFW.GLFW_KEY_J)
                        .openManagerKey(GLFW.GLFW_KEY_N)
                        .build())
                .allowClientModifications().syncToClients()
                .testCollector(FrameworkConfiguration.TestCollector.withAnnotation(TestHolder.class))
                .groupNameCollector(TestGroup.class, it -> Component.literal(it.name()), TestGroup::enabledByDefault)
                .build());
        framework.init(FMLJavaModLoadingContext.get().getModEventBus(), ModLoadingContext.get().getActiveContainer());

        MinecraftForge.EVENT_BUS.addListener((final RegisterCommandsEvent event) -> {
            final LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("tests");
            framework.registerCommands(node);
            event.getDispatcher().register(node);
        });
    }
}

package com.matyrobbrt.testframework;

import com.matyrobbrt.testframework.impl.TestFrameworkImpl;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.Type;

import java.util.List;

@Mod("examplemod")
public class ExampleMod {
    public ExampleMod() {
        final Framework framework = new Framework(new ResourceLocation("examplemod:tests"));
        framework.init(FMLJavaModLoadingContext.get().getModEventBus(), ModLoadingContext.get().getActiveContainer());
        new TestFrameworkImpl.Client(framework, GLFW.GLFW_KEY_J).init(FMLJavaModLoadingContext.get().getModEventBus());

        MinecraftForge.EVENT_BUS.addListener((final RegisterCommandsEvent event) -> {
            final LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("tests");
            framework.registerCommands(node);
            event.getDispatcher().register(node);
        });
    }

    private static final class Framework extends TestFrameworkImpl {
        private static final Type ANNOTATION = Type.getType(TestHolder.class);

        public Framework(ResourceLocation id) {
            super(id);
        }

        @Override
        public List<Test> collectTests(ModContainer container) {
            return container.getModInfo().getOwningFile().getFile().getScanResult()
                    .getAnnotations().stream().filter(it -> ANNOTATION.equals(it.annotationType()))
                    .map(LamdbaExceptionUtils.rethrowFunction(annotationData -> {
                        final Class<?> clazz = Class.forName(annotationData.clazz().getClassName());
                        return (Test) clazz.getDeclaredConstructor().newInstance();
                    })).toList();
        }
    }
}

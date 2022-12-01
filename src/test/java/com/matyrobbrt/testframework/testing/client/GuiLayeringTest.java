package com.matyrobbrt.testframework.testing.client;

import com.matyrobbrt.testframework.testing.ExampleMod;
import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.impl.test.AbstractTest;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Random;

@TestHolder(
        value = "gui_layering",
        groups = ExampleMod.CLIENT_TESTS,
        description = "Checks if GUI layer works.",
        side = Dist.CLIENT
)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GuiLayeringTest extends AbstractTest {
    public final class TestLayer extends Screen {
        private static final Random RANDOM = new Random();
        protected TestLayer(Component titleIn) {
            super(titleIn);
        }

        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
            this.renderBackground(poseStack);
            drawString(poseStack, this.font, this.title, this.width / 2, 15, 0xFFFFFF);
            super.render(poseStack, mouseX, mouseY, partialTicks);
        }

        @Override
        protected void init() {
            int buttonWidth = 150;
            int buttonHeight = 20;
            int buttonGap = 4;
            int buttonSpacing = (buttonHeight + buttonGap);
            int buttons = 3;

            int xoff = (this.width - buttonWidth);
            int yoff = (this.height - buttonHeight - buttonSpacing * (buttons - 1));
            int cnt = 0;

            xoff = RANDOM.nextInt(xoff);
            yoff = RANDOM.nextInt(yoff);

            this.addRenderableWidget(new Button(xoff, yoff + buttonSpacing * (cnt++), buttonWidth, buttonHeight, Component.literal("Push New Layer"), this::pushLayerButton));
            this.addRenderableWidget(new Button(xoff, yoff + buttonSpacing * (cnt++), buttonWidth, buttonHeight, Component.literal("Pop Current Layer"), this::popLayerButton));
            this.addRenderableWidget(new Button(xoff, yoff + buttonSpacing * (cnt++), buttonWidth, buttonHeight, Component.literal("Close entire stack"), this::closeStack));
        }

        private void closeStack(Button button) {
            this.minecraft.setScreen(null);
            requestConfirmation(Minecraft.getInstance().player, Component.literal("Did you see the GUI layers?"));
        }

        private void popLayerButton(Button button) {
            this.minecraft.popGuiLayer();
        }

        private void pushLayerButton(Button button) {
            this.minecraft.pushGuiLayer(new TestLayer(Component.literal("LayerScreen")));
        }
    }

    @Override
    public void onEnabled(EventListenerGroup buses) {
        if (FMLLoader.getDist().isClient()) {
            clientEnable(buses.getFor(Mod.EventBusSubscriber.Bus.FORGE));
        }
    }

    private void clientEnable(EventListenerGroup.EventListenerCollector bus) {
        bus.addListener((final ScreenEvent.Init event) -> {
            if (event.getScreen() instanceof AbstractContainerScreen) {
                event.addListener(new Button(2, 2, 150, 20, Component.literal("Test Gui Layering"), btn -> {
                    Minecraft.getInstance().pushGuiLayer(new TestLayer(Component.literal("LayerScreen")));
                }));
                event.addListener(new Button(2, 25, 150, 20, Component.literal("Test Gui Normal"), btn -> {
                    Minecraft.getInstance().setScreen(new TestLayer(Component.literal("LayerScreen")));
                }));
            }
        });
    }
}

package com.matyrobbrt.testframework.impl;

import com.matyrobbrt.testframework.Test;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TestsOverlay implements IGuiOverlay {
    public static final int MAX_DISPLAYED = 5;

    private final TestFrameworkImpl impl;
    private final BooleanSupplier enabled;

    private final Object2FloatMap<Test> fading = new Object2FloatOpenHashMap<>();
    private final List<Test> lastRenderedTests = new ArrayList<>(MAX_DISPLAYED);

    public TestsOverlay(TestFrameworkImpl impl, BooleanSupplier enabled) {
        this.impl = impl;
        this.enabled = enabled;
        fading.defaultReturnValue(1f);
    }

    @Override
    public void render(ForgeGui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        if (!enabled.getAsBoolean()) return;

        final List<Test> enabled = impl.tests().enabled().toList();
        if (enabled.isEmpty()) return;

        final Font font = gui.getFont();
        int startX = 10, startY = 10;
        Screen.drawString(poseStack, font, Component.literal("Tests overlay for ").append(Component.literal(impl.id().toString()).withStyle(ChatFormatting.AQUA)), startX, startX, 0xffffff);
        startY += font.lineHeight + 5;

        if (enabled.size() > MAX_DISPLAYED) {
            // In this case, we only render the first 5 which are NOT passed
            // But keeping the last completed ones, if present, and fading them out
            final Map<Test, Integer> lastCompleted = lastRenderedTests.stream()
                    .filter(it -> it.status().result() == Test.Result.PASSED)
                    .collect(Collectors.toMap(Function.identity(), lastRenderedTests::indexOf));

            final List<Test> actuallyToRender = new ArrayList<>(MAX_DISPLAYED);
            for (int i = 0; i < MAX_DISPLAYED; i++) actuallyToRender.add(null);
            lastCompleted.forEach((test, index) -> actuallyToRender.set(index, test));
            enabled.stream()
                    .filter(it -> it.status().result() != Test.Result.PASSED)
                    .limit(MAX_DISPLAYED - lastCompleted.size())
                    .forEach(it -> actuallyToRender.set(actuallyToRender.indexOf(null), it));

            int nullIndex;
            while ((nullIndex = actuallyToRender.indexOf(null)) >= 0) {
                actuallyToRender.remove(nullIndex);
            }

            for (final Test test : List.copyOf(actuallyToRender)) {
                // If we find one that isn't passed, we need to start fading it out
                if (test.status().result() == Test.Result.PASSED) {
                    final float fade = fading.computeIfAbsent(test, it -> 1f) - 0.005f;
                    if (fade <= 0) {
                        fading.removeFloat(test);
                        actuallyToRender.remove(test);
                        continue; // We don't need to render this one anymore, hurray!
                    }

                    // TODO - figure out why fading doesn't work
                    final float[] oldColour = RenderSystem.getShaderColor();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(1f, 1f, 1f, fade);

                    startY = renderTest(gui, font, test, poseStack, startX, startY, ((int)(fade * 255f) << 24) | 0xffffff) + 5;

                    RenderSystem.disableBlend();
                    RenderSystem.setShaderColor(oldColour[0], oldColour[1], oldColour[2], oldColour[3]);
                    fading.put(test, fade);
                } else {
                    startY = renderTest(gui, font, test, poseStack, startX, startY, 0xffffff) + 5;
                }
            }

            lastRenderedTests.clear();
            lastRenderedTests.addAll(actuallyToRender);
        } else {
            for (final Test test : enabled) {
                startY = renderTest(gui, font, test, poseStack, startX, startY, 0xffffff) + 5;
            }
            lastRenderedTests.clear();
            lastRenderedTests.addAll(enabled);
        }
    }

    private static final Map<Test.Result, ResourceLocation> ICON_BY_RESULT = new EnumMap<>(Map.of(
            Test.Result.FAILED, new ResourceLocation("testframework", "textures/gui/test_failed.png"),
            Test.Result.PASSED, new ResourceLocation("testframework", "textures/gui/test_passed.png"),
            Test.Result.NOT_PROCESSED, new ResourceLocation("testframework", "textures/gui/test_not_processed.png")
    ));

    private int renderTest(ForgeGui gui, Font font, Test test, PoseStack stack, int x, int y, int colour) {
        final FormattedCharSequence bullet = Component.literal("• ").withStyle(ChatFormatting.BLACK).getVisualOrderText();
        Screen.drawString(stack, font, bullet, x, y - 1, colour);
        x += font.width(bullet) + 1;

        RenderSystem.setShaderTexture(0, ICON_BY_RESULT.get(test.status().result()));
        GuiComponent.blit(stack, x, y, 0, 0, 9, 9, 9, 9);
        x += 11;

        final Component title = statusColoured(test.visuals().title(), test.status()).append(":");
        Screen.drawString(stack, font, title, x, y, colour);

        final List<Component> extras = new ArrayList<>();
        if (Screen.hasShiftDown()) extras.addAll(test.visuals().description());
        if (test.status().result() != Test.Result.PASSED && !test.status().message().isBlank()) {
            extras.add(Component.literal("!!! " + test.status().message()).withStyle(ChatFormatting.RED));
        }

        y += font.lineHeight + 2;
        if (!extras.isEmpty()) {
            x += 6;
            for (final Component extra : extras) {
                Screen.drawString(stack, font, extra, x, y, 0xffffff);
                y += font.lineHeight;
            }
        }
        return y;
    }

    private MutableComponent statusColoured(Component input, Test.Status status) {
        return switch (status.result()) {
            case PASSED -> input.copy().withStyle(ChatFormatting.GREEN);
            case FAILED -> input.copy().withStyle(ChatFormatting.RED);
            case NOT_PROCESSED -> input.copy();
        };
    }
}

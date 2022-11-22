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
    // TODO - this will need to be rendered with transparency instead of using an already-transparent texture
    public static final ResourceLocation BG_TEXTURE = new ResourceLocation("testframework", "textures/gui/background2.png");

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
        final int startX = 10, startY = 10;
        final int maxWidth = screenWidth / 3;
        int x = startX, y = startY;
        int maxX = x;

        final List<Runnable> renderingQueue = new ArrayList<>();
        final Component title = Component.literal("Tests overlay for ").append(Component.literal(impl.id().toString()).withStyle(ChatFormatting.AQUA));
        renderingQueue.add(() ->  Screen.drawString(poseStack, font, title, x, x, 0xffffff));
        y += font.lineHeight + 5;
        maxX += font.width(title);

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

                    renderingQueue.add(() -> {
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                    });

                    final XY xy = renderTest(gui, font, test, poseStack, maxWidth, x, y, ((int)(fade * 255f) << 24) | 0xffffff, renderingQueue);
                    y = xy.y() + 5;
                    maxX = Math.max(maxX, xy.x());

                    renderingQueue.add(RenderSystem::disableBlend);
                    fading.put(test, fade);
                } else {
                    final XY xy = renderTest(gui, font, test, poseStack, maxWidth, x, y, 0xffffff, renderingQueue);
                    y = xy.y() + 5;
                    maxX = Math.max(maxX, xy.x());
                }
            }

            lastRenderedTests.clear();
            lastRenderedTests.addAll(actuallyToRender);
        } else {
            for (final Test test : enabled) {
                final XY xy = renderTest(gui, font, test, poseStack, maxWidth, x, y, 0xffffff, renderingQueue);
                y = xy.y() + 5;
                maxX = Math.max(maxX, xy.x());
            }
            lastRenderedTests.clear();
            lastRenderedTests.addAll(enabled);
        }

        maxX += 3;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // final float[] oldColour = RenderSystem.getShaderColor();
        // RenderSystem.setShaderColor(1f, 1f, 1f, 0f);
        // RenderSystem.setShaderTexture(0, BG_TEXTURE);

        renderTilledTexture(poseStack, BG_TEXTURE, startX - 4, startY - 4, (maxX - startX) + 4 + 4, (y - startY) + 4 + 4, 4, 4, 256, 256);

        RenderSystem.disableBlend();
        // RenderSystem.setShaderColor(oldColour[0], oldColour[1], oldColour[2], oldColour[3]);

        renderingQueue.forEach(Runnable::run);
    }

    private static final Map<Test.Result, ResourceLocation> ICON_BY_RESULT = new EnumMap<>(Map.of(
            Test.Result.FAILED, new ResourceLocation("testframework", "textures/gui/test_failed.png"),
            Test.Result.PASSED, new ResourceLocation("testframework", "textures/gui/test_passed.png"),
            Test.Result.NOT_PROCESSED, new ResourceLocation("testframework", "textures/gui/test_not_processed.png")
    ));

    // TODO - maybe "group" together tests in the same group?
    private XY renderTest(ForgeGui gui, Font font, Test test, PoseStack stack, int maxWidth, int x, int y, int colour, List<Runnable> rendering) {
        final FormattedCharSequence bullet = Component.literal("• ").withStyle(ChatFormatting.BLACK).getVisualOrderText();
        rendering.add(withXY(x, y, (x$, y$) -> Screen.drawString(stack, font, bullet, x$, y$ - 1, colour)));
        x += font.width(bullet) + 1;

        rendering.add(withXY(x, y, (x$, y$) -> {
            RenderSystem.setShaderTexture(0, ICON_BY_RESULT.get(test.status().result()));
            GuiComponent.blit(stack, x$, y$, 0, 0, 9, 9, 9, 9);
        }));
        x += 11;

        final Component title = statusColoured(test.visuals().title(), test.status()).append(":");
        rendering.add(withXY(x, y, (x$, y$) -> Screen.drawString(stack, font, title, x$, y$, colour)));

        final List<FormattedCharSequence> extras = new ArrayList<>();
        if (Screen.hasShiftDown()) extras.addAll(test.visuals().description().stream().flatMap(it -> font.split(it, maxWidth).stream()).toList());
        if (test.status().result() != Test.Result.PASSED && !test.status().message().isBlank()) {
            extras.add(Component.literal("!!! " + test.status().message()).withStyle(ChatFormatting.RED).getVisualOrderText());
        }

        int maxX = x;
        y += font.lineHeight + 2;
        if (!extras.isEmpty()) {
            x += 6;
            for (final FormattedCharSequence extra : extras) {
                rendering.add(withXY(x, y, (x$, y$) -> Screen.drawString(stack, font, extra, x$, y$, 0xffffff)));
                y += font.lineHeight;
                maxX = Math.max(maxX, x + font.width(extra));
            }
        }
        return new XY(maxX, y);
    }

    private record XY(int x, int y) {}

    private Runnable withXY(int x, int y, IntBiConsumer consumer) {
        return () -> consumer.accept(x, y);
    }

    private MutableComponent statusColoured(Component input, Test.Status status) {
        return switch (status.result()) {
            case PASSED -> input.copy().withStyle(ChatFormatting.GREEN);
            case FAILED -> input.copy().withStyle(ChatFormatting.RED);
            case NOT_PROCESSED -> input.copy();
        };
    }

    private static void renderTilledTexture(PoseStack pose, ResourceLocation texture, int x, int y, int width, int height, int borderWidth, int borderHeight, int textureWidth, int textureHeight) {
        final var sideWidth = Math.min(borderWidth, width / 2);
        final var sideHeight = Math.min(borderHeight, height / 2);

        final var leftWidth = sideWidth < borderWidth ? sideWidth + (width % 2) : sideWidth;
        final var topHeight = sideHeight < borderHeight ? sideHeight + (height % 2) : sideHeight;

        // Calculate texture centre
        final int textureCentreWidth = textureWidth - borderWidth * 2,
                textureCenterHeight = textureHeight - borderHeight * 2;
        final int centreWidth = width - leftWidth - sideWidth,
                centerHeight = height - topHeight - sideHeight;

        // Calculate the corner positions
        final var leftEdgeEnd = x + leftWidth;
        final var rightEdgeStart = leftEdgeEnd + centreWidth;
        final var topEdgeEnd = y + topHeight;
        final var bottomEdgeStart = topEdgeEnd + centerHeight;
        RenderSystem.setShaderTexture(0, texture);

        // Top Left Corner
        Screen.blit(pose, x, y, 0, 0, leftWidth, topHeight, textureWidth, textureHeight);
        // Bottom Left Corner
        Screen.blit(pose, x, bottomEdgeStart, 0, textureHeight - sideHeight, leftWidth, sideHeight, textureWidth, textureHeight);

        // Render the Middle
        if (centreWidth > 0) {
            // Top Middle
            blitTiled(pose, leftEdgeEnd, y, centreWidth, topHeight, borderWidth, 0, textureCentreWidth, borderHeight, textureWidth, textureHeight);
            if (centerHeight > 0) {
                // Centre
                blitTiled(pose, leftEdgeEnd, topEdgeEnd, centreWidth, centerHeight, borderWidth, borderHeight, textureCentreWidth, textureCenterHeight, textureWidth, textureHeight);
            }
            // Bottom Middle
            blitTiled(pose, leftEdgeEnd, bottomEdgeStart, centreWidth, sideHeight, borderWidth, textureHeight - sideHeight, textureCentreWidth, borderHeight, textureWidth, textureHeight);
        }

        if (centerHeight > 0) {
            // Left Middle
            blitTiled(pose, x, topEdgeEnd, leftWidth, centerHeight, 0, borderHeight, borderWidth, textureCenterHeight, textureWidth, textureHeight);
            // Right Middle
            blitTiled(pose, rightEdgeStart, topEdgeEnd, sideWidth, centerHeight, textureWidth - sideWidth, borderHeight, borderWidth, textureCenterHeight, textureWidth, textureHeight);
        }

        // Top Right Corner
        Screen.blit(pose, rightEdgeStart, y, textureWidth - sideWidth, 0, sideWidth, topHeight, textureWidth, textureHeight);
        // Bottom Right Corner
        Screen.blit(pose, rightEdgeStart, bottomEdgeStart, textureWidth - sideWidth, textureHeight - sideHeight, sideWidth, sideHeight, textureWidth, textureHeight);
    }

    private static void blitTiled(PoseStack pose, int x, int y, int width, int height, int u, int v, int textureDrawWidth, int textureDrawHeight, int textureWidth, int textureHeight) {
        // Calculate the amount of tiles
        final int xTiles = (int) Math.ceil((float) width / textureDrawWidth),
                yTiles = (int) Math.ceil((float) height / textureDrawHeight);

        var drawWidth = width;
        var drawHeight = height;
        for (var tileX = 0; tileX < xTiles; tileX++) {
            for (var tileY = 0; tileY < yTiles; tileY++) {
                final var renderWidth = Math.min(drawWidth, textureDrawWidth);
                final var renderHeight = Math.min(drawHeight, textureDrawHeight);
                Screen.blit(pose, x + textureDrawWidth * tileX, y + textureDrawHeight * tileY, u, v, renderWidth, renderHeight, textureWidth, textureHeight);
                // We rendered a tile
                drawHeight -= textureDrawHeight;
            }
            drawWidth -= textureDrawWidth;
            drawHeight = height;
        }
    }

    @FunctionalInterface
    public interface IntBiConsumer {
        void accept(int x, int y);
    }
}

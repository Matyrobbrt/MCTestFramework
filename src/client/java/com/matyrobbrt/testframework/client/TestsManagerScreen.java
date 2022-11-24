package com.matyrobbrt.testframework.client;

import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.group.Group;
import com.matyrobbrt.testframework.impl.TestFrameworkInternal;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class TestsManagerScreen extends Screen {
    protected final TestFrameworkInternal framework;
    public TestsManagerScreen(Component title, TestFrameworkInternal framework) {
        super(title);
        this.framework = framework;
    }

    protected final class GroupableList extends ObjectSelectionList<GroupableList.Entry> {
        private final Function<String, List<? extends Entry>> entryGetter;

        public GroupableList(Function<String, List<? extends Entry>> entryGetter, Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight) {
            super(pMinecraft, pWidth, pHeight, pY0, pY1, pItemHeight);
            this.entryGetter = entryGetter;
        }

        public GroupableList(BooleanSupplier isGrouped, List<Group> groups, Supplier<Stream<Test>> tests, Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight) {
            super(pMinecraft, pWidth, pHeight, pY0, pY1, pItemHeight);
            this.entryGetter = search -> isGrouped.getAsBoolean() ?
                    groups.stream()
                            .filter(it -> it.title().getString().toLowerCase(Locale.ROOT).contains(search))
                            .sorted(Comparator.comparing(gr -> gr.title().getString()))
                            .map(GroupEntry::new).toList()
                    :
                    tests.get()
                    .filter(it -> it.visuals().title().getString().toLowerCase(Locale.ROOT).contains(search))
                    .sorted(Comparator.comparing(test -> test.visuals().title().getString()))
                    .map(TestEntry::new).toList();
        }

        public void resetRows(String search) {
            this.clearEntries();
            entryGetter.apply(search).forEach(this::addEntry);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 144;
        }

        @Override
        public int getRowWidth() {
            return 260;
        }

        @Override
        public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
            renderTooltips(pPoseStack, pMouseX, pMouseY);
        }

        private void renderTooltips(PoseStack poseStack, int mouseX, int mouseY) {
            if (this.isMouseOver(mouseX, mouseY)) {
                Entry entry = this.getEntryAtPosition(mouseX, mouseY);
                if (entry != null) {
                    entry.renderTooltips(poseStack, mouseX, mouseY);
                }
            }
        }

        protected abstract sealed class Entry extends ObjectSelectionList.Entry<Entry> permits TestEntry, GroupEntry {
            @Override
            public Component getNarration() {
                return Component.empty();
            }

            public abstract boolean isEnabled();

            public boolean canDisable() {
                return isEnabled();
            }
            public boolean canEnable() {
                return !isEnabled();
            }

            public abstract void enable(boolean enable);
            public abstract void reset();

            @Override
            public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
                if (pButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    setSelected(this);
                    return true;
                } else if (pButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    enable(!isEnabled());
                } else if (pButton == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                    reset();
                }
                return false;
            }

            protected void renderTooltips(PoseStack poseStack, int mouseX, int mouseY) {}
        }

        protected final class TestEntry extends Entry {
            private final Test test;
            private TestEntry(Test test) {
                this.test = test;
            }

            @Override
            public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
                pLeft += 2;
                pTop += 2;

                final Test.Status status = framework.tests().getStatus(test.id());

                final float alpha = .45f;
                final boolean renderTransparent = !isEnabled();
                RenderSystem.setShaderTexture(0, TestsOverlay.ICON_BY_RESULT.get(status.result()));
                if (renderTransparent) RenderSystem.enableBlend();
                ClientUtils.blitAlpha(pPoseStack, pLeft, pTop, 0, 0, 9, 9, 9, 9, renderTransparent ? alpha : 1f);
                if (renderTransparent) RenderSystem.disableBlend();
                final Component title = TestsOverlay.statusColoured(test.visuals().title(), status);
                Screen.drawString(pPoseStack, font, title, pLeft + 11, pTop, renderTransparent ? ((((int) (alpha * 255f)) << 24) | 0xffffff0) : 0xffffff);
            }

            @Override
            protected void renderTooltips(PoseStack poseStack, int mouseX, int mouseY) {
                final List<FormattedCharSequence> tooltip = new ArrayList<>();
                if (!isEnabled()) {
                    tooltip.add(Component.literal("DISABLED").withStyle(ChatFormatting.GRAY).getVisualOrderText());
                }

                final Test.Status status = framework.tests().getStatus(test.id());
                if (!status.message().isBlank()) {
                    tooltip.add(Component.literal("!!! ").append(status.message()).withStyle(ChatFormatting.RED).getVisualOrderText());
                }

                for (final Component desc : test.visuals().description()) {
                    tooltip.addAll(font.split(desc, 200));
                }

                if (!tooltip.isEmpty()) {
                    renderTooltip(poseStack, tooltip, mouseX, mouseY);
                }
            }

            @Override
            public boolean isEnabled() {
                return framework.tests().isEnabled(test.id());
            }

            @Override
            public void enable(boolean enable) {
                framework.setEnabled(test, enable, minecraft.player);
            }

            @Override
            public void reset() {
                framework.changeStatus(test, new Test.Status(Test.Result.NOT_PROCESSED, ""), null);
            }
        }

        protected final class GroupEntry extends Entry {
            private final Group group;
            private final Button browseButton;

            private GroupEntry(Group group) {
                this.group = group;
                this.browseButton = new Button(0, 0, 50, 12, Component.literal("Browse"), button -> openBrowseGUI());
            }

            @Override
            public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
                Screen.drawString(pPoseStack, font, getTitle(), pLeft + 11, pTop + 2, 0xffffff);
                this.browseButton.x = pLeft + pWidth - 53;
                this.browseButton.y = pTop - 1;
                pPoseStack.pushPose();
                pPoseStack.translate(0, 0, 100);
                browseButton.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
                pPoseStack.popPose();
            }

            @Override
            protected void renderTooltips(PoseStack poseStack, int mouseX, int mouseY) {
                final List<Test> all = group.resolveAll();
                final int enabledCount = (int) all.stream().filter(it -> framework.tests().isEnabled(it.id())).count();
                if (enabledCount == all.size()) {
                    renderTooltip(poseStack, Component.literal("All tests in group are enabled!").withStyle(ChatFormatting.GREEN), mouseX, mouseY);
                } else if (enabledCount == 0) {
                    renderTooltip(poseStack, Component.literal("All tests in group are disabled!").withStyle(ChatFormatting.GRAY), mouseX, mouseY);
                } else {
                    renderTooltip(poseStack, Component.literal(enabledCount + "/" + all.size() + " tests enabled!").withStyle(ChatFormatting.BLUE), mouseX, mouseY);
                }
            }

            private Component getTitle() {
                return group.title();
            }

            @Override
            public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
                if (browseButton.isMouseOver(pMouseX, pMouseY)) return browseButton.mouseClicked(pMouseX, pMouseY, pButton);
                if (pButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && (Screen.hasShiftDown() || Screen.hasControlDown())) {
                    openBrowseGUI();
                    return false;
                }
                return super.mouseClicked(pMouseX, pMouseY, pButton);
            }

            private void openBrowseGUI() {
                Minecraft.getInstance().pushGuiLayer(new GroupTestsManagerScreen(
                        Component.literal("Tests of group ").append(getTitle()),
                        framework, List.of(group)
                ) {
                    @Override
                    protected void init() {
                        super.init();
                        showAsGroup.visible = false;
                        showAsGroup.active = false;
                        showAsGroup.setValue(false);
                        groupableList.resetRows("");

                        addRenderableWidget(new Button(this.width - 20 - 60, this.height - 29, 60, 20, CommonComponents.GUI_BACK, (p_97691_) -> this.onClose()));
                    }
                });
            }

            @Override
            public boolean isEnabled() {
                return group.resolveAll().stream().allMatch(it -> framework.tests().isEnabled(it.id()));
            }

            @Override
            public boolean canDisable() {
                return group.resolveAll().stream().anyMatch(it -> framework.tests().isEnabled(it.id()));
            }

            @Override
            public boolean canEnable() {
                return group.resolveAll().stream().anyMatch(it -> !framework.tests().isEnabled(it.id()));
            }

            @Override
            public void enable(boolean enable) {
                group.resolveAll().forEach(test -> framework.setEnabled(test, enable, null));
            }

            @Override
            public void reset() {
                group.resolveAll().forEach(test -> framework.changeStatus(test, new Test.Status(Test.Result.NOT_PROCESSED, ""), null));
            }
        }
    }

}

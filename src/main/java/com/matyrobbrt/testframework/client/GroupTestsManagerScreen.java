package com.matyrobbrt.testframework.client;

import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.group.Group;
import com.matyrobbrt.testframework.impl.TestFrameworkImpl;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

public class GroupTestsManagerScreen extends TestsManagerScreen {
    protected FocusedEditBox activeTextField;
    protected FocusedEditBox searchTextField;

    private final List<Group> groups;
    protected GroupableList groupableList;
    private Consumer<String> suggestionProvider;
    protected CycleButton<Boolean> showAsGroup;
    protected CycleButton<FilterMode> filterMode;

    public GroupTestsManagerScreen(Component title, TestFrameworkImpl framework, List<Group> groups) {
        super(title, framework);
        this.groups = groups;
    }

    @Override
    protected void init() {
        final Runnable reloader = () -> groupableList.resetRows(searchTextField.getValue());
        this.showAsGroup = addRenderableWidget(CycleButton.booleanBuilder(Component.literal("Show groups"),
                Component.literal("Show all tests"))
                .displayOnlyValue().create(20, this.height - 26, 100, 20, Component.empty(), (pCycleButton, pValue) -> reloader.run()));
        this.filterMode = addRenderableWidget(CycleButton.<FilterMode>builder(mode -> mode.name)
                .withValues(FilterMode.values()).create((this.width - 160) / 2 , this.height - 26, 150, 20, Component.literal("Filter"), (pCycleButton, pValue) -> reloader.run()));

        final List<Test> tests = groups.stream().flatMap(it -> it.resolveAll().stream()).distinct().toList();
        this.groupableList = new GroupableList(() -> showAsGroup.getValue(), groups, () -> tests.stream()
                .filter(test -> filterMode.getValue().test(framework, test)), minecraft, width, height, 50, height - 36, 9 + 2 + 2 + 2);
        this.suggestionProvider = s -> {
            if (showAsGroup.getValue()) {
                updateSearchTextFieldSuggestion(this.searchTextField, s, groups, gr -> gr.title().getString());
            } else {
                updateSearchTextFieldSuggestion(this.searchTextField, s, tests, test -> test.visuals().title().getString());
            }
        };

        groupableList.resetRows("");
        this.addWidget(groupableList);

        this.searchTextField = new FocusedEditBox(this.font, this.width / 2 - 110, 22, 220, 20, Component.literal("Search"));
        this.searchTextField.setResponder(s -> {
            suggestionProvider.accept(s);
            groupableList.resetRows(s.toLowerCase(Locale.ROOT));
            if (!s.isEmpty()) {
                this.groupableList.setScrollAmount(0);
            }
        });
        this.addWidget(searchTextField);

        addRenderableWidget(new Button(searchTextField.x - 43, searchTextField.y, 40, 20, Component.literal("Disable"), pButton -> groupableList.getSelected().enable(false)) {
            @Override
            public void render(@NotNull PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
                this.active = groupableList != null && groupableList.getSelected() != null && groupableList.getSelected().canDisable();
                super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
            }
        });
        addRenderableWidget(new Button(searchTextField.x + searchTextField.getWidth() + 3, searchTextField.y, 40, 20, Component.literal("Enable"), pButton -> groupableList.getSelected().enable(true)) {
            @Override
            public void render(@NotNull PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
                this.active = groupableList != null && groupableList.getSelected() != null && groupableList.getSelected().canEnable();
                super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
            }
        });
    }

    @Override
    public void render(@NotNull PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        if (showAsGroup.getValue()) {
            filterMode.visible = false;
            filterMode.active = false;
        } else {
            filterMode.visible = true;
            filterMode.active = true;
        }

        groupableList.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        searchTextField.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

        drawCenteredString(pPoseStack, font, getTitle(), this.width / 2, 7, 0xffffff);
    }

    protected class FocusedEditBox extends EditBox {
        public FocusedEditBox(Font font, int x, int y, int width, int height, Component label) {
            super(font, x, y, width, height, label);
        }

        @Override
        protected void onFocusedChanged(boolean focused) {
            super.onFocusedChanged(focused);
            if (focused) {
                if (GroupTestsManagerScreen.this.activeTextField != null && GroupTestsManagerScreen.this.activeTextField != this) {
                    GroupTestsManagerScreen.this.activeTextField.setFocused(false);
                }
                GroupTestsManagerScreen.this.activeTextField = this;
            }
        }
    }

    public static <T> void updateSearchTextFieldSuggestion(EditBox editBox, String value, List<T> entries, Function<T, String> nameProvider) {
        if (!value.isEmpty()) {
            Optional<? extends String> optional = entries.stream().filter(info -> nameProvider.apply(info).toLowerCase(Locale.ROOT).startsWith(value.toLowerCase(Locale.ROOT))).map(nameProvider).min(Comparator.naturalOrder());
            if (optional.isPresent()) {
                int length = value.length();
                String displayName = optional.get();
                editBox.setSuggestion(displayName.substring(length));
            } else {
                editBox.setSuggestion("");
            }
        } else {
            editBox.setSuggestion("Search");
        }
    }

    public enum FilterMode implements BiPredicate<TestFrameworkImpl, Test> {
        ALL("All") {
            @Override
            public boolean test(TestFrameworkImpl framework, Test test) {
                return true;
            }
        }, NOT_PROCESSED("Not Processed") {
            @Override
            public boolean test(TestFrameworkImpl framework, Test test) {
                return test.status().result() == Test.Result.NOT_PROCESSED;
            }
        }, PASSED("Passed") {
            @Override
            public boolean test(TestFrameworkImpl framework, Test test) {
                return test.status().result().passed();
            }
        }, FAILED("Failed") {
            @Override
            public boolean test(TestFrameworkImpl framework, Test test) {
                return test.status().result() == Test.Result.FAILED;
            }
        }, ENABLED("Enabled") {
            @Override
            public boolean test(TestFrameworkImpl framework, Test test) {
                return framework.tests().isEnabled(test.id());
            }
        }, DISABLED("Disabled") {
            @Override
            public boolean test(TestFrameworkImpl framework, Test test) {
                return !framework.tests().isEnabled(test.id());
            }
        };
        private Component name;

        FilterMode(String name) {
            this.name = Component.literal(name);
        }
    }
}

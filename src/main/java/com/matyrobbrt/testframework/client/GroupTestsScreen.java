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
import java.util.function.Consumer;
import java.util.function.Function;

public class GroupTestsScreen extends TestsScreen {
    protected FocusedEditBox activeTextField;
    protected FocusedEditBox searchTextField;

    private final List<Group> groups;
    protected GroupableList groupableList;
    private Consumer<String> suggestionProvider;
    protected CycleButton<Boolean> showGroups;

    public GroupTestsScreen(Component title, TestFrameworkImpl framework, List<Group> groups) {
        super(title, framework);
        this.groups = groups;
    }

    @Override
    protected void init() {
        this.showGroups = addRenderableWidget(CycleButton.booleanBuilder(Component.literal("Show groups"),
                Component.literal("Show all tests"))
                .displayOnlyValue().create(20, this.height - 26, 100, 20, Component.empty(), (pCycleButton, pValue) -> groupableList.resetRows(searchTextField.getValue())));

        final List<Test> tests = groups.stream().flatMap(it -> it.resolveAll().stream()).distinct().toList();
        this.groupableList = new GroupableList(() -> showGroups.getValue(), groups, tests::stream, minecraft, width, height, 50, height - 36, 9 + 2 + 2 + 2);
        this.suggestionProvider = s -> {
            if (showGroups.getValue()) {
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
        groupableList.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        searchTextField.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    protected class FocusedEditBox extends EditBox {
        public FocusedEditBox(Font font, int x, int y, int width, int height, Component label) {
            super(font, x, y, width, height, label);
        }

        @Override
        protected void onFocusedChanged(boolean focused) {
            super.onFocusedChanged(focused);
            if (focused) {
                if (GroupTestsScreen.this.activeTextField != null && GroupTestsScreen.this.activeTextField != this) {
                    GroupTestsScreen.this.activeTextField.setFocused(false);
                }
                GroupTestsScreen.this.activeTextField = this;
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
}

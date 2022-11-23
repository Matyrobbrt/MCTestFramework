package com.matyrobbrt.testframework.group;

import com.matyrobbrt.testframework.Test;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class Group implements Groupable {
    private final String id;
    private final List<Groupable> entries;
    private Component title;
    private boolean enabledByDefault;

    public Group(String id, List<Groupable> entries) {
        this.id = id;
        this.entries = entries;
        this.title = Component.literal(id);
    }

    public String id() {
        return this.id;
    }

    public List<Groupable> entries() {
        return entries;
    }

    public boolean isEnabledByDefault() {
        return this.enabledByDefault;
    }

    public void setEnabledByDefault(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public Component title() {
        return this.title;
    }

    public void setTitle(Component title) {
        this.title = title;
    }

    @Override
    public List<Test> resolveAll() {
        return entries.stream().flatMap(gr -> gr.resolveAll().stream()).toList();
    }

    public void add(Groupable entry) {
        this.entries.add(entry);
    }
}

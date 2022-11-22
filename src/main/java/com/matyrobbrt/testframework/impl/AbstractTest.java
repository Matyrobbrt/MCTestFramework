package com.matyrobbrt.testframework.impl;

import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.TestFramework;
import com.matyrobbrt.testframework.TestHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractTest implements Test {
    protected TestFramework framework;
    protected String id;
    protected final List<String> groups = new ArrayList<>();
    protected boolean enabledByDefault;
    protected Visuals visuals;
    private Status status = new Status(Result.NOT_PROCESSED, "");

    protected AbstractTest() {
        final TestHolder marker = getClass().getAnnotation(TestHolder.class);
        id = marker.value();
        enabledByDefault = marker.enabledByDefault();
        visuals = new Visuals(
                Component.literal(marker.title().isBlank() ? id : marker.title()),
                Stream.of(marker.description()).<Component>map(Component::literal).toList()
        );
        groups.addAll(List.of(marker.groups()));
    }

    @Override
    public void init(TestFramework framework) {
        this.framework = framework;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean enabledByDefault() {
        return enabledByDefault;
    }

    @Override
    public Status status() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public Visuals visuals() {
        return visuals;
    }

    @Override
    public List<String> groups() {
        return groups;
    }

    public boolean isEnabled() {
        return framework.tests().isEnabled(id());
    }

    public void enable() {
        framework.setEnabled(this, true, null);
    }

    public Logger getLogger() {
        return framework.logger();
    }

    public void updateStatus(Status status, @Nullable Entity changer) {
        framework.changeStatus(this, status, changer);
    }

    protected final void fail(String message) {
        updateStatus(new Status(Result.FAILED, message), null);
    }

    protected final void pass() {
        updateStatus(new Status(Result.PASSED, ""), null);
    }
}

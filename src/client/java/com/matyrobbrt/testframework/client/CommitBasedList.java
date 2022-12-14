package com.matyrobbrt.testframework.client;

import java.util.ArrayList;
import java.util.List;

public class CommitBasedList<T> {
    private final List<T> backend;
    private List<T> currentProgress = new ArrayList<>();

    public CommitBasedList(List<T> backend) {
        this.backend = backend;
    }

    public void push() {
        this.currentProgress = new ArrayList<>();
    }

    public void revert() {
        this.currentProgress = null;
    }

    public void popAndCommit() {
        this.backend.addAll(currentProgress);
        this.currentProgress = null;
    }

    public List<T> currentProgress() {
        return currentProgress;
    }

    public void addDirectly(T value) {
        this.backend.add(value);
    }

    public void add(T value) {
        this.currentProgress.add(value);
    }

    public List<T> get() {
        return backend;
    }
}

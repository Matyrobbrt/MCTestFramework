package com.matyrobbrt.testframework.group;

import com.matyrobbrt.testframework.Test;

import java.util.List;

public interface Groupable {
    /**
     * Resolves all tests in this groupable element.
     * @return all tests
     */
    List<Test> resolveAll();
}

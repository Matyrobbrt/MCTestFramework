package com.matyrobbrt.testframework.group;

import com.matyrobbrt.testframework.Test;

import java.util.List;

public interface Groupable {
    List<Test> resolveAll();
}

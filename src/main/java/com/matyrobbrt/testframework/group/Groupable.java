package com.matyrobbrt.testframework.group;

import com.matyrobbrt.testframework.Test;
import net.minecraft.MethodsReturnNonnullByDefault;

import java.util.List;

@MethodsReturnNonnullByDefault
public interface Groupable {
    /**
     * Resolves all tests in this groupable element.
     * @return all tests
     */
    List<Test> resolveAll();
}

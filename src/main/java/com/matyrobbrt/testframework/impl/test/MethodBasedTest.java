package com.matyrobbrt.testframework.impl.test;

import com.matyrobbrt.testframework.TestFramework;
import com.matyrobbrt.testframework.annotation.ForEachTest;
import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.impl.HackyReflection;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

public class MethodBasedTest extends AbstractTest.Dynamic {
    protected MethodHandle handle;
    private final Method method;

    public MethodBasedTest(Method method) {
        this.method = method;
        final TestHolder marker = method.getAnnotation(TestHolder.class);
        if (marker != null) {
            configureFrom(method.getDeclaringClass().getAnnotation(ForEachTest.class), marker);
        }

        this.handle = HackyReflection.handle(method);
    }

    public MethodBasedTest bindTo(Object target) {
        handle = handle.bindTo(target);
        return this;
    }

    @Override
    public void init(TestFramework framework) {
        super.init(framework);
        try {
            this.handle.invoke(this);
        } catch (Throwable e) {
            throw new RuntimeException("Encountered exception initiating method-based test: " + method, e);
        }
    }

}

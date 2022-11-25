package com.matyrobbrt.testframework.annotation;

import com.matyrobbrt.testframework.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by {@link com.matyrobbrt.testframework.impl.AbstractTest} in order to collect information about a test.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestHolder {
    /**
     * {@return the test's ID}
     */
    String value();

    /**
     * Returns the groups the test is in. <br>
     * Note: if empty, it defaults to {@code ungrouped}.
     *
     * @return the groups the test is in
     * @see Test#groups()
     */
    String[] groups() default {};

    /**
     * {@return the human-readable title of the test}
     */
    String title() default "";

    /**
     * This usually contains instructions on how to use the test.
     * {@return the human-readable description of the test}
     */
    String[] description() default {};

    /**
     * {@return if this test is enabled by default}
     */
    boolean enabledByDefault() default false;
}

package com.matyrobbrt.testframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a {@link String} field with the value being the ID of a group with this annotation in order to configure the group.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestGroup {
    /**
     * {@return the human-readable name of the group}
     */
    String name();

    /**
     * {@return if the tests in this group are enabled by default}
     */
    boolean enabledByDefault() default false;

    /**
     * Note: group parents are also computed using {@code id().split(".")}.<br>
     * {@return the parents of this group}
     */
    String[] parents() default {};
}

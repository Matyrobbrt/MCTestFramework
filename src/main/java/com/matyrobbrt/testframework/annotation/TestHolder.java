package com.matyrobbrt.testframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestHolder {
    String value();
    String[] groups() default {};
    String title() default "";
    String[] description() default {};
    boolean enabledByDefault() default false;
}

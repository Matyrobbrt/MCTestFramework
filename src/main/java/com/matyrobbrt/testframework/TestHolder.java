package com.matyrobbrt.testframework;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TestHolder {
    String value();
    String[] groups() default {};
    String title() default "";
    String[] description() default {};
    boolean enabledByDefault() default false;
}

package com.matyrobbrt.testframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.matyrobbrt.testframework.TestFramework;
import com.matyrobbrt.testframework.impl.TestFrameworkInternal;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;

/**
 * Annotate a method accepting exactly one parameter of {@linkplain TestFrameworkInternal} (or parent interfaces) to
 * register that method as an on-init listener, which will be called in {@link TestFrameworkInternal#init(IEventBus, ModContainer)}.
 * The time when it will be called depends on the {@linkplain #value() stage} given as an annotation parameter.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnInit {
    /**
     * {@return the stage during which to run this listener}
     */
    Stage value() default Stage.BEFORE_SETUP;

    enum Stage {
        /**
         * This stage happens before tests are collected, but after the {@linkplain TestFramework#modEventBus() mod event bus} is configured.
         */
        BEFORE_SETUP,

        /**
         * This stage happens after tests are collected and {@linkplain RegisterStructureTemplate structure templates} are registered.
         */
        AFTER_SETUP
    }
}

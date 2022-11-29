package com.matyrobbrt.testframework.impl.test;

import com.matyrobbrt.testframework.annotation.ForEachTest;
import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.impl.HackyReflection;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.IModBusEvent;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

public class MethodBasedEventTest extends AbstractTest.Dynamic {
    protected MethodHandle handle;
    private final Method method;

    private final Class<? extends Event> eventClass;
    private final Mod.EventBusSubscriber.Bus bus;
    private final EventPriority priority;
    private final boolean receiveCancelled;

    public MethodBasedEventTest(Method method) {
        this.method = method;
        final TestHolder marker = method.getAnnotation(TestHolder.class);
        if (marker != null) {
            configureFrom(method.getDeclaringClass().getAnnotation(ForEachTest.class), marker);
        }

        //noinspection unchecked
        this.eventClass = (Class<? extends Event>) method.getParameterTypes()[0];
        this.bus = IModBusEvent.class.isAssignableFrom(eventClass) ? Mod.EventBusSubscriber.Bus.MOD : Mod.EventBusSubscriber.Bus.FORGE;

        final SubscribeEvent seAnnotation = method.getAnnotation(SubscribeEvent.class);
        if (seAnnotation == null) {
            priority = EventPriority.NORMAL;
            receiveCancelled = false;
        } else {
            priority = seAnnotation.priority();
            receiveCancelled = seAnnotation.receiveCanceled();
        }

        this.handle = HackyReflection.handle(method);
    }

    public MethodBasedEventTest bindTo(Object target) {
        handle = handle.bindTo(target);
        return this;
    }

    @Override
    public void onEnabled(@Nonnull EventListenerGroup buses) {
        super.onEnabled(buses);
        buses.getFor(bus).addListener(priority, receiveCancelled, eventClass, event -> {
            try {
                handle.invoke(event, this);
            } catch (Throwable throwable) {
                framework.logger().warn("Encountered exception firing event listeners for method-based event test {}: ", method, throwable);
            }
        });
    }
}

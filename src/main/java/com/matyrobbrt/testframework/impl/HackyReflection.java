package com.matyrobbrt.testframework.impl;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public final class HackyReflection {
    private static final Unsafe UNSAFE;
    private static final MethodHandles.Lookup LOOKUP;
    static {
        try {
            final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);

            LOOKUP = getStaticField(MethodHandles.Lookup.class, "IMPL_LOOKUP");
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException("BARF!", e);
        }
    }

    public static <T> T getStaticField(Class<?> clazz, String name) {
        try {
            final Field field = clazz.getDeclaredField(name);
            return (T)UNSAFE.getObject(UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("BARF!", e);
        }
    }

    public static <T> T getInstanceField(Object instance, String name) {
        try {
            final Field field = instance.getClass().getDeclaredField(name);
            return (T)UNSAFE.getObject(instance, UNSAFE.objectFieldOffset(field));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("BARF!", e);
        }
    }

    public static void setInstanceField(Object instance, String name, Object value) {
        try {
            final Field field = instance.getClass().getDeclaredField(name);
            UNSAFE.putObject(instance, UNSAFE.objectFieldOffset(field), value);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("BARF!", e);
        }
    }

    public static Class<?> getClass(String name) {
        try {
            return LOOKUP.findClass(name);
        } catch (ClassNotFoundException | IllegalAccessException e) {
            throw new RuntimeException("BARF!", e);
        }
    }

    public static MethodHandle staticHandle(Method method) {
        try {
            return LOOKUP.findStatic(method.getDeclaringClass(), method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("BARF!", e);
        }
    }

    public static MethodHandle virtualHandle(Method method) {
        try {
            return LOOKUP.findVirtual(method.getDeclaringClass(), method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("BARF!", e);
        }
    }

    public static MethodHandle handle(Method method) {
        return Modifier.isStatic(method.getModifiers()) ? staticHandle(method) : virtualHandle(method);
    }

    public static MethodHandle constructor(Class<?> owner, MethodType type) {
        try {
            return LOOKUP.findConstructor(owner, type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("BARF!", e);
        }
    }

    public static Method methodMatching(Class<?> clazz, Predicate<Method> methodPredicate) {
        return Stream.of(clazz.getDeclaredMethods())
                .filter(methodPredicate).findFirst().orElseThrow();
    }

    public static Class<?> parentOrTopLevel(Class<?> clazz) {
        if (clazz.getEnclosingClass() != null) return clazz.getEnclosingClass();
        return clazz;
    }
}

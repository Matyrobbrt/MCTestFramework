package com.matyrobbrt.testframework.conf;

import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.impl.HackyReflection;
import com.matyrobbrt.testframework.impl.test.MethodBasedEventTest;
import com.matyrobbrt.testframework.impl.test.MethodBasedTest;
import com.mojang.logging.LogUtils;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FunctionalInterface
public interface TestCollector {
    List<Test> collect(ModContainer container);

    default TestCollector and(TestCollector other) {
        return container -> Stream.concat(
                this.collect(container).stream(),
                other.collect(container).stream()
        ).toList();
    }

    Predicate<ModFileScanData.AnnotationData> SIDE_FILTER = data -> {
        final Dist current = FMLLoader.getDist();
        Object sidesValue = data.annotationData().get("side");
        if (sidesValue == null) sidesValue = data.annotationData().get("dist");
        if (sidesValue == null) return true;
        @SuppressWarnings("unchecked") final EnumSet<Dist> sides = ((List<ModAnnotation.EnumHolder>) sidesValue).stream().map(eh -> Dist.valueOf(eh.getValue())).
                collect(Collectors.toCollection(() -> EnumSet.noneOf(Dist.class)));
        return sides.contains(current);
    };

    static TestCollector forClassesWithAnnotation(Class<? extends Annotation> annotation) {
        final Type annType = Type.getType(annotation);
        return container -> container.getModInfo().getOwningFile().getFile().getScanResult()
                .getAnnotations().stream().filter(it -> annType.equals(it.annotationType()) && it.targetType() == ElementType.TYPE && SIDE_FILTER.test(it))
                .map(LamdbaExceptionUtils.rethrowFunction(annotationData -> {
                    final Class<?> clazz = Class.forName(annotationData.clazz().getClassName());
                    return (Test) clazz.getDeclaredConstructor().newInstance();
                })).toList();
    }

    static TestCollector forMethodsWithAnnotation(Class<? extends Annotation> annotation) {
        return container -> findMethodsWithAnnotation(container, SIDE_FILTER, annotation)
                .filter(method -> method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom(MethodBasedTest.class))
                .filter(method -> {
                    if (Modifier.isStatic(method.getModifiers())) {
                        return true;
                    }
                    LogUtils.getLogger().warn("Attempted to register method-based test on non-static method: " + method);
                    return false;
                })
                .<Test>map(MethodBasedTest::new).toList();
    }

    static TestCollector eventTestMethodsWithAnnotation(Class<? extends Annotation> annotation) {
        return container -> findMethodsWithAnnotation(container, SIDE_FILTER, annotation)
                .filter(method -> method.getParameterTypes().length == 2 && Event.class.isAssignableFrom(method.getParameterTypes()[0]) && method.getParameterTypes()[1].isAssignableFrom(MethodBasedEventTest.class))
                .filter(method -> {
                    if (Modifier.isStatic(method.getModifiers())) {
                        return true;
                    }
                    LogUtils.getLogger().warn("Attempted to register method-based event test on non-static method: " + method);
                    return false;
                })
                .<Test>map(MethodBasedEventTest::new).toList();
    }

    static Stream<Method> findMethodsWithAnnotation(ModContainer container, Predicate<ModFileScanData.AnnotationData> annotationPredicate, Class<? extends Annotation> annotation) {
        final Type annType = Type.getType(annotation);
        return container.getModInfo().getOwningFile().getFile().getScanResult()
                .getAnnotations().stream().filter(it -> annType.equals(it.annotationType()) && it.targetType() == ElementType.METHOD && annotationPredicate.test(it))
                .map(LamdbaExceptionUtils.rethrowFunction(annotationData -> {
                    final Class<?> clazz = Class.forName(annotationData.clazz().getClassName());
                    final String methodName = annotationData.memberName().substring(0, annotationData.memberName().indexOf("("));
                    return HackyReflection.methodMatching(clazz, it -> it.getName().equals(methodName) && it.getAnnotation(annotation) != null);
                }));
    }

}

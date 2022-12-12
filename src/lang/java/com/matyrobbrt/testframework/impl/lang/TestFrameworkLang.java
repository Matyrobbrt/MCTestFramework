package com.matyrobbrt.testframework.impl.lang;

import net.minecraftforge.forgespi.language.ILifecycleEvent;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.IModLanguageProvider;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TestFrameworkLang implements IModLanguageProvider {
    private static final Type TEST_HOLDER = Type.getType("Lcom/matyrobbrt/testframework/annotation/TestHolder;");
    private static final Type TEST_MAIN = Type.getType("Lcom/matyrobbrt/testframework/annotation/TestMain;");
    private static final Type FOR_EACH_TEST = Type.getType("Lcom/matyrobbrt/testframework/annotation/ForEachTest;");

    @Override
    public String name() {
        return "testframework";
    }

    @Override
    public Consumer<ModFileScanData> getFileVisitor() {
        return data -> {
            final Map<String, String> idPrefixes = idPrefixes(data);
            data.addLanguageLoader(data.getAnnotations().stream()
                    .filter(it -> it.annotationType().equals(TEST_HOLDER))
                    .collect(Collectors.toMap(it -> getModId(idPrefixes, it), it -> new Loader(getModId(idPrefixes, it), null, false))));
            data.addLanguageLoader(data.getAnnotations().stream()
                    .filter(it -> it.annotationType().equals(TEST_MAIN))
                    .collect(Collectors.toMap(it -> it.annotationData().get("value").toString(), it -> new Loader(getModId(idPrefixes, it), it.clazz().getClassName(), true))));
        };
    }

    private Map<String, String> idPrefixes(ModFileScanData scanData) {
        return scanData.getAnnotations().stream()
                .filter(it -> it.annotationType().equals(FOR_EACH_TEST) && it.targetType() == ElementType.TYPE)
                .collect(Collectors.toMap(it -> it.clazz().getInternalName(), it -> (String)it.annotationData().getOrDefault("idPrefix", "")));
    }

    private String getModId(Map<String, String> idPrefixes, ModFileScanData.AnnotationData data) {
        return idPrefixes.getOrDefault(data.clazz().getInternalName(), "") + data.annotationData().get("value");
    }

    @Override
    public <R extends ILifecycleEvent<R>> void consumeLifecycleEvent(Supplier<R> consumeEvent) {

    }

    public record Loader(String modId, String declaringClass, boolean isMain) implements IModLanguageProvider.IModLanguageLoader {

        @Override
        public <T> T loadMod(IModInfo info, ModFileScanData modFileScanResults, ModuleLayer layer) {
            return (T) (isMain ? new TFModContainer(info, declaringClass, layer) : new TFModContainer(info));
        }
    }

}

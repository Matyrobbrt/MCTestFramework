package com.matyrobbrt.testframework.impl.locator;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModProvider;
import net.minecraftforge.fml.loading.moddiscovery.ModClassVisitor;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.targets.CommonDevLaunchHandler;
import net.minecraftforge.fml.loading.targets.CommonLaunchHandler;
import net.minecraftforge.fml.loading.targets.CommonUserdevLaunchHandler;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TLModLocator extends AbstractJarFileModProvider implements IModLocator {
    private static final Type TEST_HOLDER = Type.getType("Lcom/matyrobbrt/testframework/annotation/TestHolder;");
    private static final Type TEST_MAIN = Type.getType("Lcom/matyrobbrt/testframework/annotation/TestMain;");
    private static final Type FOR_EACH_TEST = Type.getType("Lcom/matyrobbrt/testframework/annotation/ForEachTest;");


    @Override
    public List<ModFileOrException> scanMods() {
        final TestSources testSources = getTestSources();
        if (testSources == TestSources.EMPTY) return List.of();
        final ModFileOrException modFile = createMod(testSources.paths().toArray(Path[]::new));
        return List.of(modFile);
    }

    protected IModLocator.ModFileOrException createMod(Path... path) {
        return new ModFileOrException(ModJarMetadata.buildFile(
                jar -> new ModFile(jar, this, this::buildTestModsTOML),
                jar -> true,
                (p, base) -> true,
                path
        ).orElseThrow(), null);
    }

    private IModFileInfo buildTestModsTOML(IModFile modFileIn) {
        // We make the same assumption as MinecraftLocator that the IModFile provided is an instanceof ModFile
        final ModFile modFile = (ModFile) modFileIn;

        // At this point, there is no scan data in the ModFile yet, so let's scan it ourselves
        // This is normally done in a background thread, but as this is just the test source set, we just call directly
        final ModFileScanData scanData = modFile.compileContent();
        final Map<String, String> idPrefixes = idPrefixes(scanData);
        final List<ModFileScanData.AnnotationData> testModAnnotations = scanData.getAnnotations().stream()
                .filter(annotation -> annotation.annotationType().equals(TEST_HOLDER) || annotation.annotationType().equals(TEST_MAIN))
                .toList();

        final CommentedConfig modFileTOML = TomlFormat.newConfig();
        modFileTOML.set("modLoader", "testframework"); // Mods in the test source set all use FML Java lang provider
        modFileTOML.set("loaderVersion", "[1,)"); // Accept any loader version
        modFileTOML.set("license", "LGPLv2.1"); // Forge license

        final List<CommentedConfig> mods = new ArrayList<>(testModAnnotations.size());
        for (ModFileScanData.AnnotationData modAnnotation : testModAnnotations) {
            final String modId = getModId(idPrefixes, modAnnotation);
            final CommentedConfig modInfo = TomlFormat.newConfig();
            mods.add(modInfo);
            modInfo.set("modId", modId); // We only need to set the modId
        }
        modFileTOML.set("mods", mods);

        final NightConfigWrapper configWrapper = new NightConfigWrapper(modFileTOML);
        final ModFileInfo modFileInfo = new ModFileInfo(modFile, configWrapper, List.of()); // No extra language specs
        configWrapper.setFile(modFileInfo);
        return modFileInfo;
    }

    private String getModId(Map<String, String> idPrefixes, ModFileScanData.AnnotationData data) {
        return idPrefixes.getOrDefault(data.clazz().getInternalName(), "") + data.annotationData().get("value");
    }

    private Map<String, String> idPrefixes(ModFileScanData scanData) {
        return scanData.getAnnotations().stream()
                .filter(it -> it.annotationType().equals(FOR_EACH_TEST) && it.targetType() == ElementType.TYPE)
                .collect(Collectors.toMap(it -> it.clazz().getInternalName(), it -> (String)it.annotationData().getOrDefault("idPrefix", "")));
    }

    @Override
    public String name() {
        return "testframeworklocator";
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {

    }

    @Override
    public boolean isValid(IModFile modFile) {
        return true;
    }

    // See CommonLaunchHandler#getModClasses
    static TestSources getTestSources() {

        final String modClasses = System.getenv("MOD_CLASSES");
        final String testId = System.getProperty("forge.test.id");
        if (modClasses == null || testId == null) return TestSources.EMPTY;

        // modid%%path;modid%%path
        // where ';' is File.pathSeparator

        record ExplodedModDirectory(String modId, Path path) {
            static ExplodedModDirectory create(String modClassesPart) {
                final String[] split = modClassesPart.split("%%", 2);
                final String modId = split.length == 1 ? "defaultmodid" : split[0];
                final Path path = Path.of(split[split.length - 1]);
                return new ExplodedModDirectory(modId, path);
            }
        }

        final Map<String, List<Path>> modClassPaths = Arrays.stream(modClasses.split(File.pathSeparator))
                .map(ExplodedModDirectory::create)
                .collect(Collectors.groupingBy(ExplodedModDirectory::modId, Collectors.mapping(ExplodedModDirectory::path, Collectors.toList())));

        return new TestSources(testId, modClassPaths.getOrDefault(testId, List.of()));
    }

    /**
     * @param id    the named ID for the test sources
     * @param paths the paths to the test sources
     */
    record TestSources(String id, List<Path> paths) {
        public static final TestSources EMPTY = new TestSources("blank", List.of());
    }

    record DisabledData(Set<String> disabledTests, Set<String> disabledGroups) {

    }
}

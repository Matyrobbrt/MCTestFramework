package com.matyrobbrt.testframework.gametest;

import com.matyrobbrt.testframework.impl.HackyReflection;
import com.mojang.logging.LogUtils;
import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DynamicStructureTemplates {
    // StructureTemplateManager#sources
    private static final String SOURCES_FIELD = ObfuscationReflectionHelper.remapName(INameMappingService.Domain.FIELD, "f_" + "230349_");

    private final Map<ResourceLocation, Supplier<StructureTemplate>> templates = new ConcurrentHashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setup(StructureTemplateManager manager) throws Throwable {
        final List sources = new ArrayList(HackyReflection.<List>getInstanceField(manager, SOURCES_FIELD));

        final Class sourceClazz = Stream.of(StructureTemplateManager.class.getDeclaredClasses())
                .filter(it -> it.getSimpleName().equals("Source")).findFirst().orElseThrow();
        final MethodHandle ctor = HackyReflection.constructor(sourceClazz, MethodType.methodType(void.class, Function.class, Supplier.class));

        final Function<ResourceLocation, Optional<StructureTemplate>> loader = this::load;
        final Supplier<Stream<ResourceLocation>> lister = this::list;
        sources.add(ctor.invokeWithArguments(loader, lister));

        HackyReflection.setInstanceField(manager, SOURCES_FIELD, sources);

        LogUtils.getLogger().debug("Injected dynamic template source in manager {}", manager);
    }

    private Optional<StructureTemplate> load(ResourceLocation location) {
        final Supplier<StructureTemplate> sup = templates.get(location);
        if (sup == null) return Optional.empty();
        return Optional.of(sup.get());
    }

    private Stream<ResourceLocation> list() {
        return templates.keySet().stream();
    }

    public void register(ResourceLocation id, Supplier<StructureTemplate> template) {
        templates.put(id, template);
    }

    public void register(ResourceLocation id, StructureTemplate template) {
        register(id, () -> template);
    }
}

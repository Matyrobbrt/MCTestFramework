package com.matyrobbrt.testframework.collector;

import com.google.gson.reflect.TypeToken;
import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.annotation.OnInit;
import com.matyrobbrt.testframework.impl.TestFrameworkInternal;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record CollectorType<A>(Type type) {
    public static final CollectorType<Test> TESTS = get(new TypeToken<>() {});
    public static final CollectorType<Pair<ResourceLocation, Supplier<StructureTemplate>>> STRUCTURE_TEMPLATES = get(new TypeToken<>() {});
    public static final CollectorType<Pair<OnInit.Stage, Consumer<? super TestFrameworkInternal>>> ON_INIT = get(new TypeToken<>() {});
    public static final CollectorType<GroupData> GROUP_DATA = get(new TypeToken<>() {});

    public record GroupData(String id, @Nullable Component title, boolean isEnabledByDefault, String[] parents) {}

    private static <Z> CollectorType<Z> get(TypeToken<Z> token) {
        return new CollectorType<>(token.getType());
    }
}

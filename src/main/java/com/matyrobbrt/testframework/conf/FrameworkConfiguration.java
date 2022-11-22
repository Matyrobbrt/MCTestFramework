package com.matyrobbrt.testframework.conf;

import com.matyrobbrt.testframework.Test;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record FrameworkConfiguration(
        ResourceLocation id, boolean clientSynced, boolean modifiableByClients, int commandRequiredPermission,
        SimpleChannel networkingChannel, List<String> enabledTests, @Nullable Supplier<ClientConfiguration> clientConfiguration,
        TestCollector testCollector
) {
    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final ResourceLocation id;

        private boolean clientSynced = false, modifiableByClients = false;
        private int commandRequiredPermission = Commands.LEVEL_GAMEMASTERS;
        private @Nullable SimpleChannel networkingChannel;
        private final List<String> enabledTests = new ArrayList<>();
        private TestCollector testCollector = cnt -> List.of();

        private @Nullable Supplier<ClientConfiguration> clientConfiguration;

        public Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder syncToClients() {
            this.clientSynced = true;
            return this;
        }
        public Builder allowClientModifications() {
            this.modifiableByClients = true;
            return this;
        }

        public Builder commandRequiredPermission(int commandRequiredPermission) {
            this.commandRequiredPermission = commandRequiredPermission;
            return this;
        }

        public Builder enableTests(String... tests) {
            this.enabledTests.addAll(List.of(tests));
            return this;
        }

        public Builder networkingChannel(SimpleChannel channel) {
            this.networkingChannel = channel;
            return this;
        }

        public Builder clientConfiguration(Supplier<ClientConfiguration> clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
            return this;
        }

        public Builder testCollector(TestCollector testCollector) {
            this.testCollector = testCollector;
            return this;
        }

        public FrameworkConfiguration build() {
            final SimpleChannel channel = networkingChannel == null ?
                    NetworkRegistry.ChannelBuilder.named(id)
                            .clientAcceptedVersions(e -> true)
                            .serverAcceptedVersions(e -> true)
                            .networkProtocolVersion(() -> "yes")
                            .simpleChannel() : networkingChannel;
            return new FrameworkConfiguration(
                    id, clientSynced, modifiableByClients, commandRequiredPermission,
                    channel, enabledTests, clientConfiguration, testCollector
            );
        }
    }

    @FunctionalInterface
    public interface TestCollector {
        List<Test> collect(ModContainer container);

        static TestCollector withAnnotation(Class<? extends Annotation> annotation) {
            final Type annType = Type.getType(annotation);
            return container -> container.getModInfo().getOwningFile().getFile().getScanResult()
                    .getAnnotations().stream().filter(it -> annType.equals(it.annotationType()))
                    .map(LamdbaExceptionUtils.rethrowFunction(annotationData -> {
                        final Class<?> clazz = Class.forName(annotationData.clazz().getClassName());
                        return (Test) clazz.getDeclaredConstructor().newInstance();
                    })).toList();
        }
    }
}

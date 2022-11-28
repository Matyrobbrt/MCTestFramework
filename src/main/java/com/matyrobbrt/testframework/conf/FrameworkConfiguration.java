package com.matyrobbrt.testframework.conf;

import com.matyrobbrt.testframework.annotation.TestHolder;
import com.matyrobbrt.testframework.impl.TestFrameworkImpl;
import com.matyrobbrt.testframework.impl.TestFrameworkInternal;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public record FrameworkConfiguration(
        ResourceLocation id, boolean clientSynced, boolean modifiableByClients, int commandRequiredPermission,
        SimpleChannel networkingChannel, List<String> enabledTests, @Nullable Supplier<ClientConfiguration> clientConfiguration,
        TestCollector testCollector, @Nullable FrameworkConfiguration.GroupConfigurationCollector<?> groupConfigurationCollector
) {
    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public TestFrameworkInternal create() {
        return new TestFrameworkImpl(this);
    }

    public static final class Builder {
        private final ResourceLocation id;

        private boolean clientSynced = false, modifiableByClients = false;
        private int commandRequiredPermission = Commands.LEVEL_GAMEMASTERS;
        private @Nullable SimpleChannel networkingChannel;
        private final List<String> enabledTests = new ArrayList<>();
        private TestCollector testCollector = TestCollector.forClassesWithAnnotation(TestHolder.class);
        private @Nullable FrameworkConfiguration.GroupConfigurationCollector<?> groupConfigurationCollector;

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

        public <T extends Annotation> Builder groupConfigurationCollector(@Nullable FrameworkConfiguration.GroupConfigurationCollector<T> collector) {
            this.groupConfigurationCollector = collector;
            return this;
        }

        @ParametersAreNonnullByDefault
        public <T extends Annotation> Builder groupConfigurationCollector(Class<T> annotation, Function<T, Component> nameGetter, Function<T, Boolean> isEnabledByDefault, Function<T, String[]> parents) {
            this.groupConfigurationCollector = new GroupConfigurationCollector<>(annotation, Type.getType(annotation), nameGetter, isEnabledByDefault, parents);
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
                    channel, enabledTests, clientConfiguration, testCollector, groupConfigurationCollector
            );
        }
    }

    @SuppressWarnings("unchecked")
    public record GroupConfigurationCollector<T extends Annotation>(Class<T> annotation, Type asmType, Function<T, Component> nameGetter, Function<T, Boolean> isEnabledByDefault, Function<T, String[]> parents) {
        public Component getName(Object o) {
            return nameGetter.apply((T) o);
        }

        public boolean isEnabledByDefault(Object o) {
            return isEnabledByDefault.apply((T) o);
        }

        public String[] getParents(Object o) {
            return parents.apply((T) o);
        }
    }
}

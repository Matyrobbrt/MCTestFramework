package com.matyrobbrt.testframework.conf;

import com.matyrobbrt.testframework.collector.Collector;
import com.matyrobbrt.testframework.collector.CollectorType;
import com.matyrobbrt.testframework.impl.TestFrameworkImpl;
import com.matyrobbrt.testframework.impl.TestFrameworkInternal;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record FrameworkConfiguration(
        ResourceLocation id, Collection<Feature> enabledFeatures, int commandRequiredPermission,
        SimpleChannel networkingChannel, List<String> enabledTests, @Nullable Supplier<ClientConfiguration> clientConfiguration,
        Map<CollectorType<?>, Collector<?>> collectors
) {
    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    @SuppressWarnings("unchecked")
    public <Z> Collector<Z> collector(CollectorType<Z> type) {
        return (Collector<Z>) collectors.getOrDefault(type, (c, a) -> {});
    }

    public boolean isEnabled(Feature feature) {
        return enabledFeatures.contains(feature);
    }

    public TestFrameworkInternal create() {
        return new TestFrameworkImpl(this);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Collection<Feature> features = EnumSet.noneOf(Feature.class);
        private final Map<CollectorType<?>, Collector<?>> collectors = new HashMap<>();

        private int commandRequiredPermission = Commands.LEVEL_GAMEMASTERS;
        private @Nullable SimpleChannel networkingChannel;
        private final List<String> enabledTests = new ArrayList<>();

        private @Nullable Supplier<ClientConfiguration> clientConfiguration;

        public Builder(ResourceLocation id) {
            this.id = id;

            for (final Feature value : Feature.values()) {
                if (value.isEnabledByDefault()) enable(value);
            }
        }

        public Builder enable(Feature... features) {
            this.features.addAll(List.of(features));
            return this;
        }

        public Builder disable(Feature... features) {
            this.features.removeAll(List.of(features));
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

        @SuppressWarnings("unchecked")
        public <Z> Builder withCollector(CollectorType<Z> type, Collector<Z> collector) {
            final Collector<Z> initial = (Collector<Z>) collectors.get(type);
            if (initial != null) {
                collector = initial.and(collector);
            }
            collectors.put(type, collector);
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
                    id, features, commandRequiredPermission,
                    channel, enabledTests, clientConfiguration,
                    Collections.unmodifiableMap(collectors)
            );
        }
    }
}

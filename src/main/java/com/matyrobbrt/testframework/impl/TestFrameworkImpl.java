package com.matyrobbrt.testframework.impl;

import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.TestFramework;
import com.matyrobbrt.testframework.impl.packet.C2SChangeEnabledPacket;
import com.matyrobbrt.testframework.impl.packet.C2SChangeStatusPacket;
import com.matyrobbrt.testframework.impl.packet.ChangeEnabledPacket;
import com.matyrobbrt.testframework.impl.packet.ChangeStatusPacket;
import com.matyrobbrt.testframework.impl.packet.TFPacket;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static net.minecraft.commands.Commands.*;

// TODO - logging
public abstract class TestFrameworkImpl implements TestFramework {
    private final ResourceLocation id;
    private final TestsImpl tests = new TestsImpl();

    private final SimpleChannel channel;

    private @Nullable MinecraftServer server;

    public TestFrameworkImpl(ResourceLocation id) {
        this.id = id;
        channel = NetworkRegistry.ChannelBuilder.named(id)
                .clientAcceptedVersions(e -> true)
                .serverAcceptedVersions(e -> true)
                .networkProtocolVersion(() -> "yes")
                .simpleChannel();

        MinecraftForge.EVENT_BUS.addListener((final ServerStartedEvent event) -> {
            server = event.getServer();
            tests.initialiseTests();
        });
        MinecraftForge.EVENT_BUS.addListener((final ServerStoppedEvent event) -> server = event.getServer() == server ? null : server);
    }

    public void registerCommands(LiteralArgumentBuilder<CommandSourceStack> node) {
        final BiFunction<LiteralArgumentBuilder<CommandSourceStack>, Boolean, LiteralArgumentBuilder<CommandSourceStack>> commandEnabling = (stack, enabling) ->
                stack.requires(it -> it.hasPermission(LEVEL_GAMEMASTERS))
                    .then(argument("id", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
                                tests.tests.keySet().stream()
                                        .filter(it -> it.toLowerCase(Locale.ROOT).startsWith(remaining))
                                        .filter(it -> tests.isEnabled(it) == !enabling)
                                        .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                final String id = StringArgumentType.getString(ctx, "id");
                                if (tests.byId(id).isEmpty()) {
                                    ctx.getSource().sendFailure(Component.literal("Unknown test with id '%s'!".formatted(id)));
                                    return Command.SINGLE_SUCCESS;
                                }
                                if (tests().isEnabled(id) == enabling) {
                                    ctx.getSource().sendFailure(Component.literal("Test is already " + (enabling ? "enabled" : "disabled") + "!"));
                                    return Command.SINGLE_SUCCESS;
                                }
                                setEnabled(tests.tests.get(id), enabling, ctx.getSource().getEntity());
                                ctx.getSource().sendSuccess(Component.literal((enabling ? "Enabled" : "Disabled") + " test!"), true);
                                return Command.SINGLE_SUCCESS;
                            }));
        node.then(commandEnabling.apply(literal("enable"), true));
        node.then(commandEnabling.apply(literal("disable"), false));
    }

    private IEventBus modBus;
    public void init(final IEventBus modBus, final ModContainer container) {
        this.modBus = modBus;
        collectTests(container).forEach(tests()::register);

        modBus.addListener((final FMLCommonSetupEvent event) -> setupPackets());

        if (FMLLoader.getDist().isClient()) {
            setupClientListeners(this);
        }
    }

    private static void setupClientListeners(TestFrameworkImpl impl) {
        MinecraftForge.EVENT_BUS.addListener((final ClientPlayerNetworkEvent.LoggingIn logOut) -> {
            synchronized (impl.tests().enabled) {
                impl.tests().enabled.forEach(impl.tests()::disable);
            }
            impl.tests().initialiseTests();
        });
    }

    private void setupPackets() {
        class Registrar {
            private final SimpleChannel channel;
            int id = 0;

            Registrar(SimpleChannel channel) {
                this.channel = channel;
            }

            <P extends TFPacket> void register(Class<P> pkt, BiFunction<TestFrameworkImpl, FriendlyByteBuf, P> decoder) {
                channel.messageBuilder(pkt, id++)
                        .consumerMainThread((packet, contextSupplier) -> {
                            final var ctx = contextSupplier.get();
                            packet.handle(ctx);
                        })
                        .encoder(TFPacket::encode)
                        .decoder(buf -> decoder.apply(TestFrameworkImpl.this, buf))
                        .add();
            }

            <P extends TFPacket> void registerNetworkThread(Class<P> pkt, BiFunction<TestFrameworkImpl, FriendlyByteBuf, P> decoder) {
                channel.messageBuilder(pkt, id++)
                        .consumerNetworkThread((packet, contextSupplier) -> {
                            final var ctx = contextSupplier.get();
                            packet.handle(ctx);
                            ctx.setPacketHandled(true);
                        })
                        .encoder(TFPacket::encode)
                        .decoder(buf -> decoder.apply(TestFrameworkImpl.this, buf))
                        .add();
            }
        }

        final Registrar registrar = new Registrar(channel);
        registrar.register(C2SChangeStatusPacket.class, C2SChangeStatusPacket::decode);
        registrar.register(ChangeStatusPacket.class, ChangeStatusPacket::decode);

        registrar.register(C2SChangeEnabledPacket.class, C2SChangeEnabledPacket::decode);
        registrar.register(ChangeEnabledPacket.class, ChangeEnabledPacket::decode);
    }

    public abstract List<Test> collectTests(final ModContainer container);

    @Override
    public IEventBus modEventBus() {
        return modBus;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public TestsImpl tests() {
        return tests;
    }

    @Override
    public void changeStatus(Test test, Test.Status newStatus, @Nullable Entity changer) {
        test.setStatus(newStatus);

        final ChangeStatusPacket packet = new ChangeStatusPacket(this, test.id(), newStatus);
        executeIfOn(
                () -> channel.send(PacketDistributor.ALL.noArg(), packet),
                () -> channel.sendToServer(packet),
                null
        );
    }

    @Override
    public void setEnabled(Test test, boolean enabled, @Nullable Entity changer) {
        if (!(FMLLoader.getDist().isClient() && server == null)) {
            if (enabled) {
                tests.enable(test.id());
            } else {
                tests.disable(test.id());
            }
        }

        if (enabled) {
            changeStatus(test, new Test.Status(Test.Result.NOT_PROCESSED, "Event was enabled."), changer);
        }

        final ChangeEnabledPacket packet = new ChangeEnabledPacket(TestFrameworkImpl.this, test.id(), enabled);
        executeIfOn(
                () -> channel.send(PacketDistributor.ALL.noArg(), packet),
                () -> channel.sendToServer(packet),
                null
        );
    }

    @SuppressWarnings("SameParameterValue")
    private void executeIfOn(@Nullable Runnable onServer, @Nullable Runnable remoteClient, @Nullable Runnable singlePlayer) {
        if (FMLLoader.getDist().isClient() && server != null) {
            if (singlePlayer != null) singlePlayer.run();
        } else if (FMLLoader.getDist().isClient()) {
            if (remoteClient != null) remoteClient.run();
        } else if (FMLLoader.getDist().isDedicatedServer() && server != null) {
            if (onServer != null) onServer.run();
        }
    }

    public final class TestsImpl implements Tests {
        private final Map<String, Test> tests = Collections.synchronizedMap(new HashMap<>());
        private final Map<String, EventListenerCollectorImpl> collectors = new HashMap<>();
        public final Set<String> enabled = Collections.synchronizedSet(new LinkedHashSet<>());

        @Override
        public Optional<Test> byId(String id) {
            synchronized (tests) {
                return Optional.ofNullable(tests.get(id));
            }
        }

        @Override
        public void enable(String id) {
            final EventListenerCollectorImpl collector = collectors.computeIfAbsent(id, it -> new EventListenerCollectorImpl()
                    .add(Mod.EventBusSubscriber.Bus.MOD, modBus)
                    .add(Mod.EventBusSubscriber.Bus.FORGE, MinecraftForge.EVENT_BUS));
            byId(id).orElseThrow().onEnabled(collector);
            synchronized (enabled) {
                enabled.add(id);
            }
        }

        @Override
        public void disable(String id) {
            byId(id).orElseThrow().onDisabled();
            Optional.ofNullable(collectors.get(id)).ifPresent(EventListenerCollectorImpl::unregister);
            synchronized (enabled) {
                enabled.remove(id);
            }
        }

        @Override
        public boolean isEnabled(String id) {
            synchronized (enabled) {
                return enabled.contains(id);
            }
        }

        @Override
        public void register(Test test) {
            synchronized (tests) {
                tests.put(test.id(), test);
            }
            test.init(TestFrameworkImpl.this);
        }

        @Override
        public Collection<Test> all() {
            synchronized (tests) {
                return Collections.unmodifiableCollection(tests.values());
            }
        }

        public Stream<Test> enabled() {
            synchronized (enabled) {
                return enabled.stream().flatMap(it -> byId(it).stream());
            }
        }

        private void initialiseTests() {
            synchronized (tests) {
                for (final Test test : tests.values()) {
                    if (test.enabledByDefault()) {
                        enable(test.id());
                    }
                    test.setStatus(new Test.Status(Test.Result.NOT_PROCESSED, ""));
                }
            }
        }
    }

    public static final class Client {
        private final TestFrameworkImpl impl;
        private final int toggleOverlayKey;

        public Client(TestFrameworkImpl impl, int toggleOverlayKey) {
            this.impl = impl;
            this.toggleOverlayKey = toggleOverlayKey;
        }

        public void init(IEventBus modBus) {
            final ToggleKeyMapping overlayKey = new ToggleKeyMapping("key.testframework.toggleoverlay", toggleOverlayKey, "key.categories." + impl.id.getNamespace() + "." + impl.id.getPath(), () -> true);
            modBus.addListener((final RegisterKeyMappingsEvent event) -> event.register(overlayKey));

            modBus.addListener((final RegisterGuiOverlaysEvent event) -> event.registerAboveAll(impl.id.getPath(), new TestsOverlay(impl, () -> !overlayKey.isDown())));
        }
    }
}

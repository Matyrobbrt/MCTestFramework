package com.matyrobbrt.testframework.impl;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.TestFramework;
import com.matyrobbrt.testframework.conf.ClientConfiguration;
import com.matyrobbrt.testframework.conf.FrameworkConfiguration;
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
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.appender.rolling.OnStartupTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static net.minecraft.commands.Commands.*;

// TODO - logging
public class TestFrameworkImpl implements TestFramework {
    public final FrameworkConfiguration configuration;

    private final Logger logger;
    private final ResourceLocation id;
    private final TestsImpl tests = new TestsImpl();

    private final SimpleChannel channel;

    private @Nullable MinecraftServer server;

    public TestFrameworkImpl(FrameworkConfiguration configuration) {
        this.configuration = configuration;
        this.id = configuration.id();
        this.channel = configuration.networkingChannel();

        this.logger = LoggerFactory.getLogger("TestFramework " + this.id);
        prepareLogger();

        MinecraftForge.EVENT_BUS.addListener((final ServerStartedEvent event) -> {
            server = event.getServer();
            tests.initialiseTests();
        });
        MinecraftForge.EVENT_BUS.addListener((final ServerStoppedEvent event) -> {
            server = event.getServer() == server ? null : server;

            // Summarise test results
            // TODO - maybe dump a GitHub-flavoured markdown file?
            logger().info("Test summary processing..");
            tests().all().forEach(test -> {
                logger().info("\tTest " + test.id() + ": ");
                logger().info(test.status().result() + (test.status().message().isBlank() ? "" : " - " + test.status().message()));
            });
            logger.info("Test Framework finished.");
        });
    }

    public void registerCommands(LiteralArgumentBuilder<CommandSourceStack> node) {
        final BiFunction<LiteralArgumentBuilder<CommandSourceStack>, Boolean, LiteralArgumentBuilder<CommandSourceStack>> commandEnabling = (stack, enabling) ->
                stack.requires(it -> it.hasPermission(LEVEL_GAMEMASTERS))
                    .then(argument("id", StringArgumentType.greedyString())
                            .suggests((context, builder) -> {
                                String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
                                Stream.concat(
                                        tests.tests.keySet().stream(),
                                        tests.groups.keySet().stream().map(it -> "g:" + it)
                                    )
                                    .filter(it -> it.toLowerCase(Locale.ROOT).startsWith(remaining))
                                    .filter(it -> tests.isEnabled(it) == !enabling)
                                    .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                final String id = StringArgumentType.getString(ctx, "id");
                                if (id.startsWith("g:")) {
                                    final String groupId = id.substring(2);
                                    final List<Test> group = tests.getOrCreateGroup(groupId);
                                    if (group.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("Unknown test group with id '%s'!".formatted(groupId)));
                                        return Command.SINGLE_SUCCESS;
                                    } else if (group.stream().allMatch(it -> tests.isEnabled(it.id()) == enabling)) {
                                        ctx.getSource().sendFailure(Component.literal("All tests in group are " + (enabling ? "enabled" : "disabled") + "!"));
                                        return Command.SINGLE_SUCCESS;
                                    } else {
                                        group.forEach(test -> setEnabled(test, enabling, ctx.getSource().getEntity()));
                                        ctx.getSource().sendSuccess(Component.literal((enabling ? "Enabled" : "Disabled") + " test group!"), true);
                                        return Command.SINGLE_SUCCESS;
                                    }
                                } else {
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
                                }
                            }));
        node.then(commandEnabling.apply(literal("enable"), true));
        node.then(commandEnabling.apply(literal("disable"), false));
    }

    private IEventBus modBus;
    public void init(final IEventBus modBus, final ModContainer container) {
        this.modBus = modBus;
        final List<Test> collected = collectTests(container);
        logger.info("Found {} tests: {}", collected.size(), String.join(", ", collected.stream().map(Test::id).toList()));
        collected.forEach(tests()::register);

        modBus.addListener((final FMLCommonSetupEvent event) -> setupPackets());

        if (FMLLoader.getDist().isClient()) {
            setupClient(this, modBus);
        }
    }

    private static void setupClient(TestFrameworkImpl impl, IEventBus modBus) {
        if (impl.configuration.clientConfiguration() != null) new Client(impl, impl.configuration.clientConfiguration().get()).init(modBus);
        MinecraftForge.EVENT_BUS.addListener((final ClientPlayerNetworkEvent.LoggingIn logOut) -> {
            synchronized (impl.tests().enabled) {
                List.copyOf(impl.tests().enabled).forEach(impl.tests()::disable);
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
        registrar.register(ChangeStatusPacket.class, ChangeStatusPacket::decode);
        registrar.register(ChangeEnabledPacket.class, ChangeEnabledPacket::decode);
    }

    protected List<Test> collectTests(final ModContainer container) {
        return configuration.testCollector().collect(container);
    }

    @Override
    public IEventBus modEventBus() {
        return modBus;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public TestsImpl tests() {
        return tests;
    }

    @Override
    public void changeStatus(Test test, Test.Status newStatus, @Nullable Entity changer) {
        test.setStatus(newStatus);

        logger.info("Status of test '{}' has had status changed to {}{}.", test.id(), newStatus, changer instanceof Player player ? " by " + player.getGameProfile().getName() : "");

        final ChangeStatusPacket packet = new ChangeStatusPacket(this, test.id(), newStatus);
        sendPacketIfOn(
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

        logger.info("Test '{}' has been {}{}.", test.id(), enabled ? "enabled" : "disabled", changer instanceof Player player ? " by " + player.getGameProfile().getName() : "");

        if (enabled) {
            changeStatus(test, new Test.Status(Test.Result.NOT_PROCESSED, ""), changer);
        }

        final ChangeEnabledPacket packet = new ChangeEnabledPacket(TestFrameworkImpl.this, test.id(), enabled);
        sendPacketIfOn(
                () -> channel.send(PacketDistributor.ALL.noArg(), packet),
                () -> channel.sendToServer(packet),
                null
        );
    }

    @SuppressWarnings("SameParameterValue")
    private void sendPacketIfOn(@Nullable Runnable onServer, @Nullable Runnable remoteClient, @Nullable Runnable singlePlayer) {
        if (FMLLoader.getDist().isClient() && server != null) {
            if (singlePlayer != null) singlePlayer.run();
        } else if (FMLLoader.getDist().isClient()) {
            if (remoteClient != null && configuration.modifiableByClients()) remoteClient.run();
        } else if (FMLLoader.getDist().isDedicatedServer() && server != null) {
            if (onServer != null && configuration.clientSynced()) onServer.run();
        }
    }

    /**
     * Set the LOGGER instance in this class to only write to logs/curletest.log.
     * Log4J is annoying and requires manual initialization and preparation.
     */
    private void prepareLogger() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        final LoggerConfig loggerConfig = getLoggerConfiguration(config, logger.getName());

        final RollingRandomAccessFileAppender appender = RollingRandomAccessFileAppender.newBuilder()
                .setName("TestFramework " + id + " log")
                .withFileName("logs/" + id.toString().replace(":", "_") + ".log")
                .withFilePattern("logs/%d{yyyy-MM-dd}-%i.log.gz")
                .setLayout(PatternLayout.newBuilder()
                        .withPattern("[%d{ddMMMyyyy HH:mm:ss}] [%logger]: %minecraftFormatting{%msg}{strip}%n%xEx")
                        .build()
                )
                .withPolicy(
                        OnStartupTriggeringPolicy.createPolicy(1)
                )
                .build();

        appender.start();

        loggerConfig.setParent(null);
        loggerConfig.getAppenders().keySet().forEach(loggerConfig::removeAppender);
        loggerConfig.addAppender(
                appender,
                Level.DEBUG,
                null
        );
    }

    private static LoggerConfig getLoggerConfiguration(@NotNull final Configuration configuration, @NotNull final String loggerName) {
        final LoggerConfig lc = configuration.getLoggerConfig(loggerName);
        if (lc.getName().equals(loggerName)) {
            return lc;
        } else {
            final LoggerConfig nlc = new LoggerConfig(loggerName, lc.getLevel(), lc.isAdditive());
            nlc.setParent(lc);
            configuration.addLogger(loggerName, nlc);
            configuration.getLoggerContext().updateLoggers();

            return nlc;
        }
    }

    public final class TestsImpl implements Tests {
        private final Map<String, Test> tests = Collections.synchronizedMap(new HashMap<>());
        private final ListMultimap<String, Test> groups = Multimaps.synchronizedListMultimap(Multimaps.newListMultimap(new LinkedHashMap<>(), ArrayList::new));
        private final Map<String, EventListenerCollectorImpl> collectors = new HashMap<>();
        public final Set<String> enabled = Collections.synchronizedSet(new LinkedHashSet<>());

        @Override
        public Optional<Test> byId(String id) {
            synchronized (tests) {
                return Optional.ofNullable(tests.get(id));
            }
        }

        @Override
        public List<Test> getOrCreateGroup(String id) {
            synchronized (groups) {
                return groups.get(id);
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
            synchronized (groups) {
                test.groups().forEach(group -> groups.put(group, test));
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
            Predicate<Test> isEnabledByDefault = Test::enabledByDefault;

            final Set<String> enabledTests = new HashSet<>(configuration.enabledTests());
            final Set<String> enabledGroups = new HashSet<>();
            final Iterator<String> etestsItr = enabledTests.iterator();
            while (etestsItr.hasNext()) {
                final String next = etestsItr.next();
                if (next.startsWith("g:")) {
                    enabledGroups.add(next.substring(2));
                    etestsItr.remove();
                }
            }

            isEnabledByDefault = isEnabledByDefault.or(it -> enabledTests.contains(it.id()));
            isEnabledByDefault = isEnabledByDefault.or(it -> it.groups().stream().anyMatch(enabledGroups::contains));

            synchronized (tests) {
                for (final Test test : tests.values()) {
                    if (isEnabledByDefault.test(test)) {
                        enable(test.id());
                    }
                    test.setStatus(new Test.Status(Test.Result.NOT_PROCESSED, ""));
                }
            }
        }
    }

    public static final class Client {
        private final TestFrameworkImpl impl;
        private final ClientConfiguration configuration;

        public Client(TestFrameworkImpl impl, ClientConfiguration clientConfiguration) {
            this.impl = impl;
            this.configuration = clientConfiguration;
        }

        public void init(IEventBus modBus) {
            final BooleanSupplier overlayEnabled;
            if (configuration.toggleOverlayKey() != 0) {
                final ToggleKeyMapping overlayKey = new ToggleKeyMapping("key.testframework.toggleoverlay", configuration.toggleOverlayKey(), "key.categories." + impl.id.getNamespace() + "." + impl.id.getPath(), () -> true);
                modBus.addListener((final RegisterKeyMappingsEvent event) -> event.register(overlayKey));
                overlayEnabled = () -> !overlayKey.isDown();
            } else {
                overlayEnabled = () -> true;
            }

            modBus.addListener((final RegisterGuiOverlaysEvent event) -> event.registerAboveAll(impl.id.getPath(), new TestsOverlay(impl, overlayEnabled)));
        }
    }
}

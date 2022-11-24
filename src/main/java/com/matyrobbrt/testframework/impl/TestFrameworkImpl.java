package com.matyrobbrt.testframework.impl;

import com.matyrobbrt.testframework.Test;
import com.matyrobbrt.testframework.conf.FrameworkConfiguration;
import com.matyrobbrt.testframework.group.Group;
import com.matyrobbrt.testframework.impl.packet.ChangeEnabledPacket;
import com.matyrobbrt.testframework.impl.packet.ChangeStatusPacket;
import com.matyrobbrt.testframework.impl.packet.TFPacket;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.minecraft.commands.Commands.*;

@ApiStatus.Internal
public class TestFrameworkImpl implements TestFrameworkInternal {
    private final FrameworkConfiguration configuration;
    private final @Nullable FrameworkClient client;

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

        if (configuration.clientConfiguration() != null) {
            this.client = FrameworkClient.FACTORY.map(it -> it.create(this, configuration.clientConfiguration().get())).orElse(null);
        } else {
            this.client = null;
        }

        MinecraftForge.EVENT_BUS.addListener((final ServerStartedEvent event) -> {
            server = event.getServer();
            tests.initialiseDefaultEnabledTests();
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

    @Override
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
                                    final List<Test> group = tests.getOrCreateGroup(groupId).resolveAll();
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

    @Override
    public FrameworkConfiguration configuration() {
        return configuration;
    }

    private IEventBus modBus;
    @Override
    public void init(final IEventBus modBus, final ModContainer container) {
        this.modBus = modBus;
        final List<Test> collected = collectTests(container);
        logger.info("Found {} tests: {}", collected.size(), String.join(", ", collected.stream().map(Test::id).toList()));
        collected.forEach(tests()::register);
        collectGroupNames(container);

        modBus.addListener((final FMLCommonSetupEvent event) -> setupPackets());

        if (FMLLoader.getDist().isClient()) {
            setupClient(this, modBus, container);
        }
    }

    private void collectGroupNames(ModContainer container) {
        if (configuration.groupNameCollector() == null) return;
        container.getModInfo().getOwningFile().getFile().getScanResult()
                .getAnnotations().stream().filter(it -> configuration.groupNameCollector().asmType().equals(it.annotationType()))
                .forEach(LamdbaExceptionUtils.rethrowConsumer(annotationData -> {
                    final Class<?> clazz = Class.forName(annotationData.clazz().getClassName());
                    final Field field = clazz.getDeclaredField(annotationData.memberName());
                    final String groupId = (String)field.get(null);
                    final Annotation annotation = field.getAnnotation(configuration.groupNameCollector().annotation());
                    final Group group = tests().getOrCreateGroup(groupId);
                    group.setTitle(configuration.groupNameCollector().getName(annotation));
                    group.setEnabledByDefault(configuration.groupNameCollector().isEnabledByDefault(annotation));
                }));
    }

    private static void setupClient(TestFrameworkImpl impl, IEventBus modBus, ModContainer container) {
        if (impl.client != null) impl.client.init(modBus, container);
        MinecraftForge.EVENT_BUS.addListener((final ClientPlayerNetworkEvent.LoggingIn logOut) -> {
            synchronized (impl.tests().enabled) {
                List.copyOf(impl.tests().enabled).forEach(impl.tests()::disable);
            }
            impl.tests().initialiseDefaultEnabledTests();
        });
    }

    private void setupPackets() {
        class Registrar {
            private final SimpleChannel channel;
            int id = 0;

            Registrar(SimpleChannel channel) {
                this.channel = channel;
            }

            <P extends TFPacket> void register(Class<P> pkt, BiFunction<TestFrameworkInternal, FriendlyByteBuf, P> decoder) {
                channel.messageBuilder(pkt, id++)
                        .consumerMainThread((packet, contextSupplier) -> {
                            final var ctx = contextSupplier.get();
                            packet.handle(ctx);
                        })
                        .encoder(TFPacket::encode)
                        .decoder(buf -> decoder.apply(TestFrameworkImpl.this, buf))
                        .add();
            }

            <P extends TFPacket> void registerNetworkThread(Class<P> pkt, BiFunction<TestFrameworkInternal, FriendlyByteBuf, P> decoder) {
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

    @Override
    public List<Test> collectTests(final ModContainer container) {
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
    @SuppressWarnings("deprecation")
    public void changeStatus(Test test, Test.Status newStatus, @Nullable Entity changer) {
        if (test.status().equals(newStatus)) return; // If the status is the same, don't waste power

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
        if (tests.isEnabled(test.id()) == enabled) return; // If the status is the same, don't waste power

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
                .withFileName("logs/tests/" + id.toString().replace(":", "_") + ".log")
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

    public final class TestsImpl implements TestsInternal {
        private final Map<String, Test> tests = Collections.synchronizedMap(new HashMap<>());
        private final Map<String, Group> groups = Collections.synchronizedMap(new LinkedHashMap<>());
        private final Map<String, EventListenerGroupImpl> collectors = new HashMap<>();
        private final Set<String> enabled = Collections.synchronizedSet(new LinkedHashSet<>());

        @Override
        public Optional<Test> byId(String id) {
            synchronized (tests) {
                return Optional.ofNullable(tests.get(id));
            }
        }

        @Override
        public Group getOrCreateGroup(String id) {
            synchronized (groups) {
                @Nullable Group group = groups.get(id);
                if (group != null) return group;
                group = addGroupToParents(new Group(id, new CopyOnWriteArrayList<>()));
                groups.put(id, group);
                return group;
            }
        }

        @Override
        public Optional<Group> maybeGetGroup(String id) {
            synchronized (groups) {
                return Optional.ofNullable(groups.get(id));
            }
        }

        @Override
        public Collection<Group> allGroups() {
            synchronized (groups) {
                return groups.values();
            }
        }

        @Override
        public void enable(String id) {
            final EventListenerGroupImpl collector = collectors.computeIfAbsent(id, it -> new EventListenerGroupImpl()
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
            Optional.ofNullable(collectors.get(id)).ifPresent(EventListenerGroupImpl::unregister);
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
                if (test.groups().isEmpty()) {
                    getOrCreateGroup("ungrouped").add(test);
                } else {
                    test.groups().forEach(group -> getOrCreateGroup(group).add(test));
                }
            }
            test.init(TestFrameworkImpl.this);
        }

        private Group addGroupToParents(Group group) {
            synchronized (groups) {
                final List<String> splitOnDot = List.of(group.id().split("\\."));
                if (splitOnDot.size() >= 2) {
                    final Group parent = getOrCreateGroup(String.join(".", splitOnDot.subList(0, splitOnDot.size() - 1)));
                    parent.add(group);
                }
            }
            return group;
        }

        @Override
        public Collection<Test> all() {
            synchronized (tests) {
                return Collections.unmodifiableCollection(tests.values());
            }
        }

        @Override
        public Stream<Test> enabled() {
            synchronized (enabled) {
                return enabled.stream().flatMap(it -> byId(it).stream());
            }
        }

        @Override
        @SuppressWarnings("deprecation")
        public void initialiseDefaultEnabledTests() {
            synchronized (enabled) {
                enabled.clear();
            }
            Predicate<Test> isEnabledByDefault = Test::enabledByDefault;

            final Set<String> enabledTests = new HashSet<>(configuration.enabledTests());
            final Set<Group> enabledGroups = new HashSet<>();
            final Iterator<String> etestsItr = enabledTests.iterator();
            while (etestsItr.hasNext()) {
                final String next = etestsItr.next();
                if (next.startsWith("g:")) {
                    enabledGroups.add(getOrCreateGroup(next.substring(2)));
                    etestsItr.remove();
                }
            }

            synchronized (groups) {
                enabledGroups.addAll(groups.values().stream().filter(Group::isEnabledByDefault).toList());
            }
            final Set<Test> groupTestsEnabledByDefault = enabledGroups.stream().flatMap(it -> it.resolveAll().stream()).collect(Collectors.toSet());

            isEnabledByDefault = isEnabledByDefault.or(it -> enabledTests.contains(it.id()));
            isEnabledByDefault = isEnabledByDefault.or(groupTestsEnabledByDefault::contains);

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

}

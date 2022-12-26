package com.matyrobbrt.testframework.impl;

import com.mojang.bridge.game.PackType;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraftforge.resource.PathPackResources;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
record TestModPackFinder(Path source, Predicate<String> shouldRegister) implements RepositorySource {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void loadPacks(Consumer<Pack> pInfoConsumer, Pack.PackConstructor pInfoFactory) {
        try (final Stream<Path> packs = Files.walk(source, 1)) {
            final Iterator<Path> itr = packs.iterator();
            while (itr.hasNext()) {
                final Path path = itr.next();
                if (Files.isDirectory(path) && shouldRegister.test(path.getFileName().toString())) {
                    load(pInfoConsumer, pInfoFactory, path);
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Encountered exception finding test mod packs: ", exception);
        }
    }

    private void load(Consumer<Pack> pInfoConsumer, Pack.PackConstructor pInfoFactory, Path packPath) throws IOException {
        final String packId = "test_mod_" + packPath.getFileName().toString();
        final Pack pack = pInfoFactory.create(
                packId, Component.literal(packId),
                true, () -> new PathPackResources(packId, packPath),
                new PackMetadataSection(Component.literal(packId + " test mod resources"), SharedConstants.getCurrentVersion().getPackVersion(PackType.RESOURCE)),
                Pack.Position.TOP, PackSource.BUILT_IN,
                true
        );
        pInfoConsumer.accept(pack);
    }
}

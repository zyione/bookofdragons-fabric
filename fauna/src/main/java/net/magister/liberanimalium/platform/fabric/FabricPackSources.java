package net.magister.liberanimalium.platform.fabric;

import java.util.ArrayList;
import java.util.Arrays;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.magister.liberanimalium.platform.world.GameRegistrar;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.RepositorySource;

public final class FabricPackSources {
    public static RepositorySource[] add(PackType type, RepositorySource[] original) {
        var result = new ArrayList<>(Arrays.asList(original));
        // Minecraft constructs its client repository before Fabric's mod init.
        // Discover providers lazily when packs are scanned, after listeners exist.
        result.add(consumer -> {
            var dynamic = new ArrayList<RepositorySource>();
            GameRegistrar.PackSourceSink sink = new GameRegistrar.PackSourceSink() {
                public PackType getPackType() { return type; }
                public void addRepositorySource(RepositorySource source) { dynamic.add(source); }
            };
            CommonEvents.PACK_SOURCES.allEach(l -> l.accept(sink));
            for (RepositorySource provider : dynamic) provider.loadPacks(consumer);
        });
        return result.toArray(RepositorySource[]::new);
    }
}

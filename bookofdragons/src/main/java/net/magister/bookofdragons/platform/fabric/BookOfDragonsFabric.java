package net.magister.bookofdragons.platform.fabric;

import java.util.Optional;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.minecraftforge.fml.config.ModConfig;
import net.magister.bookofdragons.BookOfDragons;
import net.magister.bookofdragons.block.ModBlocks;
import net.magister.bookofdragons.block.entity.ModBlockEntities;
import net.magister.bookofdragons.config.ModClientConfig;
import net.magister.bookofdragons.config.ModCommonConfig;
import net.magister.bookofdragons.discovery.DiscoveryStore;
import net.magister.bookofdragons.effect.ModEffects;
import net.magister.bookofdragons.entity.ModEntities;
import net.magister.bookofdragons.inventory.ModMenuTypes;
import net.magister.bookofdragons.item.ModCreativeModeTabs;
import net.magister.bookofdragons.item.ModItems;
import net.magister.bookofdragons.particle.ModParticles;
import net.magister.bookofdragons.sound.ModSounds;
import net.magister.bookofdragons.villager.ModVillagers;
import net.magister.bookofdragons.worldgen.ModStructureProcessors;
import net.magister.liberanimalium.LiberAnimalium;
import net.magister.liberanimalium.platform.fabric.FabricCommonBridge;
import net.magister.liberanimalium.platform.fabric.EntityData;

public final class BookOfDragonsFabric implements ModInitializer {
    public void onInitialize() {
        ForgeConfigRegistry.INSTANCE.register("bookofdragons", ModConfig.Type.CLIENT, ModClientConfig.SPEC, "bookofdragons-client.toml");
        ForgeConfigRegistry.INSTANCE.register("bookofdragons", ModConfig.Type.COMMON, ModCommonConfig.SPEC, "bookofdragons-common.toml");
        DiscoveryStore.install(player -> Optional.of(((DiscoveryCarrier)player).bookofdragons$discovery()));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, player, alive) -> {
            ((DiscoveryCarrier)player).bookofdragons$discovery().copyFrom(((DiscoveryCarrier)oldPlayer).bookofdragons$discovery());
        });
        BookOfDragons.init();
        ModCreativeModeTabs.register();
        ModBlocks.register();
        ModItems.register();
        ModEntities.register();
        ModMenuTypes.register();
        ModSounds.register();
        ModParticles.register();
        ModVillagers.register();
        ModBlockEntities.register();
        ModStructureProcessors.register();
        ModEffects.register();
        ParalysisFabric.install();
        net.magister.liberanimalium.platform.fabric.FabricBootstrap.ready("bookofdragons", BookOfDragons::setup);
    }
}

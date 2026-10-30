package net.magister.liberanimalium.platform.fabric;

import net.fabricmc.api.ModInitializer;
import net.magister.liberanimalium.LiberAnimalium;
import net.magister.liberanimalium.attack.client.particle.FaunaParticles;
import net.magister.liberanimalium.body.effect.FaunaEffects;
import net.magister.liberanimalium.body.nav.noise.FaunaGameEvents;
import net.magister.liberanimalium.body.registry.FaunaEntities;

public final class FaunaFabric implements ModInitializer {
    public void onInitialize() {
        FabricCommonBridge.install();
        FaunaEntities.register();
        FaunaParticles.register();
        FaunaEffects.register();
        FaunaGameEvents.register();
        LiberAnimalium.init();
        FabricBootstrap.ready("liberanimalium", LiberAnimalium::setup);
    }
}

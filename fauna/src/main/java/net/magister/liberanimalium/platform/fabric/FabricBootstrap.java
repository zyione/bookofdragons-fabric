package net.magister.liberanimalium.platform.fabric;

import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entrypoint order is not the Forge registration/setup lifecycle. */
public final class FabricBootstrap {
    private static final Map<String, Runnable> READY = new LinkedHashMap<>();
    private static boolean finished;
    public static void ready(String mod, Runnable setup) {
        if (finished || READY.putIfAbsent(mod, setup) != null) throw new IllegalStateException("Repeated initialization " + mod);
        if (!READY.containsKey("liberanimalium")) return;
        if (FabricLoader.getInstance().isModLoaded("bookofdragons") && !READY.containsKey("bookofdragons")) return;
        FabricCommonBridge.setup();
        READY.get("liberanimalium").run();
        if (READY.containsKey("bookofdragons")) READY.get("bookofdragons").run();
        finished = true;
    }
}

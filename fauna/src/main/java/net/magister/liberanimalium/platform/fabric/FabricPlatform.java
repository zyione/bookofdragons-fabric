package net.magister.liberanimalium.platform.fabric;

import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.magister.liberanimalium.platform.Platform;

public final class FabricPlatform implements Platform {
    public boolean isClient() { return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT; }
    public boolean isProduction() { return !FabricLoader.getInstance().isDevelopmentEnvironment(); }
    public Path configDir() { return FabricLoader.getInstance().getConfigDir(); }
    public Path gameDir() { return FabricLoader.getInstance().getGameDir(); }
}

package net.magister.liberanimalium.platform.fabric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public final class FabricServer {
    private static MinecraftServer server;
    private FabricServer() {}
    public static void install() {
        ServerLifecycleEvents.SERVER_STARTING.register(value -> server = value);
        ServerLifecycleEvents.SERVER_STOPPED.register(value -> { if (server == value) server = null; });
    }
    public static MinecraftServer current() {
        if (server == null) throw new IllegalStateException("No active server");
        return server;
    }
}

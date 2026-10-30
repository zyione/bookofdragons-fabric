package net.magister.liberanimalium.platform.fabric;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.resource.*;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.magister.liberanimalium.platform.world.GameRegistrar;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public final class FabricCommonBridge {
    public static void install() {
        FabricServer.install();
        FragmentedC2S.install();
        ServerTickEvents.END_SERVER_TICK.register(server -> CommonEvents.SERVER_TICK_END.allEach(l -> l.accept(server)));
        ServerTickEvents.END_WORLD_TICK.register(level -> CommonEvents.LEVEL_TICK_END.allEach(l -> l.accept(level)));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> CommonEvents.SERVER_STOPPING.allEach(l -> l.accept(server)));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof net.magister.liberanimalium.chassis.FaunaBase fauna) fauna.onAddedToWorld();
            CommonEvents.ENTITY_JOIN.allEach(l -> l.onJoin(entity, level, ((EntityData)entity).liberanimalium$loadedFromDisk()));
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> CommonEvents.ENTITY_LEAVE.allEach(l -> l.onLeave(entity, level)));
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> CommonEvents.CHUNK_LOAD.allEach(l -> l.onChunk(level, chunk)));
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> CommonEvents.CHUNK_UNLOAD.allEach(l -> l.onChunk(level, chunk)));
        ServerWorldEvents.UNLOAD.register((server, level) -> CommonEvents.LEVEL_UNLOAD.allEach(l -> l.accept(level)));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> CommonEvents.LIVING_DIED.allEach(l -> l.onDied(entity, source)));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> CommonEvents.PLAYER_LOGIN.allEach(l -> l.accept(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CommonEvents.PLAYER_LOGOUT.allEach(l -> l.accept(handler.player)));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, player, alive) -> CommonEvents.PLAYER_RESPAWN.allEach(l -> l.accept(player)));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, player, alive) -> {
            if (EntityData.get(oldPlayer).contains("PlayerPersisted", 10))
                EntityData.get(player).put("PlayerPersisted", EntityData.get(oldPlayer).getCompound("PlayerPersisted").copy());
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> CommonEvents.COMMANDS.allEach(l -> l.register(dispatcher, context)));
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> CommonEvents.TAGS_UPDATED.allEach(Runnable::run));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            if (success) CommonEvents.DATA_RELOADED.allEach(l -> l.accept(java.util.List.copyOf(server.getPlayerList().getPlayers())));
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            CommonEvents.USE_BLOCK.allEach(l -> l.onUseBlock(player, level, hand, hit));
            return InteractionResult.PASS;
        });
    }
    /** Must run after both mods have registered their shared listeners. */
    public static void setup() {
        FabricRegistration.flush();
        GameRegistrar.AttributeSink attributes = FabricDefaultAttributeRegistry::register;
        CommonEvents.ENTITY_ATTRIBUTES.allEach(l -> l.accept(attributes));
        GameRegistrar.SpawnPlacementSink placements = new GameRegistrar.SpawnPlacementSink() {
            public <T extends Mob> void registerOnGround(EntityType<T> type, Heightmap.Types height, SpawnPlacements.SpawnPredicate<T> predicate) {
                SpawnPlacements.register(type, SpawnPlacements.Type.ON_GROUND, height, predicate);
            }
        };
        CommonEvents.SPAWN_PLACEMENTS.allEach(l -> l.accept(placements));
        GameRegistrar.TradeSink trades = (profession, level, listing) -> TradeOfferHelper.registerVillagerOffers(profession, level, list -> list.add(listing));
        CommonEvents.VILLAGER_TRADES.allEach(l -> l.accept(trades));
        int[] count = {0};
        CommonEvents.ReloadListenerSink reloads = listener -> ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
            new IdentifiableResourceReloadListener() {
                private final ResourceLocation id = new ResourceLocation("liberanimalium", "server_reload_" + count[0]++);
                public ResourceLocation getFabricId() { return id; }
                public java.util.concurrent.CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier,
                    net.minecraft.server.packs.resources.ResourceManager manager, net.minecraft.util.profiling.ProfilerFiller prepare,
                    net.minecraft.util.profiling.ProfilerFiller apply, java.util.concurrent.Executor background, java.util.concurrent.Executor game) {
                    return listener.reload(barrier, manager, prepare, apply, background, game);
                }
            });
        CommonEvents.RELOAD_LISTENERS.allEach(l -> l.accept(reloads));
    }
}

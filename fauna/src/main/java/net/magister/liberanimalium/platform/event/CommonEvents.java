package net.magister.liberanimalium.platform.event;

import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import java.util.function.Consumer;
import net.magister.liberanimalium.platform.world.GameRegistrar;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.BlockHitResult;

public final class CommonEvents {
   public static final Event<Consumer<MinecraftServer>> SERVER_TICK_END = new Event<>(Consumer.class);
   public static final Event<Consumer<Level>> LEVEL_TICK_END = new Event<>(Consumer.class);
   public static final Event<Consumer<MinecraftServer>> SERVER_STOPPING = new Event<>(Consumer.class);
   public static final Event<CommonEvents.EntityJoin> ENTITY_JOIN = new Event<>(CommonEvents.EntityJoin.class);
   public static final Event<CommonEvents.EntityLeave> ENTITY_LEAVE = new Event<>(CommonEvents.EntityLeave.class);
   public static final Event<CommonEvents.Mount> MOUNT = new Event<>(CommonEvents.Mount.class);
   public static final Event<CommonEvents.LivingDamaged> LIVING_DAMAGED = new Event<>(CommonEvents.LivingDamaged.class);
   public static final Event<CommonEvents.LivingDied> LIVING_DIED = new Event<>(CommonEvents.LivingDied.class);
   public static final Event<CommonEvents.AnimalTamed> ANIMAL_TAMED = new Event<>(CommonEvents.AnimalTamed.class);
   public static final Event<CommonEvents.AdvancementEarned> ADVANCEMENT_EARNED = new Event<>(CommonEvents.AdvancementEarned.class);
   public static final Event<Consumer<ServerPlayer>> PLAYER_LOGIN = new Event<>(Consumer.class);
   public static final Event<Consumer<ServerPlayer>> PLAYER_LOGOUT = new Event<>(Consumer.class);
   public static final Event<Consumer<ServerPlayer>> PLAYER_RESPAWN = new Event<>(Consumer.class);
   public static final Event<CommonEvents.ChunkChange> CHUNK_LOAD = new Event<>(CommonEvents.ChunkChange.class);
   public static final Event<CommonEvents.ChunkChange> CHUNK_UNLOAD = new Event<>(CommonEvents.ChunkChange.class);
   public static final Event<Consumer<LevelAccessor>> LEVEL_UNLOAD = new Event<>(Consumer.class);
   public static final Event<Runnable> TAGS_UPDATED = new Event<>(Runnable.class);
   public static final Event<Consumer<List<ServerPlayer>>> DATA_RELOADED = new Event<>(Consumer.class);
   public static final Event<CommonEvents.Detonation> EXPLOSION = new Event<>(CommonEvents.Detonation.class);
   public static final Event<CommonEvents.BlockChange> BLOCK_PLACED = new Event<>(CommonEvents.BlockChange.class);
   public static final Event<CommonEvents.BlockChange> BLOCK_BROKEN = new Event<>(CommonEvents.BlockChange.class);
   public static final Event<CommonEvents.UseBlock> USE_BLOCK = new Event<>(CommonEvents.UseBlock.class);
   public static final Event<CommonEvents.CommandRegistration> COMMANDS = new Event<>(CommonEvents.CommandRegistration.class);
   public static final Event<Consumer<CommonEvents.ReloadListenerSink>> RELOAD_LISTENERS = new Event<>(Consumer.class);
   public static final Event<Consumer<GameRegistrar.AttributeSink>> ENTITY_ATTRIBUTES = new Event<>(Consumer.class);
   public static final Event<Consumer<GameRegistrar.SpawnPlacementSink>> SPAWN_PLACEMENTS = new Event<>(Consumer.class);
   public static final Event<Consumer<GameRegistrar.TradeSink>> VILLAGER_TRADES = new Event<>(Consumer.class);
   public static final Event<Consumer<GameRegistrar.PackSourceSink>> PACK_SOURCES = new Event<>(Consumer.class);
   public static final Event<CommonEvents.LegacyData> LEGACY_DATA = new Event<>(CommonEvents.LegacyData.class);

   private CommonEvents() {
   }

   @FunctionalInterface
   public interface AdvancementEarned {
      void onEarned(ServerPlayer var1, ResourceLocation var2);
   }

   @FunctionalInterface
   public interface AnimalTamed {
      void onTamed(Animal var1, Player var2);
   }

   @FunctionalInterface
   public interface BlockChange {
      void onBlock(LevelAccessor var1, BlockPos var2);
   }

   @FunctionalInterface
   public interface ChunkChange {
      void onChunk(LevelAccessor var1, ChunkAccess var2);
   }

   @FunctionalInterface
   public interface CommandRegistration {
      void register(CommandDispatcher<CommandSourceStack> var1, CommandBuildContext var2);
   }

   @FunctionalInterface
   public interface Detonation {
      void onDetonate(Level var1, Explosion var2, List<BlockPos> var3, List<Entity> var4);
   }

   @FunctionalInterface
   public interface EntityJoin {
      void onJoin(Entity var1, Level var2, boolean var3);
   }

   @FunctionalInterface
   public interface EntityLeave {
      void onLeave(Entity var1, Level var2);
   }

   @FunctionalInterface
   public interface LegacyData {
      void onLoad(Entity var1, CompoundTag var2, CompoundTag var3);
   }

   @FunctionalInterface
   public interface LivingDamaged {
      void onDamaged(LivingEntity var1, DamageSource var2, float var3);
   }

   @FunctionalInterface
   public interface LivingDied {
      void onDied(LivingEntity var1, DamageSource var2);
   }

   @FunctionalInterface
   public interface Mount {
      void onMount(Entity var1, Entity var2, Level var3, boolean var4);
   }

   @FunctionalInterface
   public interface ReloadListenerSink {
      void addListener(PreparableReloadListener var1);
   }

   @FunctionalInterface
   public interface UseBlock {
      void onUseBlock(Player var1, Level var2, InteractionHand var3, BlockHitResult var4);
   }
}

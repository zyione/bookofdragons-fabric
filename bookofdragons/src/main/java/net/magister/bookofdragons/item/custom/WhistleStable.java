package net.magister.bookofdragons.item.custom;

import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.datagen.data.DragonRegistryData;
import net.magister.bookofdragons.datagen.data.DragonRegistryEntry;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonNames;
import net.magister.bookofdragons.network.Strip;
import net.magister.liberanimalium.body.locomotion.ModeChangeCause;
import net.magister.liberanimalium.body.locomotion.transition.LocomotionTransition;
import net.magister.liberanimalium.chassis.component.CommandComponent;
import net.magister.liberanimalium.chassis.component.GripComponent;
import net.magister.liberanimalium.chassis.component.GripComponent.Release;
import net.magister.liberanimalium.chassis.component.command.Modifier;
import net.magister.liberanimalium.chassis.component.command.Stance;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.magister.liberanimalium.util.TerrainUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class WhistleStable {
   private static final Map<UUID, WhistleStable.Reach> REACHING = new ConcurrentHashMap<>();
   private static final long REACH_TICKS = 200L;

   private WhistleStable() {
   }

   public static void init() {
      CommonEvents.SERVER_TICK_END.register(WhistleStable::tick);
   }

   public static WhistleStable.Call call(ServerPlayer player, ServerLevel level, UUID id) {
      DragonRegistryData registry = DragonRegistryData.get(level.getServer());
      DragonBase live = find(level.getServer(), id);
      if (live != null) {
         registry.take(id);
         return callLoaded(live, player, level);
      }

      if (!registry.isStored(id)) {
         return reach(player, level, registry.getDragon(id), id);
      }

      DragonBase out = release(player, level, id);
      return new WhistleStable.Call(out == null ? WhistleStable.Outcome.FAILED : WhistleStable.Outcome.CALLED, out);
   }

   private static WhistleStable.Call reach(ServerPlayer player, ServerLevel level, @Nullable DragonRegistryEntry entry, UUID id) {
      if (entry != null && entry.getLastKnownPos() != null) {
         if (!level.dimension().equals(entry.getDimension())) {
            return new WhistleStable.Call(WhistleStable.Outcome.OTHER_WORLD, null);
         }

         if (!REACHING.containsKey(id)) {
            BlockPos at = entry.getLastKnownPos();
            level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(at), 2, at);
            REACHING.put(id, new WhistleStable.Reach(player.getUUID(), level.dimension(), entry.getCustomName(), level.getGameTime() + 200L));
         }

         return new WhistleStable.Call(WhistleStable.Outcome.REACHING, null);
      } else {
         return new WhistleStable.Call(WhistleStable.Outcome.NOT_FOUND, null);
      }
   }

   private static void tick(MinecraftServer server) {
      if (!REACHING.isEmpty()) {
         for (Entry<UUID, WhistleStable.Reach> waiting : REACHING.entrySet()) {
            UUID id = waiting.getKey();
            WhistleStable.Reach reach = waiting.getValue();
            ServerLevel level = server.getLevel(reach.level());
            ServerPlayer player = server.getPlayerList().getPlayer(reach.player());
            if (level != null && player != null) {
               if (level.getEntity(id) instanceof DragonBase dragon) {
                  REACHING.remove(id);
                  DragonRegistryData.get(server).take(id);
                  WhistleStable.Call call = player.level() == level
                     ? callLoaded(dragon, player, level)
                     : new WhistleStable.Call(WhistleStable.Outcome.OTHER_WORLD, dragon);
                  tell(player, call, reach.name());
               } else if (level.getGameTime() > reach.until()) {
                  REACHING.remove(id);
                  tell(player, new WhistleStable.Call(WhistleStable.Outcome.LOST_TRAIL, null), reach.name());
               }
            } else {
               REACHING.remove(id);
            }
         }
      }
   }

   private static WhistleStable.Call callLoaded(DragonBase dragon, ServerPlayer player, ServerLevel level) {
      if (dragon.level() != level) {
         return new WhistleStable.Call(WhistleStable.Outcome.OTHER_WORLD, dragon);
      }

      if (dragon.isVehicle() || dragon.isPassenger()) {
         return new WhistleStable.Call(WhistleStable.Outcome.CARRYING, dragon);
      }

      if (dragon.getSyncedIsIncapacitated()) {
         return new WhistleStable.Call(WhistleStable.Outcome.DOWN, dragon);
      }

      double follow = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
      if (dragon.distanceToSqr(player) > follow * follow) {
         return relocate(dragon, player, level);
      }

      ((CommandComponent)dragon.componentRegistry.get(CommandComponent.class)).setCommandForced(Stance.FOLLOW, Modifier.TRAIL);
      return new WhistleStable.Call(WhistleStable.Outcome.CALLED, dragon);
   }

   private static WhistleStable.Call relocate(DragonBase dragon, ServerPlayer player, ServerLevel level) {
      GripComponent grip = dragon.grip();
      if (grip != null) {
         grip.release(Release.DROP);
      }

      CompoundTag body = new CompoundTag();
      if (dragon.getOwnerUUID() != null && dragon.save(body)) {
         DragonRegistryData.get(level.getServer()).store(new DragonRegistryData.Stored(dragon.getUUID(), dragon.getOwnerUUID(), DragonNames.of(dragon), body));
         dragon.remove(RemovalReason.CHANGED_DIMENSION);
         DragonBase out = release(player, level, dragon.getUUID());
         return new WhistleStable.Call(out == null ? WhistleStable.Outcome.FAILED : WhistleStable.Outcome.CALLED, out);
      } else {
         return new WhistleStable.Call(WhistleStable.Outcome.FAILED, dragon);
      }
   }

   @Nullable
   private static DragonBase release(ServerPlayer player, ServerLevel level, UUID id) {
      DragonRegistryData registry = DragonRegistryData.get(level.getServer());
      DragonRegistryData.Stored kept = registry.take(id);
      if (kept == null) {
         return null;
      }

      if (EntityType.loadEntityRecursive(kept.body(), level, entity -> entity) instanceof DragonBase dragon) {
         Vec3 at = behind(dragon, player, level);
         dragon.moveTo(at.x, at.y, at.z, player.getYRot(), 0.0F);
         dragon.setDeltaMovement(Vec3.ZERO);
         if (!level.tryAddFreshEntityWithPassengers(dragon)) {
            registry.store(kept);
            return null;
         } else {
            dragon.getLocomotionOrchestrator().requestTransition(LocomotionTransition.TAKEOFF, ModeChangeCause.WHISTLE);
            ((CommandComponent)dragon.componentRegistry.get(CommandComponent.class)).setCommandForced(Stance.FOLLOW, Modifier.TRAIL);
            return dragon;
         }
      } else {
         registry.store(kept);
         return null;
      }
   }

   private static Vec3 behind(DragonBase dragon, ServerPlayer player, ServerLevel level) {
      double follow = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
      double back = Math.max(follow - 2.0, 10.0);
      Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() < 1.0E-4) {
         look = new Vec3(0.0, 0.0, 1.0);
      }

      look = look.normalize();
      Vec3 target = new Vec3(player.getX() - look.x * back, player.getY(), player.getZ() - look.z * back);
      Vec3 ground = TerrainUtils.snapAndClear(level, target, dragon, 1.0);
      return new Vec3(ground.x, ground.y + 20.0, ground.z);
   }

   public static void tell(ServerPlayer player, WhistleStable.Call call, @Nullable String name) {
      String line = switch (call.outcome()) {
         case CALLED -> "summoning";
         case REACHING -> "reaching";
         case NOT_FOUND -> "out_of_reach";
         case LOST_TRAIL -> "lost_trail";
         case OTHER_WORLD -> "other_world";
         case CARRYING -> "carrying";
         case DOWN -> "down";
         case FAILED -> "dragon_not_found";
      };
      Component who = call.dragon() != null ? call.dragon().getName() : DragonNames.shown(name);
      boolean good = call.outcome() == WhistleStable.Outcome.CALLED || call.outcome() == WhistleStable.Outcome.REACHING;
      Strip.say(
         player,
         Component.translatable("item.bookofdragons.dragon_whistle." + line, new Object[]{who}).withStyle(good ? ChatFormatting.GREEN : ChatFormatting.YELLOW)
      );
   }

   @Nullable
   static DragonBase find(MinecraftServer server, UUID id) {
      for (ServerLevel level : server.getAllLevels()) {
         if (level.getEntity(id) instanceof DragonBase dragon) {
            return dragon;
         }
      }

      return null;
   }

   public record Call(WhistleStable.Outcome outcome, @Nullable DragonBase dragon) {
   }

   public enum Outcome {
      CALLED,
      REACHING,
      NOT_FOUND,
      LOST_TRAIL,
      OTHER_WORLD,
      CARRYING,
      DOWN,
      FAILED;
   }

   private record Reach(UUID player, ResourceKey<Level> level, String name, long until) {
   }
}

package net.magister.liberanimalium.body.nav.noise;

import com.mojang.serialization.Dynamic;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.IFaunaComponent;
import net.magister.liberanimalium.fauna.api.ContainerOpened;
import net.magister.liberanimalium.fauna.api.HeardSound;
import net.magister.liberanimalium.fauna.api.Kind;
import net.magister.liberanimalium.fauna.api.SoundKind;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.magister.liberanimalium.fauna.sense.SenseTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.gameevent.vibrations.VibrationInfo;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem.Data;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem.Listener;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem.User;
import net.minecraft.world.phys.Vec3;

public class FaunaNoiseListener implements IFaunaComponent, VibrationSystem {
   private FaunaBase host;
   private FaunaNoiseListener.Ears ears;
   private Data vibrationData = new Data();
   private DynamicGameEventListener<Listener> dynamicListener;

   @Override
   public void onAttach(FaunaBase host) {
      this.host = host;
      this.ears = new FaunaNoiseListener.Ears(host);
      this.dynamicListener = new DynamicGameEventListener(new Listener(this));
   }

   @Override
   public void serverTick() {
      if (this.host.level() instanceof ServerLevel level) {
         tickQuietly(level, this.vibrationData, this.ears);
      }
   }

   private static void tickQuietly(ServerLevel level, Data data, FaunaNoiseListener.Ears ears) {
      if (data.getCurrentVibration() == null) {
         data.getSelectionStrategy().chosenCandidate(level.getGameTime()).ifPresent(infox -> {
            data.setCurrentVibration(infox);
            data.setTravelTimeInTicks(ears.calculateTravelTimeInTicks(infox.distance()));
            data.getSelectionStrategy().startOver();
         });
      }

      VibrationInfo info = data.getCurrentVibration();
      if (info != null) {
         data.setReloadVibrationParticle(false);
         data.decrementTravelTime();
         if (data.getTravelTimeInTicks() <= 0) {
            BlockPos from = BlockPos.containing(info.pos());
            BlockPos at = ears.getPositionSource().getPosition(level).<BlockPos>map(BlockPos::containing).orElse(from);
            ears.onReceiveVibration(
               level,
               from,
               info.gameEvent(),
               (Entity)info.getEntity(level).orElse(null),
               (Entity)info.getProjectileOwner(level).orElse(null),
               Listener.distanceBetweenInBlocks(from, at)
            );
            data.setCurrentVibration(null);
         }
      }
   }

   @Override
   public void save(CompoundTag tag) {
      Data.CODEC.encodeStart(NbtOps.INSTANCE, this.vibrationData).resultOrPartial(e -> {}).ifPresent(encoded -> tag.put("noise_listener", encoded));
   }

   @Override
   public void load(CompoundTag tag) {
      if (tag.contains("noise_listener", 10)) {
         Data.CODEC
            .parse(new Dynamic<>(NbtOps.INSTANCE, tag.getCompound("noise_listener")))
            .resultOrPartial(e -> {})
            .ifPresent(data -> this.vibrationData = data);
      }
   }

   public Data getVibrationData() {
      return this.vibrationData;
   }

   public User getVibrationUser() {
      return this.ears;
   }

   public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> consumer) {
      if (this.host != null && this.host.level() instanceof ServerLevel level) {
         consumer.accept(this.dynamicListener, level);
      }
   }

   private static final class Ears implements User {
      private final FaunaBase body;
      private final PositionSource positionSource;

      Ears(FaunaBase body) {
         this.body = body;
         this.positionSource = new EntityPositionSource(body, body.getEyeHeight());
      }

      public int getListenerRadius() {
         float skill = this.body.species().senses().hearingSkill();
         return Math.max(1, Mth.ceil(SenseTuning.maxLoudness() * SenseTuning.earOf(skill)));
      }

      public int calculateTravelTimeInTicks(float distance) {
         return Math.max(1, Mth.ceil(distance / 17.0F));
      }

      public PositionSource getPositionSource() {
         return this.positionSource;
      }

      public TagKey<GameEvent> getListenableEvents() {
         return FaunaGameEvents.FAUNA_CAN_LISTEN;
      }

      private static boolean heardAs(SoundKind kind, @Nullable Entity source) {
         return kind != SoundKind.VOCAL || source instanceof FaunaBase;
      }

      public boolean isValidVibration(GameEvent event, Context context) {
         if (!NoiseKind.isRelevant(event)) {
            return false;
         } else {
            Entity source = context.sourceEntity();
            if (!heardAs(NoiseKind.classify(event), source)) {
               return false;
            } else if (source == null) {
               return true;
            } else if (source == this.body) {
               return false;
            } else if (source.isSpectator()) {
               return false;
            } else if (source instanceof Player player && player.isCreative()) {
               return false;
            } else {
               return source.dampensVibrations() ? false : event != GameEvent.STEP || !source.isShiftKeyDown();
            }
         }
      }

      public boolean canReceiveVibration(ServerLevel level, BlockPos pos, GameEvent event, Context ctx) {
         return !this.body.isNoAi() && !this.body.isDeadOrDying() && !this.body.isRemoved() && level.getWorldBorder().isWithinBounds(pos);
      }

      public void onReceiveVibration(ServerLevel level, BlockPos pos, GameEvent event, @Nullable Entity entity, @Nullable Entity projectileOwner, float distance) {
         if (!this.body.isDeadOrDying()) {
            FaunaMind mind = this.body.faunaMind();
            if (mind != null) {
               SoundKind kind = NoiseKind.classify(event);
               if (kind != null) {
                  Entity source = entity != null ? entity : projectileOwner;
                  Vec3 position = source != null ? source.position() : Vec3.atCenterOf(pos);
                  UUID sourceId = source != null ? source.getUUID() : null;
                  Kind made = source != null ? this.body.kindOf(source) : null;
                  boolean heard = this.body.hear(new HeardSound(kind, position, sourceId, mind.age(), distance, made));
                  if (heard && event == GameEvent.CONTAINER_OPEN && source instanceof Player) {
                     mind.perception().post(this.body, new ContainerOpened(sourceId, pos.immutable()));
                  }
               }
            }
         }
      }
   }
}

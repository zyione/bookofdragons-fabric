package net.magister.liberanimalium.sound.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.magister.liberanimalium.ride.client.CameraShakeUtil;
import net.magister.liberanimalium.sound.FaunaSoundHelper;
import net.magister.liberanimalium.sound.SoundConfig;
import net.magister.liberanimalium.sound.SoundEventKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class SoundClientHelper {
   private static final Map<Integer, SoundInstance> activeDiveLoops = new HashMap<>();
   private static final Map<Integer, SoundInstance> activeDiveStarts = new HashMap<>();
   private static final ChannelVoice ABILITY = new ChannelVoice();
   private static final ChannelVoice BREATH = new ChannelVoice();

   public static void playOneShotSound(FaunaBase entity, SoundEvent sound, float volume) {
      entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), sound, entity.getSoundSource(), volume, 1.0F, false);
   }

   public static SoundConfig diveVoice(FaunaBase entity, String name) {
      SoundConfig config = FaunaSoundHelper.voice(entity, name);
      return config != null && config.sound() != null ? config : null;
   }

   public static void playDiveStartSound(FaunaBase entity, SoundEvent sound, float volume) {
      stopDiveStartSound(entity);
      EntityBoundSoundInstance instance = new EntityBoundSoundInstance(sound, SoundSource.HOSTILE, volume, 1.0F, entity, entity.getRandom().nextLong());
      Minecraft.getInstance().getSoundManager().play(instance);
      activeDiveStarts.put(entity.getId(), instance);
   }

   public static void startDiveLoopSound(FaunaBase entity, SoundEvent sound, float volume, final Supplier<Float> pitchRamp) {
      if (!isDiveLoopPlaying(entity)) {
         EntityBoundSoundInstance instance = new EntityBoundSoundInstance(sound, SoundSource.HOSTILE, volume, 1.0F, entity, entity.getRandom().nextLong()) {
            public void tick() {
               super.tick();
               if (pitchRamp != null) {
                  this.pitch = pitchRamp.get();
               }
            }

            public boolean isLooping() {
               return true;
            }
         };
         Minecraft.getInstance().getSoundManager().play(instance);
         activeDiveLoops.put(entity.getId(), instance);
      }
   }

   public static void stopDiveLoopSound(FaunaBase entity) {
      SoundInstance instance = activeDiveLoops.remove(entity.getId());
      if (instance != null) {
         Minecraft.getInstance().getSoundManager().stop(instance);
      }
   }

   public static void stopDiveStartSound(FaunaBase entity) {
      SoundInstance instance = activeDiveStarts.remove(entity.getId());
      if (instance != null) {
         Minecraft.getInstance().getSoundManager().stop(instance);
      }
   }

   public static boolean isDiveLoopPlaying(FaunaBase entity) {
      SoundInstance instance = activeDiveLoops.get(entity.getId());
      return instance != null && Minecraft.getInstance().getSoundManager().isActive(instance);
   }

   public static void tickAbilityLoopSound(FaunaBase entity) {
      ABILITY.tick(entity, entity.abilityLoopActive(), "ability_start", "ability_loop", true, "ability_end");
   }

   public static void tickBreathSound(FaunaBase entity) {
      OmniAttackHandler breath = entity.componentRegistry.get(OmniAttackHandler.class);
      boolean on = breath != null && breath.isBreathing();
      if (FaunaSoundHelper.voice(entity, "breath_loop") != null) {
         BREATH.tick(entity, on, "breath_start", "breath_loop", true, "breath_end");
      } else {
         BREATH.tick(entity, on, null, SoundEventKey.SHOOT.voiceName(), false, null);
      }
   }

   public static void onBodyRemoved(FaunaBase entity) {
      ABILITY.remove(entity);
      BREATH.remove(entity);
   }

   public static void handleRoarCameraShake(FaunaBase body) {
      Player localPlayer = Minecraft.getInstance().player;
      if (localPlayer != null) {
         double distance = body.distanceTo(localPlayer);
         double range = 16.0;
         if (!(distance > range)) {
            if (PhysicsSheet.loaded()) {
               float baseIntensity = PhysicsSheet.of(body).weight(body.getWeightClass()).cameraShake();
               float attenuation = 1.0F - (float)(distance / range);
               float intensity = baseIntensity * attenuation;
               if (intensity > 0.0F) {
                  CameraShakeUtil.startShake(intensity);
               }
            }
         }
      }
   }
}

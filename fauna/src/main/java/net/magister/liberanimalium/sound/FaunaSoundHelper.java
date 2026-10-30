package net.magister.liberanimalium.sound;

import net.magister.liberanimalium.body.nav.noise.FaunaGameEvents;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class FaunaSoundHelper {
   private static final String IDLE = "idle";
   private static final String IDLE_SETTLED = "idle_settled";
   private static final String ASLEEP = "asleep";

   private FaunaSoundHelper() {
   }

   public static FaunaSoundTable table(FaunaBase body) {
      return FaunaSoundRegistry.of(body.animSpeciesId());
   }

   public static SoundConfig voice(FaunaBase body, String name) {
      return table(body).voice(name);
   }

   public static SoundConfig voice(FaunaBase body, SoundEventKey key) {
      return voice(body, key.voiceName());
   }

   public static String ambientVoice(FaunaBase body) {
      if (body.hasFaunaPose(PoseFlag.SLEEPING)) {
         return "asleep";
      } else {
         return body.isTame() && voice(body, "idle_settled") != null ? "idle_settled" : "idle";
      }
   }

   public static boolean isCall(String name) {
      return name.equals("idle") || name.equals("idle_settled") || name.equals(SoundEventKey.ROAR.voiceName());
   }

   public static void play(FaunaBase body, String name) {
      if (!body.level().isClientSide) {
         SoundConfig config = voice(body, name);
         if (config != null) {
            SoundEvent sound = config.sound();
            if (sound != null) {
               if (body.voiceGate().admit(name, body.tickCount)) {
                  if (config.range() > 0) {
                     playAtRange(body, sound, config);
                  } else {
                     body.playSound(sound, config.volume(), config.pitchFor(body.getRandom()));
                  }

                  if (isCall(name)) {
                     FaunaGameEvents.call(body);
                  }
               }
            }
         }
      }
   }

   private static void playAtRange(FaunaBase body, SoundEvent sound, SoundConfig config) {
      if (!body.isSilent() && body.level() instanceof ServerLevel level) {
         Holder var5 = Holder.direct(SoundEvent.createFixedRangeEvent(sound.getLocation(), config.range()));
         level.playSeededSound(
            null,
            body.getX(),
            body.getY(),
            body.getZ(),
            var5,
            body.getSoundSource(),
            config.volume(),
            config.pitchFor(body.getRandom()),
            body.getRandom().nextLong()
         );
      }
   }

   public static void playLocal(FaunaBase body, String name) {
      if (body.level().isClientSide) {
         if (body.voiceGate().admit(name, body.tickCount)) {
            playLocalUngated(body, name);
         }
      }
   }

   private static void playLocalUngated(FaunaBase body, String name) {
      SoundConfig config = voice(body, name);
      if (config != null) {
         SoundEvent sound = config.sound();
         if (sound != null) {
            body.level()
               .playLocalSound(body.getX(), body.getY(), body.getZ(), sound, body.getSoundSource(), config.volume(), config.pitchFor(body.getRandom()), false);
         }
      }
   }

   public static void playSound(FaunaBase body, SoundEventKey key) {
      play(body, key.voiceName());
   }

   public static void playFlapSound(FaunaBase body) {
      if (body.level().isClientSide) {
         playLocalUngated(body, SoundEventKey.FLAP.voiceName());
      }
   }

   public static void playIdleSound(FaunaBase body) {
      play(body, ambientVoice(body));
   }

   public static void playRoarSound(FaunaBase body) {
      if (voice(body, SoundEventKey.ROAR) != null) {
         playSound(body, SoundEventKey.ROAR);
      } else if (!body.level().isClientSide) {
         body.playSound(SoundEvents.RAVAGER_ROAR, 1.2F, 0.9F + body.getRandom().nextFloat() * 0.2F);
         FaunaGameEvents.call(body);
      }
   }

   public static SoundEvent getAmbientSound(FaunaBase body) {
      SoundConfig config = voice(body, ambientVoice(body));
      return config == null ? null : config.sound();
   }

   public static SoundEvent getHurtSound(FaunaBase body) {
      SoundConfig config = voice(body, SoundEventKey.HURT);
      return config == null ? null : config.sound();
   }
}

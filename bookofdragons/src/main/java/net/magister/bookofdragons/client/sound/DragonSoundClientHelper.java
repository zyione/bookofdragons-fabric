package net.magister.bookofdragons.client.sound;

import net.magister.bookofdragons.entity.custom.dragon.NightfuryEntity;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.magister.liberanimalium.sound.SoundConfig;
import net.magister.liberanimalium.sound.client.SoundClientHelper;
import net.minecraft.util.Mth;

public class DragonSoundClientHelper {
   public static void tickNightfuryDiveSounds(NightfuryEntity entity) {
      float throttle = entity.getThrottleLevel();
      OmniAttackHandler attackHandler = (OmniAttackHandler)entity.componentRegistry.get(OmniAttackHandler.class);
      boolean isCharging = attackHandler != null && attackHandler.isCharging();
      boolean whistling = entity.isFlying() && isCharging && throttle > 0.8F;
      int continuousDiveTicks = entity.getContinuousDiveTicks();
      int diveStartSoundTicks = entity.getDiveStartSoundTicks();
      if (whistling) {
         continuousDiveTicks++;
      } else {
         continuousDiveTicks = 0;
      }

      boolean shouldPlaySounds = continuousDiveTicks >= 20;
      boolean wasSoundActive = diveStartSoundTicks > 0 || SoundClientHelper.isDiveLoopPlaying(entity);
      if (shouldPlaySounds && diveStartSoundTicks == 0 && !SoundClientHelper.isDiveLoopPlaying(entity)) {
         SoundConfig voice = SoundClientHelper.diveVoice(entity, "dive_start");
         if (voice != null) {
            SoundClientHelper.playDiveStartSound(entity, voice.sound(), voice.volume());
         }

         diveStartSoundTicks = 1;
      }

      if (shouldPlaySounds && diveStartSoundTicks > 0) {
         if (++diveStartSoundTicks >= 60 && !SoundClientHelper.isDiveLoopPlaying(entity)) {
            SoundConfig voice = SoundClientHelper.diveVoice(entity, "dive_loop");
            if (voice != null) {
               SoundClientHelper.startDiveLoopSound(
                  entity, voice.sound(), voice.volume(), () -> Mth.clamp(1.0F + (entity.getDiveStartSoundTicks() - 60) * 0.01F, 1.0F, 1.8F)
               );
            }
         }
      }

      if (!whistling && wasSoundActive) {
         SoundClientHelper.stopDiveLoopSound(entity);
         SoundClientHelper.stopDiveStartSound(entity);
         SoundConfig voice = SoundClientHelper.diveVoice(entity, "dive_end");
         if (voice != null) {
            SoundClientHelper.playOneShotSound(entity, voice.sound(), voice.volume());
         }

         diveStartSoundTicks = 0;
      }

      entity.setContinuousDiveTicks(continuousDiveTicks);
      entity.setDiveStartSoundTicks(diveStartSoundTicks);
   }
}

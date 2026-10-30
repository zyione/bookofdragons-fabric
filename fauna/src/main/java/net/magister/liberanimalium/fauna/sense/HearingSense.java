package net.magister.liberanimalium.fauna.sense;

import net.magister.liberanimalium.fauna.api.HeardSound;
import net.magister.liberanimalium.fauna.api.SoundKind;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.minecraft.util.Mth;

public final class HearingSense {
   private HearingSense() {
   }

   public static float audibleRange(SpeciesProfile.Senses senses, double ownSpeed, HeardSound sound) {
      float loudness = SenseTuning.LOUDNESS.getOrDefault(sound.kind(), 0.0F);
      float moving = 1.0F - 0.6F * movement(ownSpeed);
      boolean carries = sound.kind() == SoundKind.CALL || sound.kind() == SoundKind.ROAR || sound.kind() == SoundKind.VOCAL;
      float kin = sound.sameSpecies() && !carries ? 0.1F : 1.0F;
      float skill = Math.min(senses.hearingSkill(), 2.0F);
      return loudness * skill * moving * kin;
   }

   public static boolean hears(SpeciesProfile.Senses senses, double ownSpeed, HeardSound sound) {
      return sound.distance() < audibleRange(senses, ownSpeed, sound);
   }

   public static float strength(SpeciesProfile.Senses senses, double ownSpeed, HeardSound sound) {
      float range = audibleRange(senses, ownSpeed, sound);
      return range <= 0.0F ? 0.0F : Mth.clamp(1.0F - sound.distance() / range, 0.0F, 1.0F);
   }

   private static float movement(double ownSpeed) {
      return Mth.clamp((float)(ownSpeed / 0.25), 0.0F, 1.0F);
   }
}

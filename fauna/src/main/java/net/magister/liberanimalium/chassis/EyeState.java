package net.magister.liberanimalium.chassis;

import net.magister.liberanimalium.body.anim.binding.AnimSignalSync;
import net.minecraft.util.Mth;

public final class EyeState {
   static final float EASE = 0.15F;
   private float mood;
   private float moodO;

   void tick(FaunaBase body) {
      this.moodO = this.mood;
      float target = AnimSignalSync.regardOf(body.getSyncedSocialSignalsPacked());
      this.mood = this.mood + (target - this.mood) * 0.15F;
   }

   public float mood(float partialTick) {
      return Mth.lerp(partialTick, this.moodO, this.mood);
   }
}

package net.magister.liberanimalium.fauna.know;

import net.minecraft.util.Mth;

public final class Compliance {
   private Compliance() {
   }

   public static float bondNorm(float like) {
      return Mth.clamp(like / 0.5F, 0.0F, 1.0F);
   }

   public static float equilibrium(float bondNorm, float steadiness, float sympathy, float ownerFind) {
      float raw = 0.15F + 0.6F * bondNorm + 0.15F * steadiness + 0.1F * sympathy * ownerFind;
      return Mth.clamp(raw, 0.05F, 0.98F);
   }

   public static float drift(float c, float equilibrium, float steadiness, int elapsed) {
      float rate = Mth.lerp(steadiness, 5.0E-4F, 0.003F);
      float value = c;

      for (int i = 0; i < elapsed; i++) {
         value += (equilibrium - value) * rate;
      }

      return value;
   }

   public static float cap(boolean estranged, float intensity) {
      if (!estranged) {
         return 0.98F;
      }

      float depth = Mth.clamp(intensity / 0.5F, 0.0F, 1.0F);
      return 0.98F - 0.93F * depth;
   }

   public static float share(float c) {
      return Mth.clamp((c - 0.05F) / 0.93F, 0.0F, 1.0F);
   }

   public static float afterEvent(float c, float delta) {
      return Mth.clamp(c + delta, 0.05F, 0.98F);
   }
}

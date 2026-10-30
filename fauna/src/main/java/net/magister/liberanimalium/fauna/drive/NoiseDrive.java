package net.magister.liberanimalium.fauna.drive;

import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Wants;
import net.minecraft.util.Mth;

public final class NoiseDrive implements Drive {
   public static final String ID = "noise";

   @Override
   public String id() {
      return "noise";
   }

   @Override
   public Wants.Want shows() {
      return Wants.Want.CURIOUS;
   }

   @Override
   public boolean occupies(DriveContext context) {
      return true;
   }

   @Override
   public float score(DriveContext context) {
      if (context.mysteryPoint() == null) {
         return 0.0F;
      }

      float meals = Math.min(context.mysteryCount(), context.mysteryMeals());
      float noises = context.mysteryCount() - meals + meals * Mth.clamp(context.reservoir("hunger"), 0.0F, 1.0F);
      if (noises <= 1.0F) {
         return 0.0F;
      }

      float pressure = (float)Math.sqrt(Mth.clamp((noises - 1.0F) / 19.0F, 0.0F, 1.0F));
      return pressure * 0.2F * context.trait(Trait.BRAVERY);
   }
}

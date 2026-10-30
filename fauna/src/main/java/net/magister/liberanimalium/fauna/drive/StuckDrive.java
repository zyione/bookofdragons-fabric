package net.magister.liberanimalium.fauna.drive;

import net.minecraft.util.Mth;

public final class StuckDrive implements Drive {
   public static final String ID = "stuck";

   @Override
   public String id() {
      return "stuck";
   }

   @Override
   public String store() {
      return null;
   }

   @Override
   public float score(DriveContext context) {
      float pressure = Mth.clamp(context.stuckPressure(), 0.0F, 1.0F);
      return pressure <= 0.0F ? 0.0F : BidShape.feltUrgent(pressure, 0.5F);
   }
}

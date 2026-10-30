package net.magister.liberanimalium.fauna.drive;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.know.Representation;
import net.minecraft.util.Mth;

final class Presses {
   private Presses() {
   }

   static float sum(DriveContext context, Presses.Press press) {
      float sum = 0.0F;

      for (Representation rep : context.known()) {
         sum += press.of(context, rep);
      }

      return Mth.clamp(sum, 0.0F, 1.0F);
   }

   @Nullable
   static Representation worst(DriveContext context, Presses.Press press) {
      Representation worst = null;
      float worstPress = 0.0F;

      for (Representation rep : context.known()) {
         float p = press.of(context, rep);
         if (p > worstPress) {
            worstPress = p;
            worst = rep;
         }
      }

      return worst;
   }

   @FunctionalInterface
   interface Press {
      float of(DriveContext var1, Representation var2);
   }
}

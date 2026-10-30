package net.magister.liberanimalium.body.nav.pathfinding;

import org.jetbrains.annotations.Nullable;

public final class VerticalScan {
   private VerticalScan() {
   }

   @Nullable
   public static Integer nearestStandableY(int x, int y, int z, int scanDown, int scanUp, VerticalScan.Standable standable, VerticalScan.Open open) {
      for (int dy = 0; dy >= -scanDown; dy--) {
         if (standable.at(x, y + dy, z)) {
            return y + dy;
         }

         if (!open.at(x, y + dy, z)) {
            break;
         }
      }

      for (int dy = 1; dy <= scanUp; dy++) {
         if (standable.at(x, y + dy, z)) {
            return y + dy;
         }
      }

      return null;
   }

   @FunctionalInterface
   public interface Open {
      boolean at(int var1, int var2, int var3);
   }

   @FunctionalInterface
   public interface Standable {
      boolean at(int var1, int var2, int var3);
   }
}

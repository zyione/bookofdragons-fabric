package net.magister.liberanimalium.fauna.know;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.Order;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Ties {
   private Ties() {
   }

   public static float pull(Representation rep, long now, Order order, @Nullable Vec3 home, Vec3 believed) {
      return order == Order.FREE && home != null && believed.distanceTo(home) > 48.0 ? 0.0F : Mth.clamp(1.0F - (float)rep.age(now) / 24000.0F, 0.0F, 1.0F);
   }

   public static float strayed(Vec3 point, @Nullable Vec3 home) {
      if (home == null) {
         return 0.0F;
      }

      double past = point.distanceTo(home) - 48.0;
      return past <= 0.0 ? 0.0F : (float)(past / 48.0) * 2.0F;
   }
}

package net.magister.liberanimalium.body;

import java.util.List;
import net.minecraft.world.phys.AABB;

public final class CullBox {
   public static final double MARGIN_PER_WIDTH = 3.0;
   public static final double MARGIN_BLOCKS = 3.0;

   private CullBox() {
   }

   public static AABB of(AABB body, List<AABB> parts, double bodyWidth) {
      double minX = body.minX;
      double minY = body.minY;
      double minZ = body.minZ;
      double maxX = body.maxX;
      double maxY = body.maxY;
      double maxZ = body.maxZ;

      for (AABB part : parts) {
         if (part.minX < minX) {
            minX = part.minX;
         }

         if (part.minY < minY) {
            minY = part.minY;
         }

         if (part.minZ < minZ) {
            minZ = part.minZ;
         }

         if (part.maxX > maxX) {
            maxX = part.maxX;
         }

         if (part.maxY > maxY) {
            maxY = part.maxY;
         }

         if (part.maxZ > maxZ) {
            maxZ = part.maxZ;
         }
      }

      double margin = margin(bodyWidth);
      return new AABB(minX - margin, minY - margin, minZ - margin, maxX + margin, maxY + margin, maxZ + margin);
   }

   public static double margin(double bodyWidth) {
      return bodyWidth * 3.0 + 3.0;
   }
}

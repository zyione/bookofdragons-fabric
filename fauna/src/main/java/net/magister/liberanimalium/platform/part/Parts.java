package net.magister.liberanimalium.platform.part;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.Entity;

public final class Parts {
   private Parts() {
   }

   public static boolean isPart(Entity entity) {
      return PartBody.isPart(entity);
   }

   @Nullable
   public static Entity parentOf(Entity entity) {
      return PartBody.isPart(entity) ? PartBody.parentOf(entity) : null;
   }

   public static boolean isMultipart(Entity entity) {
      return PartBody.isMultipart(entity);
   }

   @Nullable
   public static Entity[] partsOf(Entity entity) {
      return PartBody.partsOf(entity);
   }
}

package net.magister.bookofdragons.entity.data;

import org.jetbrains.annotations.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class DragonNames {
   private static final String KEY = "#";

   private DragonNames() {
   }

   public static String of(Entity entity) {
      return entity.hasCustomName() ? entity.getCustomName().getString() : of(entity.getType());
   }

   public static String of(EntityType<?> type) {
      return "#" + type.getDescriptionId();
   }

   public static Component shown(@Nullable String stored) {
      if (stored == null || stored.isEmpty()) {
         return Component.literal("?");
      } else {
         return stored.startsWith("#") ? Component.translatable(stored.substring("#".length())) : Component.literal(stored);
      }
   }
}

package net.magister.liberanimalium.compat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

public final class LegacyStacks {
   private static final Set<String> KEEP_ENTITY_TAG = ConcurrentHashMap.newKeySet();

   private LegacyStacks() {
   }

   public static void keepEntityTag(ResourceLocation item) {
      KEEP_ENTITY_TAG.add(item.toString());
   }

   public static boolean keepsEntityTag(String item) {
      return KEEP_ENTITY_TAG.contains(item);
   }
}

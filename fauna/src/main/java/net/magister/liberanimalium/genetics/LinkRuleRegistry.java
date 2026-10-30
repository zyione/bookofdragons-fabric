package net.magister.liberanimalium.genetics;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class LinkRuleRegistry {
   private static final Map<ResourceLocation, LinkRule> REGISTRY = new HashMap<>();

   private LinkRuleRegistry() {
   }

   public static void register(ResourceLocation id, LinkRule rule) {
      REGISTRY.put(id, rule);
   }

   public static LinkRule get(ResourceLocation id) {
      return REGISTRY.get(id);
   }

   public static void clear() {
      REGISTRY.clear();
   }

   public static int size() {
      return REGISTRY.size();
   }
}

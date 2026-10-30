package net.magister.liberanimalium.render.layer;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import mod.azure.azurelib.model.AzBakedModel;

public final class BoneNames {
   private static final Map<AzBakedModel, Map<String, Set<String>>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

   private BoneNames() {
   }

   public static Set<String> matching(AzBakedModel model, String key, Predicate<String> rule) {
      if (model == null) {
         return Set.of();
      }

      Map<String, Set<String>> perModel = CACHE.computeIfAbsent(model, m -> new ConcurrentHashMap<>());
      return perModel.computeIfAbsent(key, k -> {
         Set<String> found = new LinkedHashSet<>();

         for (String name : model.getBonesByName().keySet()) {
            if (rule.test(name)) {
               found.add(name);
            }
         }

         return Set.copyOf(found);
      });
   }

   public static void clear() {
      CACHE.clear();
   }

   public static int cachedModels() {
      return CACHE.size();
   }
}

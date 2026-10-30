package net.magister.liberanimalium.body;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class BabyArt {
   public static final String SUFFIX = "_baby";
   private static final Map<String, Boolean> SHIPPED = new ConcurrentHashMap<>();
   private static final Map<String, String> BINDING_KEYS = new ConcurrentHashMap<>();
   private static final Map<String, String> CLIP_KEYS = new ConcurrentHashMap<>();
   private static final Map<String, String> MODEL_KEYS = new ConcurrentHashMap<>();

   private BabyArt() {
   }

   public static boolean young(FaunaBase body) {
      int stage = body.getGrowthStage();
      return stage < 2;
   }

   public static String bindingKey(FaunaBase body) {
      return key(body, BINDING_KEYS, "animation_bindings/", ".json");
   }

   public static String clipKey(FaunaBase body) {
      return key(body, CLIP_KEYS, "animations/", ".animation.json");
   }

   public static String modelKey(FaunaBase body) {
      return key(body, MODEL_KEYS, "geo/", ".geo.json");
   }

   public static ResourceLocation plainTexture(FaunaBase body) {
      String species = body.speciesId();
      String adult = "textures/entity/" + species + "/" + species + "_default.png";
      if (!asksYoung(body)) {
         return SpeciesAssets.locate(adult);
      }

      String baby = "textures/entity/" + species + "/" + species + "_baby_default.png";
      return SpeciesAssets.locate(shipped(baby) ? baby : adult);
   }

   public static ResourceLocation layer(ResourceLocation layer) {
      ResourceLocation young = youngLayerOf(layer);
      return young != null && shippedAt(young) ? young : layer;
   }

   @Nullable
   public static ResourceLocation youngLayerOf(ResourceLocation layer) {
      String path = layer.getPath();
      String root = "textures/entity/genetics/";
      if (!path.startsWith(root)) {
         return null;
      } else {
         int slash = path.indexOf(47, root.length());
         if (slash >= 0 && !path.startsWith("/palettes/", slash) && !path.startsWith("/baby/", slash)) {
            String young = path.substring(0, slash) + "/baby" + path.substring(slash);
            return new ResourceLocation(layer.getNamespace(), young);
         } else {
            return null;
         }
      }
   }

   static void invalidate() {
      SHIPPED.clear();
      BINDING_KEYS.clear();
      CLIP_KEYS.clear();
      MODEL_KEYS.clear();
   }

   private static boolean asksYoung(FaunaBase body) {
      return body.level().isClientSide() && young(body);
   }

   private static String key(FaunaBase body, Map<String, String> keys, String dir, String extension) {
      String species = body.speciesId();
      if (!asksYoung(body)) {
         return species;
      }

      String known = keys.get(species);
      if (known != null) {
         return known;
      }

      String baby = species + "_baby";
      String key = shipped(dir + baby + extension) ? baby : species;
      keys.put(species, key);
      return key;
   }

   private static boolean shipped(String path) {
      return SHIPPED.computeIfAbsent(path, p -> SpeciesAssets.findClient(p).isPresent());
   }

   private static boolean shippedAt(ResourceLocation location) {
      return SHIPPED.computeIfAbsent(location.toString(), p -> SpeciesAssets.clientHas(location));
   }
}

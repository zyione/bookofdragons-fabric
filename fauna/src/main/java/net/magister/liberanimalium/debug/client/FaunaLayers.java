package net.magister.liberanimalium.debug.client;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum FaunaLayers {
   ROUTE("route"),
   RANGES("ranges"),
   RING("ring"),
   SIGHT("sight"),
   HEARING("hearing"),
   KNOWLEDGE("knowledge"),
   STIMULI("stimuli"),
   TERRITORY("territory"),
   NODES("nodes"),
   COMPANY("company"),
   MELEE("melee"),
   RIG("rig"),
   PARTS("parts"),
   BED("bed"),
   LABELS("labels");

   private final String id;
   private static final Set<FaunaLayers> ON = EnumSet.allOf(FaunaLayers.class);
   private static FaunaLayers soloed;

   FaunaLayers(String id) {
      this.id = id;
   }

   public String id() {
      return this.id;
   }

   public static boolean on(FaunaLayers layer) {
      return ON.contains(layer);
   }

   public static boolean drawing(FaunaLayers layer) {
      return FaunaDebugClient.enabled() && ON.contains(layer);
   }

   public static boolean toggle(FaunaLayers layer) {
      if (!ON.remove(layer)) {
         ON.add(layer);
         return true;
      } else {
         return false;
      }
   }

   public static void setAll(boolean on) {
      ON.clear();
      if (on) {
         ON.addAll(EnumSet.allOf(FaunaLayers.class));
      }

      soloed = null;
   }

   public static String cycleSolo() {
      FaunaLayers[] values = values();
      int next = soloed == null ? 0 : soloed.ordinal() + 1;
      if (next >= values.length - 1) {
         soloed = null;
         setAll(true);
         return "all layers";
      } else {
         soloed = values[next];
         ON.clear();
         ON.add(soloed);
         ON.add(LABELS);
         return soloed.id() + " only";
      }
   }

   public static FaunaLayers byId(String id) {
      for (FaunaLayers layer : values()) {
         if (layer.id().equalsIgnoreCase(id)) {
            return layer;
         }
      }

      return null;
   }

   public static String summary() {
      StringBuilder out = new StringBuilder();

      for (FaunaLayers layer : values()) {
         out.append(on(layer) ? "§a" : "§8").append(layer.id().toUpperCase(Locale.ROOT), 0, 2).append(' ');
      }

      return out.toString();
   }
}

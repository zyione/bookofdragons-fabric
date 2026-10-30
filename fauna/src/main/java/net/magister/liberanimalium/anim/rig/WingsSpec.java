package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonObject;

public record WingsSpec(String diveLayer) {
   public static final WingsSpec NONE = new WingsSpec(null);

   public static WingsSpec parse(JsonObject json) {
      if (json == null) {
         return NONE;
      }

      String layer = json.has("dive_layer") && !json.get("dive_layer").isJsonNull() ? json.get("dive_layer").getAsString() : null;
      return new WingsSpec(layer != null && !layer.isEmpty() ? layer : null);
   }
}

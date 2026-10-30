package net.magister.liberanimalium.anim.v2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record LayerSpec(String name, LayerSpec.Mode mode, String mask, int transition, String easing) {
   public LayerSpec {
      if (transition < 0) {
         transition = 0;
      }
   }

   public static LayerSpec parse(JsonObject json) {
      String name = json.get("name").getAsString();
      String modeWord = json.has("mode") ? json.get("mode").getAsString() : "override";
      LayerSpec.Mode mode = "additive".equalsIgnoreCase(modeWord)
         ? LayerSpec.Mode.ADDITIVE
         : ("weighted".equalsIgnoreCase(modeWord) ? LayerSpec.Mode.WEIGHTED : LayerSpec.Mode.OVERRIDE);
      String mask = json.has("mask") ? json.get("mask").getAsString() : null;
      int transition = json.has("transition") && json.get("transition").isJsonPrimitive() ? json.get("transition").getAsInt() : 0;
      String easing = json.has("easing") && !json.get("easing").isJsonNull() ? json.get("easing").getAsString() : null;
      return new LayerSpec(name, mode, mask, transition, easing);
   }

   public static String easingKey(String easing) {
      return easing == null ? null : easing.toLowerCase(Locale.ROOT).replace("_", "");
   }

   public static List<LayerSpec> parseAll(JsonArray array) {
      if (array == null) {
         return List.of();
      }

      List<LayerSpec> out = new ArrayList<>();

      for (JsonElement el : array) {
         if (el.isJsonObject() && el.getAsJsonObject().has("name")) {
            out.add(parse(el.getAsJsonObject()));
         }
      }

      return List.copyOf(out);
   }

   public enum Mode {
      OVERRIDE,
      ADDITIVE,
      WEIGHTED;
   }
}

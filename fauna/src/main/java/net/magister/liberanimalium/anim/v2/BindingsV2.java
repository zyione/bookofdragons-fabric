package net.magister.liberanimalium.anim.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

public record BindingsV2(List<LayerSpec> layers, Map<String, ControllerSpec> controllers) {
   public static final BindingsV2 EMPTY = new BindingsV2(List.of(), Map.of());

   public static BindingsV2 parse(JsonObject root, Predicate<String> isScalarKey) {
      if (root == null) {
         return EMPTY;
      }

      List<LayerSpec> layers = root.has("layers") && root.get("layers").isJsonArray() ? LayerSpec.parseAll(root.getAsJsonArray("layers")) : List.of();
      Map<String, ControllerSpec> controllers = new LinkedHashMap<>();
      if (root.has("controllers") && root.get("controllers").isJsonObject()) {
         for (Entry<String, JsonElement> entry : root.getAsJsonObject("controllers").entrySet()) {
            if (entry.getValue().isJsonObject()) {
               controllers.put(entry.getKey(), ControllerSpec.parse(entry.getKey(), entry.getValue().getAsJsonObject(), isScalarKey));
            }
         }
      }

      return new BindingsV2(layers, Map.copyOf(controllers));
   }

   public ControllerSpec controller(String layer) {
      return this.controllers.get(layer);
   }

   public LayerSpec layer(String name) {
      for (LayerSpec layer : this.layers) {
         if (layer.name().equals(name)) {
            return layer;
         }
      }

      return null;
   }

   public boolean isEmpty() {
      return this.layers.isEmpty() && this.controllers.isEmpty();
   }
}

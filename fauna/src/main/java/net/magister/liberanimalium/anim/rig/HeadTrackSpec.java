package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public record HeadTrackSpec(List<Float> distribution, float headReach) {
   public static final HeadTrackSpec DEFAULT = new HeadTrackSpec(List.of(0.25F, 0.3F, 0.35F, 0.4F, 0.45F, 0.5F));
   private static final List<Float> DEFAULT_WEIGHTS = List.of(0.25F, 0.3F, 0.35F, 0.4F, 0.45F, 0.5F);

   public HeadTrackSpec {
      distribution = distribution != null && !distribution.isEmpty() ? List.copyOf(distribution) : DEFAULT_WEIGHTS;
      headReach = Float.isFinite(headReach) ? Math.max(0.0F, Math.min(1.0F, headReach)) : 1.0F;
   }

   public HeadTrackSpec(List<Float> distribution) {
      this(distribution, 1.0F);
   }

   public float weight(int index) {
      return this.distribution.isEmpty() ? 0.0F : this.distribution.get(Math.min(index, this.distribution.size() - 1));
   }

   public int neckSlots() {
      return Math.max(0, this.distribution.size() - 1);
   }

   public static HeadTrackSpec parse(JsonObject json) {
      if (json == null) {
         return DEFAULT;
      }

      float reach = json.has("head_reach") && json.get("head_reach").isJsonPrimitive() ? json.get("head_reach").getAsFloat() : 1.0F;
      List<Float> weights = new ArrayList<>();
      if (json.has("distribution") && json.get("distribution").isJsonArray()) {
         for (JsonElement element : json.getAsJsonArray("distribution")) {
            if (element.isJsonPrimitive()) {
               weights.add(element.getAsFloat());
            }
         }
      }

      return weights.isEmpty() && reach == 1.0F ? DEFAULT : new HeadTrackSpec(weights, reach);
   }
}

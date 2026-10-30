package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public record SizeSpec(List<Float> stages, float multiplier) {
   public static final SizeSpec DEFAULT = new SizeSpec(List.of(0.4F, 0.75F, 1.0F, 1.25F), 1.0F);

   public SizeSpec {
      stages = stages != null && !stages.isEmpty() ? List.copyOf(stages) : List.of(0.4F, 0.75F, 1.0F, 1.25F);
   }

   public float scaleFor(int stage) {
      float base = stage >= 0 && stage < this.stages.size() ? this.stages.get(stage) : 1.0F;
      return base * this.multiplier;
   }

   public static SizeSpec parse(JsonObject json) {
      if (json == null) {
         return DEFAULT;
      }

      List<Float> stages = new ArrayList<>();
      if (json.has("stages") && json.get("stages").isJsonArray()) {
         for (JsonElement element : json.getAsJsonArray("stages")) {
            if (element.isJsonPrimitive()) {
               stages.add(element.getAsFloat());
            }
         }
      }

      float multiplier = json.has("multiplier") && json.get("multiplier").isJsonPrimitive() ? json.get("multiplier").getAsFloat() : DEFAULT.multiplier;
      return new SizeSpec(stages.isEmpty() ? DEFAULT.stages : stages, multiplier);
   }
}

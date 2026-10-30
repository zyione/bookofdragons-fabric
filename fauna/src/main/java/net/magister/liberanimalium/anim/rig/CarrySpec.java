package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.phys.Vec3;

public record CarrySpec(String bone, Vec3 offset, Vec3 rotation, float scale) {
   public static final CarrySpec DEFAULT = new CarrySpec(null, new Vec3(0.0, -1.92, 7.2), Vec3.ZERO, 1.0F);

   public static CarrySpec parse(JsonObject json) {
      if (json == null) {
         return DEFAULT;
      }

      String bone = json.has("bone") && json.get("bone").isJsonPrimitive() ? json.get("bone").getAsString() : null;
      Vec3 offset = vec(json, "offset", DEFAULT.offset);
      Vec3 rotation = vec(json, "rotation", DEFAULT.rotation);
      float scale = json.has("scale") && json.get("scale").isJsonPrimitive() ? json.get("scale").getAsFloat() : DEFAULT.scale;
      return new CarrySpec(bone, offset, rotation, Math.max(0.01F, scale));
   }

   private static Vec3 vec(JsonObject json, String key, Vec3 fallback) {
      if (json.has(key) && json.get(key).isJsonArray()) {
         JsonArray array = json.getAsJsonArray(key);
         if (array.size() != 3) {
            return fallback;
         }

         double[] out = new double[3];

         for (int i = 0; i < 3; i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonPrimitive()) {
               return fallback;
            }

            out[i] = element.getAsDouble();
         }

         return new Vec3(out[0], out[1], out[2]);
      } else {
         return fallback;
      }
   }
}

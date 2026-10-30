package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.GsonHelper;

public record EyesSpec(List<String> pupils, float range, float travel, float[] narrow, float[] wide) {
   public static final EyesSpec EMPTY = new EyesSpec(List.of(), 30.0F, 1.0F, new float[]{1.0F, 1.0F}, new float[]{1.0F, 1.0F});

   public boolean isEmpty() {
      return this.pupils.isEmpty();
   }

   public static EyesSpec parse(JsonObject json) {
      if (json == null) {
         return EMPTY;
      }

      List<String> pupils = new ArrayList<>();
      if (json.has("pupils") && json.get("pupils").isJsonArray()) {
         for (JsonElement e : json.getAsJsonArray("pupils")) {
            pupils.add(e.getAsString());
         }
      }

      return new EyesSpec(
         List.copyOf(pupils),
         GsonHelper.getAsFloat(json, "range", 30.0F),
         GsonHelper.getAsFloat(json, "travel", 1.0F),
         pair(json, "narrow", 0.3F, 1.0F),
         pair(json, "wide", 2.0F, 1.15F)
      );
   }

   private static float[] pair(JsonObject json, String key, float width, float length) {
      if (json.has(key) && json.get(key).isJsonArray()) {
         JsonArray a = json.getAsJsonArray(key);
         return new float[]{a.get(0).getAsFloat(), a.size() > 1 ? a.get(1).getAsFloat() : length};
      } else {
         return new float[]{width, length};
      }
   }
}

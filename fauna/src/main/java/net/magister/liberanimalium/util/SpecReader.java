package net.magister.liberanimalium.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class SpecReader {
   private SpecReader() {
   }

   public static double num(JsonObject json, String key, double fallback) {
      return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsDouble() : fallback;
   }

   public static float numF(JsonObject json, String key, float fallback) {
      return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsFloat() : fallback;
   }

   public static boolean flag(JsonObject json, String key, boolean fallback) {
      return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsBoolean() : fallback;
   }

   @Nullable
   public static String string(JsonObject json, String key) {
      return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : null;
   }

   @Nullable
   public static JsonObject object(JsonObject json, String key) {
      return json.has(key) && json.get(key).isJsonObject() ? json.getAsJsonObject(key) : null;
   }

   public static List<String> strings(JsonObject json, String key) {
      if (json.has(key) && json.get(key).isJsonArray()) {
         List<String> out = new ArrayList<>();

         for (JsonElement element : json.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) {
               out.add(element.getAsString());
            }
         }

         return List.copyOf(out);
      } else {
         return List.of();
      }
   }
}

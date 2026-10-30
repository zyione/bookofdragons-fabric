package net.magister.liberanimalium.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public record FaunaSoundTable(Map<String, SoundConfig> voices) {
   public static final FaunaSoundTable EMPTY = new FaunaSoundTable(Map.of());

   public static FaunaSoundTable parse(JsonObject root) {
      if (root != null && root.has("sounds") && root.get("sounds").isJsonObject()) {
         Map<String, SoundConfig> voices = new LinkedHashMap<>();

         for (Entry<String, JsonElement> entry : root.getAsJsonObject("sounds").entrySet()) {
            if (entry.getValue().isJsonNull()) {
               voices.put(entry.getKey().toLowerCase(Locale.ROOT), SoundConfig.SILENT);
            } else if (entry.getValue().isJsonObject()) {
               JsonObject body = entry.getValue().getAsJsonObject();
               if (body.has("sound")) {
                  voices.put(entry.getKey().toLowerCase(Locale.ROOT), SoundConfig.parse(body));
               }
            }
         }

         return new FaunaSoundTable(Map.copyOf(voices));
      } else {
         return EMPTY;
      }
   }

   public SoundConfig voice(String name) {
      return name == null ? null : this.voices.get(name.toLowerCase(Locale.ROOT));
   }

   public Set<String> names() {
      return this.voices.keySet();
   }
}

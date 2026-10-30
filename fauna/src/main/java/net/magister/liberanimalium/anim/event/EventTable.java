package net.magister.liberanimalium.anim.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public record EventTable(Map<String, EventRow> rows) {
   public static final EventTable EMPTY = new EventTable(Map.of());

   public static EventTable parse(JsonObject json) {
      if (json != null && json.size() != 0) {
         Map<String, EventRow> rows = new LinkedHashMap<>();

         for (Entry<String, JsonElement> entry : json.entrySet()) {
            if (entry.getValue().isJsonObject()) {
               rows.put(entry.getKey(), EventRow.parse(entry.getValue().getAsJsonObject()));
            }
         }

         return new EventTable(Map.copyOf(rows));
      } else {
         return EMPTY;
      }
   }

   public EventRow row(String key) {
      return this.rows.get(key);
   }

   public Set<String> keys() {
      return this.rows.keySet();
   }

   public Set<String> bonesNamed() {
      Set<String> out = new LinkedHashSet<>();

      for (EventRow row : this.rows.values()) {
         out.addAll(row.bones());
      }

      return out;
   }
}

package net.magister.liberanimalium.anim.mask;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public record BoneMask(String name, List<String> patterns, List<String> excludes) {
   public static final BoneMask ALL = new BoneMask("*", List.of(), List.of());

   public BoneMask(String name, List<String> patterns) {
      this(name, patterns, List.of());
   }

   public boolean allows(String boneName) {
      String lower = boneName.toLowerCase(Locale.ROOT);

      for (String pattern : this.excludes) {
         if (matches(lower, pattern)) {
            return false;
         }
      }

      if (this.patterns.isEmpty()) {
         return true;
      }

      for (String pattern : this.patterns) {
         if (matches(lower, pattern)) {
            return true;
         }
      }

      return false;
   }

   public boolean filters() {
      return !this.patterns.isEmpty() || !this.excludes.isEmpty();
   }

   private static boolean matches(String name, String pattern) {
      int star = pattern.indexOf(42);
      if (star < 0) {
         return name.equals(pattern);
      }

      if (pattern.equals("*")) {
         return true;
      }

      String[] parts = pattern.split("\\*", -1);
      int at = 0;

      for (int i = 0; i < parts.length; i++) {
         String part = parts[i];
         if (!part.isEmpty()) {
            if (i == 0) {
               if (!name.startsWith(part)) {
                  return false;
               }

               at = part.length();
            } else {
               int found = name.indexOf(part, at);
               if (found < 0) {
                  return false;
               }

               at = found + part.length();
            }
         }
      }

      String last = parts[parts.length - 1];
      return last.isEmpty() || name.endsWith(last);
   }

   public static Map<String, BoneMask> parseGroups(JsonObject json) {
      if (json == null) {
         return Map.of();
      }

      Map<String, BoneMask> out = new LinkedHashMap<>();

      for (Entry<String, JsonElement> entry : json.entrySet()) {
         if (entry.getValue().isJsonArray()) {
            out.put(entry.getKey(), new BoneMask(entry.getKey(), globs(entry.getValue().getAsJsonArray()), List.of()));
         } else if (entry.getValue().isJsonObject()) {
            JsonObject body = entry.getValue().getAsJsonObject();
            out.put(
               entry.getKey(),
               new BoneMask(
                  entry.getKey(),
                  body.has("allow") ? globs(body.getAsJsonArray("allow")) : List.of(),
                  body.has("exclude") ? globs(body.getAsJsonArray("exclude")) : List.of()
               )
            );
         }
      }

      return Map.copyOf(out);
   }

   private static List<String> globs(JsonArray array) {
      List<String> out = new ArrayList<>(array.size());

      for (JsonElement el : array) {
         out.add(el.getAsString().toLowerCase(Locale.ROOT));
      }

      return List.copyOf(out);
   }
}

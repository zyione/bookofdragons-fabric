package net.magister.liberanimalium.fauna.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceLocation;

public final class TamingMatrix {
   public static final TamingMatrix EMPTY = new TamingMatrix(Map.of());
   public static final String ACT_GROOM = "groom";
   public static final String ACT_LIVE_PREY = "live_prey";
   private final Map<String, Valence> rows;

   private TamingMatrix(Map<String, Valence> rows) {
      this.rows = Map.copyOf(rows);
   }

   public boolean isEmpty() {
      return this.rows.isEmpty();
   }

   public Map<String, Valence> rows() {
      return this.rows;
   }

   public Valence item(@Nullable ResourceLocation id, Predicate<ResourceLocation> isInTag, Valence fallback) {
      Valence row = this.row(id, isInTag);
      return row == null ? fallback : row;
   }

   @Nullable
   public Valence row(@Nullable ResourceLocation id, Predicate<ResourceLocation> isInTag) {
      if (id != null) {
         Valence exact = this.rows.get("item:" + id);
         if (exact != null) {
            return exact;
         }
      }

      for (Entry<String, Valence> row : this.rows.entrySet()) {
         if (row.getKey().startsWith("item:#")) {
            ResourceLocation tag = ResourceLocation.tryParse(row.getKey().substring(6));
            if (tag != null && isInTag.test(tag)) {
               return row.getValue();
            }
         }
      }

      return null;
   }

   @Nullable
   public Valence act(String family) {
      return this.rows.get("act:" + family.toLowerCase(Locale.ROOT));
   }

   public static TamingMatrix parse(@Nullable JsonObject matrix) {
      if (matrix == null) {
         return EMPTY;
      }

      Map<String, Valence> rows = new LinkedHashMap<>();

      for (Entry<String, JsonElement> e : matrix.entrySet()) {
         if (e.getValue().isJsonPrimitive()) {
            rows.put(e.getKey().toLowerCase(Locale.ROOT), Valence.parse(e.getValue().getAsString()));
         }
      }

      return rows.isEmpty() ? EMPTY : new TamingMatrix(rows);
   }

   public static TamingMatrix of(Map<String, Valence> rows) {
      return rows.isEmpty() ? EMPTY : new TamingMatrix(rows);
   }
}

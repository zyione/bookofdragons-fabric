package net.magister.liberanimalium.body.gameplay;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.data.FaunaWeightClass;

public final class SpeciesGameplayRegistry {
   private static volatile Map<String, SpeciesGameplayProfile> profiles = Map.of();
   private static volatile Map<String, JsonObject> files = Map.of();

   private SpeciesGameplayRegistry() {
   }

   public static void installAll(Map<String, SpeciesGameplayProfile> next, Map<String, JsonObject> raw) {
      Map<String, SpeciesGameplayProfile> keyed = new HashMap<>();
      next.forEach((id, profile) -> keyed.put(id.toLowerCase(), profile));
      Map<String, JsonObject> rawKeyed = new HashMap<>();
      raw.forEach((id, json) -> rawKeyed.put(id.toLowerCase(), json.deepCopy()));
      profiles = Map.copyOf(keyed);
      files = Map.copyOf(rawKeyed);
      VariantGameplay.invalidate();
   }

   @Nullable
   public static JsonObject file(String speciesId) {
      JsonObject file = files.get(speciesId.toLowerCase());
      return file == null ? null : file.deepCopy();
   }

   public static FaunaWeightClass weightOf(String speciesId) {
      SpeciesGameplayProfile profile = getProfile(speciesId);
      return profile != null && profile.weightClass() != null ? profile.weightClass() : SpeciesGameplayProfile.DEFAULT_WEIGHT;
   }

   @Nullable
   public static String speciesClassOf(String speciesId) {
      SpeciesGameplayProfile profile = getProfile(speciesId);
      return profile == null ? null : profile.speciesClass();
   }

   public static SpeciesGameplayProfile getProfile(String speciesId) {
      return profiles.get(speciesId.toLowerCase());
   }

   public static Map<String, SpeciesGameplayProfile> getAll() {
      return profiles;
   }

   public static int size() {
      return profiles.size();
   }
}

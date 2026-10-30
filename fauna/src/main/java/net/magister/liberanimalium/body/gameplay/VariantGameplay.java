package net.magister.liberanimalium.body.gameplay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.variant.CreatureVariant;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VariantGameplay {
   private static final Logger LOGGER = LoggerFactory.getLogger("VariantGameplay");
   private static final Map<ResourceLocation, Optional<SpeciesGameplayProfile>> MERGED = new ConcurrentHashMap<>();

   private VariantGameplay() {
   }

   @Nullable
   public static SpeciesGameplayProfile of(CreatureVariant variant, String speciesId) {
      SpeciesGameplayProfile own = SpeciesGameplayRegistry.getProfile(speciesId);
      return !variant.overridesGameplay() ? own : MERGED.computeIfAbsent(variant.id(), id -> merge(variant, speciesId)).orElse(own);
   }

   public static void invalidate() {
      MERGED.clear();
   }

   private static Optional<SpeciesGameplayProfile> merge(CreatureVariant variant, String speciesId) {
      JsonObject file = SpeciesGameplayRegistry.file(speciesId);
      if (file == null) {
         LOGGER.warn("[Variants] {}: species '{}' has no gameplay file to change; its own profile stands", variant.id(), speciesId);
         return Optional.empty();
      }

      overlay(file, "identity", variant.identity());
      overlay(file, "body", variant.body());

      try {
         return Optional.of(SpeciesGameplayProfile.parse(file));
      } catch (RuntimeException e) {
         LOGGER.error("[Variants] {}: its identity or body does not read over species '{}': {}", new Object[]{variant.id(), speciesId, e.getMessage()});
         return Optional.empty();
      }
   }

   static void overlay(JsonObject file, String block, @Nullable JsonObject own) {
      if (own != null) {
         JsonObject target = file.has(block) && file.get(block).isJsonObject() ? file.getAsJsonObject(block) : new JsonObject();

         for (Entry<String, JsonElement> key : own.entrySet()) {
            target.add(key.getKey(), key.getValue().deepCopy());
         }

         file.add(block, target);
      }
   }
}

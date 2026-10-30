package net.magister.liberanimalium.body.gameplay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.data.FaunaBodyPlan;
import net.magister.liberanimalium.body.data.FaunaWeightClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public record SpeciesGameplayProfile(
   @Nullable String speciesClass,
   @Nullable FaunaWeightClass weightClass,
   @Nullable FaunaBodyPlan plan,
   int breathTicks,
   ResourceLocation attackId,
   Set<ResourceLocation> favoriteFoods,
   Set<ResourceLocation> specialFoods,
   Set<ResourceLocation> generalFoods,
   Set<ResourceLocation> hoardItems,
   @Nullable SpeciesGameplayProfile.SpeciesChannel channel,
   List<ResourceLocation> melee,
   Set<ResourceLocation> meleeReserved,
   float scale,
   Set<ResourceLocation> dislikedFoods
) {
   public static final int DEFAULT_CHANNEL_COOLDOWN = 200;
   public static final int DEFAULT_CHANNEL_POINTS = 40;
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final FaunaWeightClass DEFAULT_WEIGHT = FaunaWeightClass.MEDIUM;

   public SpeciesGameplayProfile(
      @Nullable String speciesClass,
      @Nullable FaunaWeightClass weightClass,
      @Nullable FaunaBodyPlan plan,
      int breathTicks,
      ResourceLocation attackId,
      Set<ResourceLocation> favoriteFoods,
      Set<ResourceLocation> specialFoods,
      Set<ResourceLocation> generalFoods,
      Set<ResourceLocation> hoardItems,
      SpeciesGameplayProfile.SpeciesChannel channel,
      List<ResourceLocation> melee,
      Set<ResourceLocation> meleeReserved,
      float scale
   ) {
      this(
         speciesClass,
         weightClass,
         plan,
         breathTicks,
         attackId,
         favoriteFoods,
         specialFoods,
         generalFoods,
         hoardItems,
         channel,
         melee,
         meleeReserved,
         scale,
         Set.of()
      );
   }

   public static boolean isSpeciesFile(JsonObject root) {
      return root.has("body") || root.has("identity");
   }

   public static Map<String, SpeciesGameplayProfile> parseAll(Map<String, JsonObject> files) {
      Map<String, SpeciesGameplayProfile> profiles = new HashMap<>();

      for (Entry<String, JsonObject> file : files.entrySet()) {
         if (!file.getKey().startsWith("_") && isSpeciesFile(file.getValue())) {
            try {
               profiles.put(file.getKey(), parse(file.getValue()));
            } catch (Exception e) {
               LOGGER.error("[SpeciesGameplay] Failed to load the profile for {}: {}", new Object[]{file.getKey(), e.getMessage(), e});
            }
         }
      }

      return profiles;
   }

   public static SpeciesGameplayProfile parse(JsonObject root) {
      JsonObject identity = GsonHelper.getAsJsonObject(root, "identity", new JsonObject());
      String speciesClass = identity.has("class") ? GsonHelper.getAsString(identity, "class") : null;
      ResourceLocation attackId = identity.has("attack_id") ? ResourceLocation.tryParse(GsonHelper.getAsString(identity, "attack_id")) : null;
      Set<ResourceLocation> favoriteFoods = parseFoods(identity, "favorite_food");
      Set<ResourceLocation> specialFoods = parseFoods(identity, "special_food");
      Set<ResourceLocation> generalFoods = parseFoods(identity, "general_food");
      Set<ResourceLocation> hoardItems = parseFoods(identity, "hoard_item");
      List<ResourceLocation> melee = parseIds(identity, "melee");
      Set<ResourceLocation> meleeReserved = Set.copyOf(parseIds(identity, "melee_reserved"));

      for (ResourceLocation held : meleeReserved) {
         if (!melee.contains(held)) {
            throw new IllegalArgumentException("identity.melee_reserved names " + held + ", which identity.melee does not list");
         }
      }

      JsonObject body = GsonHelper.getAsJsonObject(root, "body", new JsonObject());
      FaunaWeightClass weightClass = body.has("weight") ? FaunaWeightClass.valueOf(GsonHelper.getAsString(body, "weight")) : null;
      FaunaBodyPlan plan = body.has("plan") ? FaunaBodyPlan.valueOf(GsonHelper.getAsString(body, "plan")) : null;
      int breathTicks = Math.max(0, GsonHelper.getAsInt(body, "breath_ticks", 0));
      float scale = GsonHelper.getAsFloat(body, "scale", 1.0F);
      if (!(scale > 0.0F)) {
         throw new IllegalArgumentException("body.scale must be above 0, got " + scale);
      }

      SpeciesGameplayProfile.SpeciesChannel channel = root.has("channel") ? parseChannel(GsonHelper.getAsJsonObject(root, "channel")) : null;
      return new SpeciesGameplayProfile(
         speciesClass,
         weightClass,
         plan,
         breathTicks,
         attackId,
         favoriteFoods,
         specialFoods,
         generalFoods,
         hoardItems,
         channel,
         melee,
         meleeReserved,
         scale,
         parseFoods(identity, "disliked_food")
      );
   }

   @Nullable
   private static SpeciesGameplayProfile.SpeciesChannel parseChannel(JsonObject json) {
      TamingPredicate match = json.has("match") ? TamingPredicate.parse(GsonHelper.getAsJsonObject(json, "match")) : null;
      return match == null
         ? null
         : new SpeciesGameplayProfile.SpeciesChannel(
            GsonHelper.getAsString(json, "id", "species_channel"), match, GsonHelper.getAsInt(json, "points", 40), GsonHelper.getAsInt(json, "cooldown", 200)
         );
   }

   private static List<ResourceLocation> parseIds(JsonObject identity, String key) {
      List<ResourceLocation> ids = new ArrayList<>();
      if (!identity.has(key)) {
         return List.of();
      }

      for (JsonElement el : GsonHelper.getAsJsonArray(identity, key)) {
         ResourceLocation id = ResourceLocation.tryParse(el.getAsString());
         if (id == null) {
            throw new IllegalArgumentException("identity." + key + ": not an id: " + el);
         }

         ids.add(id);
      }

      return List.copyOf(ids);
   }

   private static Set<ResourceLocation> parseFoods(JsonObject identity, String key) {
      Set<ResourceLocation> foods = new HashSet<>();
      if (identity.has(key)) {
         for (JsonElement el : GsonHelper.getAsJsonArray(identity, key)) {
            ResourceLocation rl = ResourceLocation.tryParse(el.getAsString());
            if (rl != null) {
               foods.add(rl);
            }
         }
      }

      return foods;
   }

   public record SpeciesChannel(String id, TamingPredicate match, int points, int cooldown) {
   }
}

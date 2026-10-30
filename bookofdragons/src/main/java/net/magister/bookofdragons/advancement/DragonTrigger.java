package net.magister.bookofdragons.advancement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayProfile;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayRegistry;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DragonTrigger extends SimpleCriterionTrigger<DragonTrigger.TriggerInstance> {
   private static final String ANY = "any";
   private static final ResourceLocation ID = new ResourceLocation("bookofdragons", "dragon");

   @NotNull
   public ResourceLocation getId() {
      return ID;
   }

   @NotNull
   protected DragonTrigger.TriggerInstance createInstance(
      @NotNull JsonObject json, @NotNull ContextAwarePredicate player, @NotNull DeserializationContext context
   ) {
      List<String> foods = new ArrayList<>();
      if (json.has("favorite_food")) {
         for (JsonElement food : GsonHelper.getAsJsonArray(json, "favorite_food")) {
            foods.add(food.getAsString());
         }
      }

      return new DragonTrigger.TriggerInstance(
         player,
         GsonHelper.getAsString(json, "event"),
         GsonHelper.getAsString(json, "dragon_type", "any"),
         GsonHelper.getAsString(json, "dragon_class", "any"),
         List.copyOf(foods)
      );
   }

   public void trigger(ServerPlayer player, String event, @Nullable String species, @Nullable String dragonClass) {
      Set<String> loves = new HashSet<>();
      SpeciesGameplayProfile profile = species == null ? null : SpeciesGameplayRegistry.getProfile(species);
      if (profile != null) {
         profile.favoriteFoods().forEach(food -> loves.add(food.toString()));
      }

      this.trigger(player, instance -> instance.matches(event, species, dragonClass, loves));
   }

   private static boolean matches(
      String wantEvent,
      String wantType,
      String wantClass,
      List<String> wantFood,
      String event,
      @Nullable String species,
      @Nullable String dragonClass,
      Set<String> loves
   ) {
      if (!wantEvent.equals(event)) {
         return false;
      } else if (!wantType.equals("any") && !wantType.equalsIgnoreCase(species)) {
         return false;
      } else {
         return !wantFood.isEmpty() && wantFood.stream().noneMatch(loves::contains)
            ? false
            : wantClass.equals("any") || wantClass.equalsIgnoreCase(dragonClass);
      }
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final String event;
      private final String dragonType;
      private final String dragonClass;
      private final List<String> favoriteFood;

      public TriggerInstance(ContextAwarePredicate player, String event, String dragonType, String dragonClass, List<String> favoriteFood) {
         super(DragonTrigger.ID, player);
         this.event = event;
         this.dragonType = dragonType;
         this.dragonClass = dragonClass;
         this.favoriteFood = favoriteFood;
      }

      public boolean matches(String event, @Nullable String species, @Nullable String dragonClass, Set<String> loves) {
         return DragonTrigger.matches(this.event, this.dragonType, this.dragonClass, this.favoriteFood, event, species, dragonClass, loves);
      }
   }
}

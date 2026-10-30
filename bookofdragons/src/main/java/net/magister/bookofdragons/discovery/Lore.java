package net.magister.bookofdragons.discovery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.datagen.data.DragonDiscoveryData;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayProfile;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayRegistry;
import net.magister.liberanimalium.fauna.api.Check;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.api.SpeciesProfiles;
import net.magister.liberanimalium.fauna.api.Ladder.StageDef;
import net.magister.liberanimalium.fauna.api.SpeciesProfile.RelationRule;
import net.minecraft.resources.ResourceLocation;

public final class Lore {
   public static final String ASK = "ask:";
   public static final String FOOD = "food:";
   public static final String DISLIKE = "dislike:";
   public static final String GIFT = "gift:";
   public static final String PHASE = "phase:";
   public static final String SEEN = "seen:";
   public static final String PREY = "prey";
   public static final String FEAR = "fear";
   public static final String FOE = "foe";
   public static final float FULL = 0.75F;
   private static final Set<String> PLAIN_STEPS = Set.of("approach", "meal");
   private static final List<String> PHASES = List.of("tolerates", "accepted", "familiar");

   private Lore() {
   }

   public static String step(Check check) {
      String id = check.id();
      return id.substring(id.indexOf(58) + 1);
   }

   public static List<ResourceLocation> sorted(Set<ResourceLocation> ids) {
      List<ResourceLocation> out = new ArrayList<>(ids);
      out.sort(null);
      return out;
   }

   public static List<ResourceLocation> foods(SpeciesGameplayProfile g) {
      LinkedHashSet<ResourceLocation> out = new LinkedHashSet<>(sorted(g.favoriteFoods()));
      out.addAll(sorted(g.specialFoods()));
      return new ArrayList<>(out);
   }

   public static Set<String> all(String species) {
      List<String> steps = new ArrayList<>();
      SpeciesProfile p = SpeciesProfiles.get(species);
      if (p != null) {
         for (StageDef stage : p.taming().pool().stages()) {
            steps.add(step(stage.check()));
         }
      }

      SpeciesGameplayProfile g = SpeciesGameplayRegistry.getProfile(species);
      return all(
         steps,
         g == null ? List.of() : foods(g),
         g == null ? List.of() : sorted(g.dislikedFoods()),
         g == null ? List.of() : sorted(g.hoardItems()),
         p == null ? List.of() : seen(p)
      );
   }

   static List<String> seen(SpeciesProfile p) {
      Set<String> kinds = new HashSet<>();

      for (RelationRule rule : p.relations()) {
         if (!rule.targets().equals("minecraft:player") && !rule.targets().equals("#fauna:same_species")) {
            String kind = kind(rule.type());
            if (kind != null) {
               kinds.add(kind);
            }
         }
      }

      List<String> out = new ArrayList<>();

      for (String kind : List.of("prey", "fear", "foe")) {
         if (kinds.contains(kind)) {
            out.add(kind);
         }
      }

      return out;
   }

   @Nullable
   public static String kind(RelationType type) {
      return switch (type) {
         case EATS -> "prey";
         case AFRAID -> "fear";
         case ATTACKS -> "foe";
         default -> null;
      };
   }

   static Set<String> all(List<String> steps, List<ResourceLocation> foods, List<ResourceLocation> dislikes, List<ResourceLocation> hoard) {
      return all(steps, foods, dislikes, hoard, List.of());
   }

   static Set<String> all(List<String> steps, List<ResourceLocation> foods, List<ResourceLocation> dislikes, List<ResourceLocation> hoard, List<String> seen) {
      Set<String> out = new LinkedHashSet<>();

      for (String step : steps) {
         out.add("ask:" + step);
      }

      for (ResourceLocation food : foods) {
         out.add("food:" + food);
      }

      for (ResourceLocation food : dislikes) {
         out.add("dislike:" + food);
      }

      for (ResourceLocation item : hoard) {
         out.add("gift:" + item);
      }

      if (!steps.isEmpty()) {
         for (String phase : PHASES) {
            out.add("phase:" + phase);
         }
      }

      for (String kind : seen) {
         out.add("seen:" + kind);
      }

      return out;
   }

   public static Set<String> plain() {
      Set<String> out = new LinkedHashSet<>();

      for (String step : PLAIN_STEPS) {
         out.add("ask:" + step);
      }

      return out;
   }

   private static boolean met(DragonDiscoveryData data, DragonType type) {
      return data.hasEncountered(type) || data.hasKilled(type) || data.hasTamed(type) || data.isBonded(type);
   }

   public static Set<String> known(DragonDiscoveryData data, DragonType type) {
      if (!met(data, type)) {
         return Set.of();
      }

      String species = type.getSerializedName();
      return known(all(species), plain(), data.facts(type));
   }

   static Set<String> known(Set<String> all, Set<String> plain, Set<String> learned) {
      Set<String> out = new LinkedHashSet<>(plain);
      out.addAll(learned);
      out.retainAll(all);
      return out;
   }

   public static float progress(DragonDiscoveryData data, DragonType type) {
      return share(all(type.getSerializedName()).size(), known(data, type).size());
   }

   static float share(int all, int known) {
      return all == 0 ? 0.0F : (float)known / all;
   }

   static boolean full(float share) {
      return share >= 0.75F;
   }

   public static int tier(DragonDiscoveryData data, DragonType type) {
      if (!met(data, type)) {
         return 0;
      } else {
         return full(progress(data, type)) ? 2 : 1;
      }
   }
}

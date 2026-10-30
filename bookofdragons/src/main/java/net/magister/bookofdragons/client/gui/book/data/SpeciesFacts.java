package net.magister.bookofdragons.client.gui.book.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.client.gui.hud.StripText;
import net.magister.bookofdragons.datagen.data.DragonDiscoveryData;
import net.magister.bookofdragons.discovery.DiscoveryStore;
import net.magister.bookofdragons.discovery.Lore;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayProfile;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayRegistry;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.api.SpeciesProfiles;
import net.magister.liberanimalium.fauna.api.Valence;
import net.magister.liberanimalium.fauna.api.DayCycle.Habit;
import net.magister.liberanimalium.fauna.api.Ladder.StageDef;
import net.magister.liberanimalium.fauna.api.SpeciesProfile.RelationRule;
import net.magister.liberanimalium.spawn.Placement;
import net.magister.liberanimalium.spawn.SpawnCondition;
import net.magister.liberanimalium.spawn.SpawnRule;
import net.magister.liberanimalium.spawn.SpawnRules;
import net.magister.liberanimalium.spawn.SpawnCondition.AboveGround;
import net.magister.liberanimalium.spawn.SpawnCondition.Advancement;
import net.magister.liberanimalium.spawn.SpawnCondition.BelowSurface;
import net.magister.liberanimalium.spawn.SpawnCondition.BiomeIs;
import net.magister.liberanimalium.spawn.SpawnCondition.BlockBelow;
import net.magister.liberanimalium.spawn.SpawnCondition.CaveSize;
import net.magister.liberanimalium.spawn.SpawnCondition.CreaturesNearby;
import net.magister.liberanimalium.spawn.SpawnCondition.DifficultyAtLeast;
import net.magister.liberanimalium.spawn.SpawnCondition.DimensionIs;
import net.magister.liberanimalium.spawn.SpawnCondition.Fluid;
import net.magister.liberanimalium.spawn.SpawnCondition.FromWorldSpawn;
import net.magister.liberanimalium.spawn.SpawnCondition.KinNearby;
import net.magister.liberanimalium.spawn.SpawnCondition.Light;
import net.magister.liberanimalium.spawn.SpawnCondition.Moon;
import net.magister.liberanimalium.spawn.SpawnCondition.NearRoost;
import net.magister.liberanimalium.spawn.SpawnCondition.NearbyBlock;
import net.magister.liberanimalium.spawn.SpawnCondition.PlayerDistance;
import net.magister.liberanimalium.spawn.SpawnCondition.Poi;
import net.magister.liberanimalium.spawn.SpawnCondition.Range;
import net.magister.liberanimalium.spawn.SpawnCondition.Ref;
import net.magister.liberanimalium.spawn.SpawnCondition.Sky;
import net.magister.liberanimalium.spawn.SpawnCondition.Slope;
import net.magister.liberanimalium.spawn.SpawnCondition.StructureAt;
import net.magister.liberanimalium.spawn.SpawnCondition.Temperature;
import net.magister.liberanimalium.spawn.SpawnCondition.Time;
import net.magister.liberanimalium.spawn.SpawnCondition.Weather;
import net.magister.liberanimalium.spawn.SpawnCondition.WorldAge;
import net.magister.liberanimalium.spawn.SpawnCondition.Y;
import net.magister.liberanimalium.spawn.SpawnRule.Beacon;
import net.magister.liberanimalium.spawn.SpawnRule.Special;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;

public final class SpeciesFacts {
   private static final String K = "book.bookofdragons.";
   public static final int INK_DATA = 10035230;
   private static final int INK_HINT = 9075298;
   private static final Set<String> WHEN_KINDS = Set.of(
      "dimension", "time", "moon", "weather", "world_age", "difficulty", "player_distance", "from_world_spawn", "advancement", "kin_nearby", "creatures_nearby"
   );
   public static final List<String> TRAITS = List.of("aggression", "bravery", "dominance", "energy", "nervousness", "sympathy");
   private static final int ALL = Integer.MAX_VALUE;
   private static final int ICONS = 4;

   private SpeciesFacts() {
   }

   @Nullable
   private static DragonDiscoveryData book() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == null ? null : DiscoveryStore.get(mc.player).orElse(null);
   }

   public static int tier(DragonType type) {
      DragonDiscoveryData d = book();
      return d == null ? 0 : Lore.tier(d, type);
   }

   public static float progress(DragonType type) {
      DragonDiscoveryData d = book();
      return d == null ? 0.0F : Lore.progress(d, type);
   }

   private static Set<String> known(DragonType type) {
      DragonDiscoveryData d = book();
      return d == null ? Set.of() : Lore.known(d, type);
   }

   private static Component say(boolean sure, String key, String unsure, Object... args) {
      return Component.translatable(sure ? key : unsure, args);
   }

   private static String id(DragonType type) {
      return type.getSerializedName();
   }

   private static SpeciesProfile profile(DragonType type) {
      return SpeciesProfiles.get(id(type));
   }

   @Nullable
   private static SpeciesGameplayProfile gameplay(DragonType type) {
      return SpeciesGameplayRegistry.getProfile(id(type));
   }

   private static List<Component> unseen() {
      return List.of(Component.translatable("book.bookofdragons.journal.unseen"));
   }

   private static List<SpawnRule> spawns(DragonType type) {
      List<SpawnRule> out = new ArrayList<>();

      for (SpawnRule rule : SpawnRules.of("bookofdragons")) {
         if (rule.type() == type.getEntityType() && rule.variant() == null) {
            out.add(rule);
         }
      }

      return out;
   }

   public static List<Component> habitatJournal(DragonType type, int tier) {
      List<SpawnRule> spawns = spawns(type);
      if (spawns.isEmpty()) {
         return unseen();
      }

      List<Component> out = new ArrayList<>();
      out.add(
         Component.translatable(
            "book.bookofdragons." + (tier < 1 ? "journal.habitat.tales" : "journal.habitat.found"),
            new Object[]{list(regions(spawns), 3, SpeciesFacts::ink, "land")}
         )
      );
      if (tier < 2) {
         return out;
      }

      List<Component> when = sharedWhen(spawns, type);
      if (!when.isEmpty()) {
         out.add(Component.translatable("book.bookofdragons.journal.habitat.when", new Object[]{plain(when, 3, SpeciesFacts::ink)}));
      }

      List<Component> signs = signs(spawns, type);
      if (!signs.isEmpty()) {
         out.add(Component.translatable("book.bookofdragons.journal.habitat.signs", new Object[]{plain(signs, 3, SpeciesFacts::ink)}));
      }

      if (colony(spawns) != null) {
         out.add(Component.translatable("book.bookofdragons.journal.habitat.colony"));
      }

      long age = age(spawns);
      if (age > 0L) {
         out.add(Component.translatable("book.bookofdragons.journal.habitat.age", new Object[]{ink(Component.literal(String.valueOf(age)))}));
      }

      Difficulty difficulty = difficulty(spawns);
      if (difficulty != null) {
         out.add(Component.translatable("book.bookofdragons.journal.habitat.difficulty", new Object[]{ink(difficulty.getDisplayName())}));
      }

      return out;
   }

   public static List<Component> habitatData(DragonType type, int tier) {
      List<Component> out = new ArrayList<>();
      out.add(title("habitat"));
      List<SpawnRule> spawns = spawns(type);
      if (spawns.isEmpty()) {
         return out;
      }

      if (tier < 2) {
         List<Component> names = new ArrayList<>();

         for (SpeciesFacts.Named n : regions(spawns)) {
            names.add(n.name());
         }

         out.add(row(Component.translatable("book.bookofdragons.data.regions"), names, ", "));
         out.add(hint(tier < 1 ? Component.translatable("book.bookofdragons.data.hint.find") : Component.translatable("book.bookofdragons.data.hint.learn")));
         return out;
      } else {
         Set<String> shared = strings(sharedWhen(spawns, type));

         for (SpawnRule rule : spawns) {
            List<Component> parts = new ArrayList<>();
            parts.add(Component.translatable("book.bookofdragons.field.place." + rule.placement().name().toLowerCase(Locale.ROOT)));

            for (SpawnCondition c : rule.conditions()) {
               if (!(c instanceof WorldAge) && !(c instanceof DifficultyAtLeast)) {
                  Component p = phrase(c, type, Integer.MAX_VALUE);
                  if (p != null && !shared.contains(p.getString())) {
                     parts.add(p);
                  }
               }
            }

            out.add(row(placeLabel(rule), parts, ", "));
         }

         List<Component> when = sharedWhen(spawns, type);
         if (!when.isEmpty()) {
            out.add(row(Component.translatable("book.bookofdragons.data.when"), when, ", "));
         }

         long age = age(spawns);
         if (age > 0L) {
            out.add(row(Component.translatable("book.bookofdragons.data.from_day"), List.of(Component.literal(String.valueOf(age))), ", "));
         }

         Difficulty difficulty = difficulty(spawns);
         if (difficulty != null) {
            out.add(row(Component.translatable("book.bookofdragons.data.difficulty"), List.of(difficulty.getDisplayName()), ", "));
         }

         Beacon beacon = colony(spawns);
         if (beacon != null) {
            out.add(
               row(
                  Component.translatable("book.bookofdragons.data.colony"),
                  List.of(Component.translatable("book.bookofdragons.data.colony.value", new Object[]{beacon.maxKin(), beacon.radius()})),
                  ", "
               )
            );
         }

         List<Component> signs = signs(spawns, type);
         if (!signs.isEmpty()) {
            out.add(row(Component.translatable("book.bookofdragons.data.more_often"), signs, ", "));
         }

         return out;
      }
   }

   private static Component placeLabel(SpawnRule rule) {
      String path = rule.id().getPath();
      String name = path.substring(path.lastIndexOf(47) + 1);
      String words = name.replace('_', ' ');
      String fallback = words.isEmpty() ? words : Character.toUpperCase(words.charAt(0)) + words.substring(1);
      return Component.translatableWithFallback("book.bookofdragons.place." + name, fallback);
   }

   private static List<SpeciesFacts.Named> regions(List<SpawnRule> spawns) {
      List<List<SpeciesFacts.Named>> places = new ArrayList<>();

      for (SpawnRule rule : spawns) {
         places.add(regionsOf(rule));
      }

      List<SpeciesFacts.Named> out = new ArrayList<>();
      Set<String> seen = new HashSet<>();
      int i = 0;

      while (true) {
         boolean more = false;

         for (List<SpeciesFacts.Named> place : places) {
            if (i < place.size()) {
               more = true;
               if (seen.add(place.get(i).name().getString())) {
                  out.add(place.get(i));
               }
            }
         }

         if (!more) {
            return out;
         }

         i++;
      }
   }

   private static List<SpeciesFacts.Named> regionsOf(SpawnRule rule) {
      List<SpeciesFacts.Named> out = new ArrayList<>();

      for (SpawnCondition c : rule.conditions()) {
         if (c instanceof BiomeIs b) {
            for (Ref ref : b.refs()) {
               out.add(new SpeciesFacts.Named(name(ref.id(), ref.tag(), "biome"), landKind(ref)));
            }
         }
      }

      if (!out.isEmpty()) {
         return out;
      }

      for (SpawnCondition c : rule.conditions()) {
         if (c instanceof Temperature t) {
            String climate = climate(t);

            String kind = switch (climate) {
               case "hot_dry", "hot" -> "dry";
               case "warm" -> "lush";
               case "cold" -> "cold";
               default -> "wild";
            };
            return List.of(new SpeciesFacts.Named(Component.translatable("book.bookofdragons.region.climate." + climate), kind));
         }

         if (c instanceof Weather w) {
            return List.of(
               new SpeciesFacts.Named(Component.translatable("book.bookofdragons.region.weather." + w.kind()), w.kind().equals("thunder") ? "high" : "wild")
            );
         }

         if (c instanceof NearRoost r && !r.kind().isEmpty()) {
            String kind = switch (r.kind()) {
               case "cliff" -> "high";
               case "cave" -> "cave";
               case "glade" -> "wood";
               case "wetland" -> "wet";
               case "barrens" -> "dry";
               default -> "wild";
            };
            return List.of(new SpeciesFacts.Named(Component.translatable("book.bookofdragons.region.roost", new Object[]{roost(r.kind())}), kind));
         }
      }

      boolean deep = false;

      for (SpawnCondition c : rule.conditions()) {
         if (c instanceof BelowSurface b && b.range().min() >= 30.0) {
            deep = true;
         }
      }

      String ground = rule.placement() == Placement.CAVE && deep ? "cave_deep" : rule.placement().name().toLowerCase(Locale.ROOT);
      return List.of(new SpeciesFacts.Named(Component.translatable("book.bookofdragons.region." + ground), rule.placement() == Placement.CAVE ? "cave" : "wild"));
   }

   public static String habitatKind(DragonType type) {
      Map<String, Double> weight = new HashMap<>();

      for (SpawnRule rule : spawns(type)) {
         List<SpeciesFacts.Named> lands = regionsOf(rule);

         for (SpeciesFacts.Named land : lands) {
            weight.merge(land.kind(), (double)rule.weight() / lands.size(), Double::sum);
         }
      }

      String best = "wild";
      double most = 0.0;

      for (Entry<String, Double> e : weight.entrySet()) {
         if (e.getValue() > most) {
            most = e.getValue();
            best = e.getKey();
         }
      }

      return best;
   }

   private static String landKind(Ref ref) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return "wild";
      }

      Registry<Biome> biomes = mc.level.registryAccess().registryOrThrow(Registries.BIOME);
      List<String> kinds = new ArrayList<>();
      if (ref.tag()) {
         biomes.getTag(TagKey.create(Registries.BIOME, ref.id())).ifPresent(set -> set.forEach(b -> kinds.add(landKind(b))));
      } else {
         biomes.getHolder(ResourceKey.create(Registries.BIOME, ref.id())).ifPresent(b -> kinds.add(landKind(b)));
      }

      return majority(kinds, "wild");
   }

   private static String landKind(Holder<Biome> b) {
      if (b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_DEEP_OCEAN)) {
         return "sea";
      }

      if (b.is(BiomeTags.IS_BEACH)) {
         return "shore";
      }

      if (b.is(BiomeTags.IS_RIVER)) {
         return "river";
      }

      if (!b.is(Biomes.SWAMP) && !b.is(Biomes.MANGROVE_SWAMP)) {
         if (((Biome)b.value()).getBaseTemperature() < 0.3F) {
            return "cold";
         } else if (!((Biome)b.value()).hasPrecipitation()) {
            return "dry";
         } else if (b.is(BiomeTags.IS_JUNGLE)) {
            return "lush";
         } else if (b.is(BiomeTags.IS_FOREST) || b.is(BiomeTags.IS_TAIGA)) {
            return "wood";
         } else {
            return !b.is(BiomeTags.IS_MOUNTAIN) && !b.is(BiomeTags.IS_HILL) ? "wild" : "high";
         }
      } else {
         return "wet";
      }
   }

   private static boolean whenKind(SpawnCondition c) {
      return WHEN_KINDS.contains(c.name()) && !(c instanceof WorldAge) && !(c instanceof DifficultyAtLeast);
   }

   private static List<Component> sharedWhen(List<SpawnRule> spawns, DragonType type) {
      List<Component> out = new ArrayList<>();
      Set<String> seen = new HashSet<>();

      for (SpawnCondition c : spawns.get(0).conditions()) {
         if (whenKind(c)) {
            Component p = phrase(c, type, Integer.MAX_VALUE);
            if (p != null && seen.add(p.getString())) {
               boolean everywhere = true;

               for (SpawnRule other : spawns) {
                  boolean has = false;

                  for (SpawnCondition o : other.conditions()) {
                     Component q = whenKind(o) ? phrase(o, type, Integer.MAX_VALUE) : null;
                     if (q != null && q.getString().equals(p.getString())) {
                        has = true;
                     }
                  }

                  everywhere &= has;
               }

               if (everywhere) {
                  out.add(p);
               }
            }
         }
      }

      return out;
   }

   private static List<Component> signs(List<SpawnRule> spawns, DragonType type) {
      List<Component> out = new ArrayList<>();
      Set<String> seen = new HashSet<>();

      for (SpawnRule rule : spawns) {
         for (Special s : rule.specials()) {
            Component p = phrase(s.condition(), type, 3);
            if (p != null && seen.add(p.getString())) {
               out.add(p);
            }
         }
      }

      return out;
   }

   private static long age(List<SpawnRule> spawns) {
      long out = Long.MAX_VALUE;

      for (SpawnRule rule : spawns) {
         long days = 0L;

         for (SpawnCondition c : rule.conditions()) {
            if (c instanceof WorldAge a) {
               days = a.minDays();
            }
         }

         out = Math.min(out, days);
      }

      return out == Long.MAX_VALUE ? 0L : out;
   }

   @Nullable
   private static Difficulty difficulty(List<SpawnRule> spawns) {
      Difficulty out = null;

      for (SpawnRule rule : spawns) {
         Difficulty d = null;

         for (SpawnCondition c : rule.conditions()) {
            if (c instanceof DifficultyAtLeast a) {
               d = a.min();
            }
         }

         if (d == null) {
            return null;
         }

         if (out == null || d.getId() < out.getId()) {
            out = d;
         }
      }

      return out;
   }

   @Nullable
   private static Beacon colony(List<SpawnRule> spawns) {
      for (SpawnRule rule : spawns) {
         if (rule.beacon() != null) {
            return rule.beacon();
         }
      }

      return null;
   }

   @Nullable
   static Component phrase(SpawnCondition c, DragonType type, int cap) {
      String n = "book.bookofdragons.cond.";
      if (c instanceof DimensionIs) {
         return null;
      } else if (c instanceof BiomeIs b) {
         return Component.translatable(n + "biome", new Object[]{names(b.refs(), "biome", cap)});
      } else if (c instanceof AboveGround a) {
         return range(n + "above_ground", a.range());
      } else if (c instanceof BelowSurface b) {
         return range(n + "below_surface", b.range());
      } else if (c instanceof Y y) {
         return range(n + "y", y.range());
      } else if (c instanceof Sky s) {
         return Component.translatable(n + "sky." + (s.exposed() ? "exposed" : "sheltered"));
      } else if (c instanceof Light l) {
         return Component.translatable(n + (l.block().max() <= 0.0 ? "light.dark" : "light"));
      } else if (c instanceof Time t) {
         if (!t.habit().isEmpty()) {
            boolean night = profile(type).habit() == Habit.NIGHT;
            boolean awake = t.habit().equals("active");
            return Component.translatable(n + "time." + (night == awake ? "night" : "day"));
         } else if (t.windows().size() == 1 && ((long[])t.windows().get(0))[0] >= 12000L) {
            return Component.translatable(n + "time.night");
         } else {
            return t.windows().size() == 1 && ((long[])t.windows().get(0))[1] <= 13000L
               ? Component.translatable(n + "time.day")
               : Component.translatable(n + "time.windows");
         }
      } else if (c instanceof Moon m) {
         if (m.phases().equals(Set.of(0))) {
            return Component.translatable(n + "moon.full");
         } else {
            return m.phases().equals(Set.of(4)) ? Component.translatable(n + "moon.new") : Component.translatable(n + "moon.other");
         }
      } else if (c instanceof Weather w) {
         return Component.translatable(n + "weather." + w.kind());
      } else if (c instanceof Temperature t) {
         return Component.translatable(n + "temperature." + climate(t));
      } else if (c instanceof BlockBelow b) {
         return Component.translatable(n + "block_below", new Object[]{names(b.refs(), "block", cap)});
      } else if (c instanceof NearbyBlock b) {
         return Component.translatable(n + "nearby_block", new Object[]{names(b.refs(), "block", cap)});
      } else if (c instanceof Fluid f) {
         return f.kind().equals("none") ? null : Component.translatable(n + "fluid." + f.kind());
      } else if (c instanceof StructureAt s) {
         return Component.translatable(n + "structure", new Object[]{names(s.refs(), "structure", cap)});
      } else if (c instanceof Poi p) {
         return Component.translatable(
            n + "poi",
            new Object[]{
               Component.translatableWithFallback(
                  "book.bookofdragons.poi." + ((Ref)p.refs().get(0)).id().getPath(), ((Ref)p.refs().get(0)).id().getPath().replace('_', ' ')
               )
            }
         );
      } else if (c instanceof NearRoost r) {
         return r.kind().isEmpty()
            ? Component.translatable(n + "near_roost.any")
            : Component.translatable(n + (r.empty() ? "near_roost.empty" : "near_roost"), new Object[]{roost(r.kind())});
      } else if (c instanceof KinNearby k) {
         return Component.translatable(n + (k.count().max() <= 0.0 ? "kin_nearby.none" : "kin_nearby.some"));
      } else if (c instanceof CreaturesNearby k) {
         return Component.translatable(n + (k.max() <= 0 ? "creatures_nearby.none" : "creatures_nearby.few"));
      } else if (c instanceof PlayerDistance) {
         return Component.translatable(n + "player_distance");
      } else if (c instanceof FromWorldSpawn) {
         return Component.translatable(n + "from_world_spawn");
      } else if (c instanceof WorldAge a) {
         return Component.translatable(n + "world_age", new Object[]{a.minDays()});
      } else if (c instanceof DifficultyAtLeast d) {
         return Component.translatable(n + "difficulty", new Object[]{d.min().getDisplayName()});
      } else if (c instanceof Advancement) {
         return Component.translatable(n + "advancement");
      } else if (c instanceof Slope) {
         return Component.translatable(n + "slope");
      } else {
         return c instanceof CaveSize ? Component.translatable(n + "cave_size") : null;
      }
   }

   private static String climate(Temperature t) {
      if (t.range().min() >= 1.5) {
         return "none".equals(t.precipitation()) ? "hot_dry" : "hot";
      } else if (t.range().min() >= 0.8) {
         return "warm";
      } else {
         return t.range().max() <= 0.3 ? "cold" : "other";
      }
   }

   private static Component roost(String kind) {
      return Component.translatableWithFallback("book.bookofdragons.roost." + kind, kind);
   }

   private static Component range(String key, Range r) {
      boolean lo = Double.isFinite(r.min());
      boolean hi = Double.isFinite(r.max());
      if (lo && hi) {
         return Component.translatable(key + ".range", new Object[]{(int)r.min(), (int)r.max()});
      } else {
         return lo ? Component.translatable(key + ".min", new Object[]{(int)r.min()}) : Component.translatable(key + ".max", new Object[]{(int)r.max()});
      }
   }

   private static Component names(List<Ref> refs, String kind, int cap) {
      List<Component> names = new ArrayList<>();

      for (Ref ref : refs) {
         names.add(name(ref.id(), ref.tag(), kind));
      }

      return plain(names, cap, UnaryOperator.identity());
   }

   private static Component name(ResourceLocation id, boolean tag, String kind) {
      String fallback = id.getPath().replace("is_", "").replace('_', ' ');
      if (tag) {
         return Component.translatableWithFallback("book.bookofdragons.tag." + id.getNamespace() + "." + id.getPath(), fallback);
      }

      return (Component)(switch (kind) {
         case "biome" -> Component.translatableWithFallback("biome." + id.getNamespace() + "." + id.getPath(), fallback);
         case "block" -> BuiltInRegistries.BLOCK.containsKey(id) ? ((Block)BuiltInRegistries.BLOCK.get(id)).getName() : Component.literal(fallback);
         case "item" -> BuiltInRegistries.ITEM.containsKey(id)
            ? new ItemStack((ItemLike)BuiltInRegistries.ITEM.get(id)).getHoverName()
            : Component.literal(fallback);
         default -> Component.translatableWithFallback("book.bookofdragons.structure." + id.getPath(), fallback);
      });
   }

   public static List<Component> dietJournal(DragonType type, int tier) {
      SpeciesGameplayProfile g = gameplay(type);
      if (g == null) {
         return unseen();
      }

      List<Component> out = new ArrayList<>();
      Component meat = ink(Component.translatable("book.bookofdragons.journal.word.meat"));
      if (tier < 1) {
         out.add(Component.translatable("book.bookofdragons.journal.diet.tales", new Object[]{meat}));
         return out;
      }

      Set<String> known = known(type);
      boolean sure = tier >= 2;
      List<SpeciesFacts.Named> prey = known.contains("seen:prey") ? targets(type, RelationType.EATS) : List.of();
      if (!prey.isEmpty()) {
         out.add(Component.translatable("book.bookofdragons.journal.diet.eats_hunts", new Object[]{meat, list(prey, 3, SpeciesFacts::ink, "beast")}));
      } else {
         out.add(say(sure, "book.bookofdragons.journal.diet.eats", "book.bookofdragons.journal.diet.eats.unsure", meat));
      }

      List<SpeciesFacts.Named> favourite = foods(g.favoriteFoods(), known, "food:");
      List<SpeciesFacts.Named> treat = foods(g.specialFoods(), known, "food:");
      List<SpeciesFacts.Named> spurned = foods(g.dislikedFoods(), known, "dislike:");
      if (!favourite.isEmpty()) {
         out.add(taste(sure, "favorite", favourite));
      }

      if (!treat.isEmpty()) {
         out.add(taste(sure, "treat", treat));
      }

      if (!spurned.isEmpty()) {
         out.add(taste(sure, "dislike", spurned));
      }

      return out;
   }

   private static Component taste(boolean sure, String kind, List<SpeciesFacts.Named> foods) {
      Component taste = Component.translatable("book.bookofdragons.journal.diet.taste." + kind, new Object[]{list(foods, 2, SpeciesFacts::ink, "food")});
      return say(sure, "book.bookofdragons.journal.diet.tastes", "book.bookofdragons.journal.diet.tastes.unsure", taste);
   }

   public static List<Component> dietData(DragonType type, int tier) {
      List<Component> out = new ArrayList<>();
      out.add(title("diet"));
      SpeciesGameplayProfile g = gameplay(type);
      if (g == null) {
         return out;
      }

      List<Component> meat = List.of(Component.translatable("book.bookofdragons.journal.word.meat"));
      if (tier < 1) {
         addRow(out, Component.translatable("book.bookofdragons.data.taming_food"), meat);
         out.add(hint(Component.translatable("book.bookofdragons.data.hint.find")));
         return out;
      }

      Set<String> known = known(type);
      addRow(out, Component.translatable("book.bookofdragons.data.favorite"), names(foods(g.favoriteFoods(), known, "food:")));
      addRow(out, Component.translatable("book.bookofdragons.data.treat"), names(foods(g.specialFoods(), known, "food:")));
      addRow(out, Component.translatable("book.bookofdragons.data.dislikes"), names(foods(g.dislikedFoods(), known, "dislike:")));
      addRow(out, Component.translatable("book.bookofdragons.data.staple"), meat);
      if (known.contains("seen:prey")) {
         addRow(out, Component.translatable("book.bookofdragons.data.hunts"), names(targets(type, RelationType.EATS)));
      }

      boolean all = true;

      for (ResourceLocation food : Lore.foods(g)) {
         all &= known.contains("food:" + food);
      }

      for (ResourceLocation food : g.dislikedFoods()) {
         all &= known.contains("dislike:" + food);
      }

      if (tier < 2 || !all) {
         out.add(hint(Component.translatable("book.bookofdragons.data.hint.learn")));
      }

      return out;
   }

   public static List<ItemStack> knownFoods(DragonType type) {
      SpeciesGameplayProfile g = gameplay(type);
      if (g == null) {
         return List.of();
      }

      Set<String> known = known(type);
      List<ItemStack> out = new ArrayList<>();

      for (ResourceLocation food : Lore.foods(g)) {
         if (out.size() < 4 && known.contains("food:" + food)) {
            add(out, food);
         }
      }

      return out;
   }

   public static boolean knowsFavorite(DragonType type) {
      SpeciesGameplayProfile g = gameplay(type);
      if (g == null) {
         return false;
      }

      Set<String> known = known(type);

      for (ResourceLocation food : g.favoriteFoods()) {
         if (known.contains("food:" + food)) {
            return true;
         }
      }

      return false;
   }

   public static List<ItemStack> knownTreasures(DragonType type) {
      SpeciesGameplayProfile g = gameplay(type);
      if (g == null) {
         return List.of();
      }

      Set<String> known = known(type);
      List<ItemStack> out = new ArrayList<>();

      for (ResourceLocation item : Lore.sorted(g.hoardItems())) {
         if (out.size() < 4 && known.contains("gift:" + item)) {
            add(out, item);
         }
      }

      return out;
   }

   private static void add(List<ItemStack> out, ResourceLocation id) {
      Item item = (Item)BuiltInRegistries.ITEM.get(id);
      if (item != Items.AIR) {
         out.add(new ItemStack(item));
      }
   }

   private static List<ItemStack> items(Set<ResourceLocation> ids) {
      List<ItemStack> out = new ArrayList<>();

      for (ResourceLocation id : Lore.sorted(ids)) {
         add(out, id);
      }

      return out;
   }

   private static List<SpeciesFacts.Named> foods(Set<ResourceLocation> ids) {
      List<SpeciesFacts.Named> out = new ArrayList<>();

      for (ItemStack stack : items(ids)) {
         out.add(new SpeciesFacts.Named(stack.getHoverName(), stack.is(ItemTags.FISHES) ? "fish" : "any"));
      }

      return out;
   }

   private static List<SpeciesFacts.Named> foods(Set<ResourceLocation> ids, Set<String> known, String fact) {
      Set<ResourceLocation> learned = new HashSet<>();

      for (ResourceLocation id : ids) {
         if (known.contains(fact + id)) {
            learned.add(id);
         }
      }

      return foods(learned);
   }

   public static List<Component> temperamentJournal(DragonType type, int tier) {
      SpeciesProfile p = profile(type);
      RelationType people = relationTo(p, "minecraft:player");
      if (tier < 1) {
         return List.of(Component.translatable("book.bookofdragons.journal.temper.rumor." + (people == null ? "none" : word(people))));
      }

      List<Component> out = new ArrayList<>();
      boolean night = p.habit() == Habit.NIGHT;
      RelationType kin = relationTo(p, "#fauna:same_species");
      boolean sure = tier >= 2;
      out.add(
         say(
            sure,
            "book.bookofdragons.journal.temper.open",
            "book.bookofdragons.journal.temper.open.unsure",
            Component.translatable(
               "book.bookofdragons.journal.temper.habit." + (night ? "night" : "day"),
               new Object[]{ink(Component.translatable("book.bookofdragons.journal.word." + (night ? "night" : "day")))}
            ),
            Component.translatable("book.bookofdragons.journal.temper.kin." + (kin == null ? "none" : word(kin)))
         )
      );
      boolean guards = p.social().territorial() && p.social().territoryRadius() > 0.0F;
      Component ground = ink(Component.literal(String.valueOf(Math.round(p.social().territoryRadius()))));
      if (people != null) {
         Component manner = Component.translatable("book.bookofdragons.journal.temper.people." + word(people));
         out.add(
            guards
               ? say(sure, "book.bookofdragons.journal.temper.people_ground", "book.bookofdragons.journal.temper.people_ground.unsure", manner, ground)
               : say(sure, "book.bookofdragons.journal.temper.people_only", "book.bookofdragons.journal.temper.people_only.unsure", manner)
         );
      } else if (guards) {
         out.add(say(sure, "book.bookofdragons.journal.temper.ground", "book.bookofdragons.journal.temper.ground.unsure", ground));
      }

      Set<String> learned = known(type);
      if (tier >= 1) {
         List<SpeciesFacts.Named> fears = learned.contains("seen:fear") ? targets(type, RelationType.AFRAID) : List.of();
         List<SpeciesFacts.Named> foes = learned.contains("seen:foe") ? targets(type, RelationType.ATTACKS) : List.of();
         Component feared = list(fears, 2, SpeciesFacts::ink, "beast");
         Component fought = list(foes, 2, SpeciesFacts::ink, "beast");
         if (!fears.isEmpty() && !foes.isEmpty()) {
            out.add(Component.translatable("book.bookofdragons.journal.temper.fears_foes", new Object[]{feared, fought}));
         } else if (!fears.isEmpty()) {
            out.add(Component.translatable("book.bookofdragons.journal.temper.fears", new Object[]{feared}));
         } else if (!foes.isEmpty()) {
            out.add(Component.translatable("book.bookofdragons.journal.temper.foes", new Object[]{fought}));
         }
      }

      return out;
   }

   private static String word(RelationType type) {
      return type.name().toLowerCase(Locale.ROOT);
   }

   public static float[] traits(DragonType type) {
      Map<String, float[]> bands = profile(type).traits();
      float[] out = new float[TRAITS.size()];

      for (int i = 0; i < TRAITS.size(); i++) {
         float[] band = bands.get(TRAITS.get(i));
         out[i] = band == null ? -1.0F : (band[0] + band[1]) / 2.0F;
      }

      return out;
   }

   @Nullable
   private static RelationType relationTo(SpeciesProfile p, String target) {
      for (RelationRule rule : p.relations()) {
         if (rule.targets().equals(target)) {
            return rule.type();
         }
      }

      return null;
   }

   private static List<SpeciesFacts.Named> targets(DragonType type, RelationType kind) {
      List<SpeciesFacts.Named> out = new ArrayList<>();

      for (RelationRule rule : profile(type).relations()) {
         if (rule.type() == kind && !rule.targets().equals("minecraft:player") && !rule.targets().equals("#fauna:same_species")) {
            boolean tag = rule.targets().startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? rule.targets().substring(1) : rule.targets());
            if (id != null) {
               if (tag) {
                  List<String> kinds = new ArrayList<>();
                  BuiltInRegistries.ENTITY_TYPE
                     .getTag(TagKey.create(Registries.ENTITY_TYPE, id))
                     .ifPresent(set -> set.forEach(e -> kinds.add(creatureKind((EntityType<?>)e.value()))));
                  out.add(new SpeciesFacts.Named(name(id, true, "entity"), majority(kinds, "any")));
               } else if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                  EntityType<?> entity = (EntityType<?>)BuiltInRegistries.ENTITY_TYPE.get(id);
                  out.add(new SpeciesFacts.Named(entity.getDescription(), creatureKind(entity)));
               }
            }
         }
      }

      return out;
   }

   private static String creatureKind(EntityType<?> entity) {
      if (BuiltInRegistries.ENTITY_TYPE.getKey(entity).getNamespace().equals("bookofdragons")) {
         return "dragon";
      }

      return switch (entity.getCategory().getName()) {
         case "creature" -> "beast";
         case "monster" -> "foul";
         case "ambient" -> "small";
         case "water_creature", "water_ambient", "underground_water_creature", "axolotls" -> "sea";
         default -> "any";
      };
   }

   public static List<Component> tamingJournal(DragonType type, int tier) {
      SpeciesProfile p = profile(type);
      SpeciesGameplayProfile g = gameplay(type);
      if (tier < 1) {
         return List.of(
            Component.translatable("book.bookofdragons.journal.taming.tales", new Object[]{ink(Component.translatable("book.bookofdragons.journal.word.meat"))})
         );
      }

      List<Component> out = new ArrayList<>();
      Set<String> known = known(type);
      List<Component> road = road(p, g, known);
      if (!road.isEmpty()) {
         out.add(
            say(
               tier >= 2,
               "book.bookofdragons.journal.taming.road",
               "book.bookofdragons.journal.taming.road.unsure",
               plain(road, Integer.MAX_VALUE, SpeciesFacts::ink)
            )
         );
      }

      if (g != null) {
         List<SpeciesFacts.Named> hoard = new ArrayList<>();

         for (ItemStack stack : knownTreasures(type)) {
            hoard.add(new SpeciesFacts.Named(stack.getHoverName(), "trinket"));
         }

         if (!hoard.isEmpty()) {
            out.add(Component.translatable("book.bookofdragons.journal.taming.hoard", new Object[]{list(hoard, 3, SpeciesFacts::ink, "hoard")}));
         }
      }

      if (tier < 2) {
         return out.isEmpty() ? unseen() : out;
      }

      List<SpeciesFacts.Named> loves = g == null ? List.of() : foods(g.favoriteFoods(), known, "food:");
      if (!loves.isEmpty()) {
         out.add(Component.translatable("book.bookofdragons.journal.taming.gift", new Object[]{list(loves, 2, SpeciesFacts::ink, "food")}));
      }

      List<Component> feelings = new ArrayList<>();

      for (Entry<String, Valence> row : p.taming().matrix().rows().entrySet()) {
         String act = row.getKey().startsWith("act:") ? row.getKey().substring(4) : row.getKey();
         feelings.add(
            Component.translatable(
               "book.bookofdragons.journal.taming.feel." + row.getValue().name().toLowerCase(Locale.ROOT),
               new Object[]{ink(Component.translatableWithFallback("book.bookofdragons.taming.act." + act, act.replace('_', ' ')))}
            )
         );
      }

      if (!feelings.isEmpty()) {
         out.add(Component.translatable("book.bookofdragons.journal.taming.feelings", new Object[]{plain(feelings, Integer.MAX_VALUE, UnaryOperator.identity())}));
      }

      return out;
   }

   private static List<Component> road(SpeciesProfile p, @Nullable SpeciesGameplayProfile g, Set<String> known) {
      List<Component> out = new ArrayList<>();

      for (StageDef stage : p.taming().pool().stages()) {
         if (known.contains("ask:" + Lore.step(stage.check()))) {
            out.add(step(stage, g));
         }
      }

      return out;
   }

   private static Component step(StageDef stage, @Nullable SpeciesGameplayProfile g) {
      String id = stage.check().id();
      String step = id.substring(id.indexOf(58) + 1);
      if (step.equals("touch") && stage.item() != null) {
         boolean tag = stage.item().startsWith("#");
         ResourceLocation item = ResourceLocation.tryParse(tag ? stage.item().substring(1) : stage.item());
         return item == null
            ? Component.translatable("book.bookofdragons.taming.step.touch")
            : Component.translatable("book.bookofdragons.taming.step.touch.item", new Object[]{name(item, tag, "item")});
      } else {
         return Component.translatableWithFallback("book.bookofdragons.taming.step." + step, step.replace('_', ' '));
      }
   }

   @Nullable
   public static Component strip(DragonBase dragon, String kind, String act) {
      Component who = (Component)(dragon.hasCustomName()
         ? dragon.getName()
         : Component.translatable("book.bookofdragons.strip.subject", new Object[]{dragon.getType().getDescription()}));
      if (kind.equals("refuses")) {
         ResourceLocation item = ResourceLocation.tryParse(act);
         return item == null ? null : StripText.style("book.bookofdragons.strip.refuses", who, name(item, false, "item"));
      }

      if (!kind.equals("ask")) {
         return StripText.style("book.bookofdragons.strip." + kind, who);
      }

      String species = dragon.speciesId();
      SpeciesProfile p = SpeciesProfiles.get(species);
      if (p == null) {
         return null;
      }

      for (StageDef stage : p.taming().pool().stages()) {
         String id = stage.check().id();
         if (id.substring(id.indexOf(58) + 1).equals(act)) {
            String key = "book.bookofdragons.strip.ask." + act;
            String own = key + "." + species;
            return StripText.style(I18n.exists(own) ? own : key, who, step(stage, SpeciesGameplayRegistry.getProfile(species)));
         }
      }

      return null;
   }

   private static Component ink(Component c) {
      return c.copy().withStyle(s -> s.withColor(10035230));
   }

   private static Component title(String section) {
      return Component.translatable("book.bookofdragons.field.title." + section);
   }

   private static Component hint(Component text) {
      return text.copy().withStyle(s -> s.withColor(9075298));
   }

   private static Component row(Component label, List<Component> values, String separator) {
      List<Component> shown = new ArrayList<>();

      for (Component v : values) {
         shown.add(ink(v));
      }

      return label.copy().append(": ").append(join(shown, separator));
   }

   private static void addRow(List<Component> out, Component label, List<Component> values) {
      if (!values.isEmpty()) {
         out.add(row(label, values, ", "));
      }
   }

   private static List<Component> names(List<SpeciesFacts.Named> named) {
      List<Component> out = new ArrayList<>();

      for (SpeciesFacts.Named n : named) {
         out.add(n.name());
      }

      return out;
   }

   private static Component list(List<SpeciesFacts.Named> items, int cap, UnaryOperator<Component> style, String family) {
      List<Component> shown = new ArrayList<>();

      for (int i = 0; i < items.size() && i < cap; i++) {
         shown.add(style.apply(items.get(i).name()));
      }

      if (shown.isEmpty()) {
         return Component.empty();
      }

      if (items.size() <= cap) {
         return and(shown);
      }

      List<String> left = new ArrayList<>();

      for (int i = cap; i < items.size(); i++) {
         left.add(items.get(i).kind());
      }

      Component rest = Component.translatable("book.bookofdragons.field.more." + family + "." + majority(left, "any"));
      return Component.translatable("book.bookofdragons.field.and", new Object[]{join(shown, ", "), rest});
   }

   private static Component plain(List<Component> items, int cap, UnaryOperator<Component> style) {
      List<Component> shown = new ArrayList<>();

      for (int i = 0; i < items.size() && i < cap; i++) {
         shown.add(style.apply(items.get(i)));
      }

      if (shown.isEmpty()) {
         return Component.empty();
      } else {
         return (Component)(items.size() <= cap
            ? and(shown)
            : Component.translatable("book.bookofdragons.field.and", new Object[]{join(shown, ", "), Component.translatable("book.bookofdragons.field.more")}));
      }
   }

   private static Component and(List<Component> shown) {
      return (Component)(shown.size() == 1
         ? shown.get(0)
         : Component.translatable("book.bookofdragons.field.and", new Object[]{join(shown.subList(0, shown.size() - 1), ", "), shown.get(shown.size() - 1)}));
   }

   private static String majority(List<String> kinds, String fallback) {
      Map<String, Integer> counts = new HashMap<>();
      String best = fallback;
      int most = 0;

      for (String kind : kinds) {
         int n = counts.merge(kind, 1, Integer::sum);
         if (n > most) {
            most = n;
            best = kind;
         }
      }

      return best;
   }

   private static Component join(List<Component> parts, String sep) {
      MutableComponent out = Component.empty();

      for (int i = 0; i < parts.size(); i++) {
         if (i > 0) {
            out.append(sep);
         }

         out.append(parts.get(i));
      }

      return out;
   }

   private static Set<String> strings(List<Component> parts) {
      Set<String> out = new HashSet<>();

      for (Component c : parts) {
         out.add(c.getString());
      }

      return out;
   }

   static List<String> moreKeys() {
      List<String> out = new ArrayList<>();

      for (String kind : List.of("cold", "dry", "lush", "wood", "high", "sea", "shore", "river", "wet", "cave", "wild", "any")) {
         out.add("book.bookofdragons.field.more.land." + kind);
      }

      for (String kind : List.of("fish", "any")) {
         out.add("book.bookofdragons.field.more.food." + kind);
      }

      for (String kind : List.of("beast", "foul", "small", "sea", "dragon", "any")) {
         out.add("book.bookofdragons.field.more.beast." + kind);
      }

      for (String kind : List.of("trinket", "any")) {
         out.add("book.bookofdragons.field.more.hoard." + kind);
      }

      return out;
   }

   static List<String> placementKeys() {
      List<String> out = new ArrayList<>();

      for (Placement p : Placement.values()) {
         String name = p.name().toLowerCase(Locale.ROOT);
         out.add("book.bookofdragons.field.place." + name);
         out.add("book.bookofdragons.region." + name);
      }

      return out;
   }

   private record Named(Component name, String kind) {
   }
}

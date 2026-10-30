package net.magister.liberanimalium.spawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.body.locomotion.ModeChangeCause;
import net.magister.liberanimalium.body.locomotion.transition.LocomotionTransition;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.compat.Entities;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.variant.CreatureVariant;
import net.magister.liberanimalium.variant.CreatureVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class FaunaSpawner {
   private static final Set<String> WHERE = Set.of("biome", "dimension");

   private FaunaSpawner() {
   }

   public static void onLevelTick(Level level) {
      if (level instanceof ServerLevel server) {
         if (server.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            long now = server.getGameTime();
            Census census = null;

            for (String host : SpawnRules.hosts()) {
               SpawnSettings settings = SpawnSettings.of(host);
               if (settings.enabled() && Math.floorMod(now + host.hashCode(), settings.periodTicks()) == 0) {
                  if (census == null) {
                     census = Census.of(server);
                  }

                  pass(server, host, settings, census);
               }
            }
         }
      }
   }

   private static void pass(ServerLevel level, String host, SpawnSettings settings, Census census) {
      List<ServerPlayer> players = watchers(level);
      if (!players.isEmpty()) {
         SpawnLog log = SpawnLog.of(host);
         int cap = roostlessCapNow(level, host, settings, players.size());
         if (census.roostless(host) >= cap) {
            log.refused("pass skipped: roostless cap " + cap + " full");
         } else {
            List<ChunkPos> chunks = new ArrayList<>(chunksNear(level, players));

            for (int i = chunks.size() - 1; i > 0; i--) {
               Collections.swap(chunks, i, level.random.nextInt(i + 1));
            }

            for (ChunkPos chunk : chunks) {
               if (census.roostless(host) >= cap) {
                  return;
               }

               tryChunk(level, host, settings, census, players, chunk);
            }

            for (SpawnRule rule : SpawnRules.of(host)) {
               if (rule.beacon() != null) {
                  beacons(level, rule, settings, census, players);
               }
            }
         }
      }
   }

   public static int roostlessCapNow(ServerLevel level, String host, SpawnSettings settings, int players) {
      return SpawnSettings.scaled(settings.roostlessCap(players), AbundanceField.tide(level.getSeed(), host, level.getGameTime(), settings.abundance()));
   }

   private static void beacons(ServerLevel level, SpawnRule rule, SpawnSettings settings, Census census, List<ServerPlayer> players) {
      SpawnRule.Beacon beacon = rule.beacon();
      SpawnLog log = SpawnLog.of(rule.host());
      RandomSource random = level.random;
      String id = rule.id().toString();

      for (Census.Entry caller : census.beacons(rule.typeId(), beacon.everyTicks(), settings.periodTicks())) {
         if (census.nearOfType(rule.typeId(), caller.position(), beacon.radius()) >= beacon.maxKin()) {
            log.refused(id, "beacon: " + beacon.maxKin() + " kin already around it");
         } else {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double reach = Math.sqrt(random.nextDouble()) * beacon.radius();
            int x = Mth.floor(caller.position().x + Math.cos(angle) * reach);
            int z = Mth.floor(caller.position().z + Math.sin(angle) * reach);
            if (level.hasChunkAt(new BlockPos(x, 0, z))) {
               int surface = level.getHeight(Types.WORLD_SURFACE, x, z);
               BlockPos vanilla = new BlockPos(x, Mth.randomBetweenInclusive(random, level.getMinBuildHeight(), surface), z);
               BlockPos at = rule.placement().point(level, rule, vanilla, surface, new Placement.Column(x, z), random);
               String why = at == null ? noPoint(rule.placement()) : refusal(level, rule, at, census, settings, players);
               String where = "beacon " + Mth.floor(caller.position().x) + " " + Mth.floor(caller.position().z);
               SpawnRule call = at == null ? rule : rule.resolve(rule.held(contextAt(level, rule, at, census, players)));
               if (why == null && random.nextFloat() >= call.chance()) {
                  why = "its chance did not come up";
               }

               if (why != null) {
                  log.refused(id, "beacon: " + why);
                  log.tried(false, where + " - " + id + ": " + why);
               } else {
                  int spawned = spawnGroup(level, call, at, census, true);
                  log.refused(id, spawned > 0 ? "SPAWNED" : "the group found no room");
                  log.tried(true, where + " - " + id + ": spawned " + spawned + " " + rule.typeId());
               }
            }
         }
      }
   }

   private static String noPoint(Placement placement) {
      return placement == Placement.AIR ? "no air at its height in the column" : "no cave floor in the column at its depth";
   }

   private static List<ServerPlayer> watchers(ServerLevel level) {
      List<ServerPlayer> out = new ArrayList<>();

      for (ServerPlayer player : level.players()) {
         if (!player.isSpectator()) {
            out.add(player);
         }
      }

      return out;
   }

   private static Set<ChunkPos> chunksNear(ServerLevel level, List<ServerPlayer> players) {
      Set<ChunkPos> out = new LinkedHashSet<>();
      double reach = 128.0;

      for (ServerPlayer player : players) {
         ChunkPos at = player.chunkPosition();

         for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
               ChunkPos chunk = new ChunkPos(at.x + dx, at.z + dz);
               double cx = chunk.getMiddleBlockX() + 0.5 - player.getX();
               double cz = chunk.getMiddleBlockZ() + 0.5 - player.getZ();
               if (!(cx * cx + cz * cz >= reach * reach) && level.isNaturalSpawningAllowed(chunk) && level.getWorldBorder().isWithinBounds(chunk)) {
                  out.add(chunk);
               }
            }
         }
      }

      return out;
   }

   private static void tryChunk(ServerLevel level, String host, SpawnSettings settings, Census census, List<ServerPlayer> players, ChunkPos chunk) {
      RandomSource random = level.random;
      int x = chunk.getMinBlockX() + random.nextInt(16);
      int z = chunk.getMinBlockZ() + random.nextInt(16);
      int surface = level.getHeight(Types.WORLD_SURFACE, x, z);
      BlockPos vanilla = new BlockPos(x, Mth.randomBetweenInclusive(random, level.getMinBuildHeight(), surface), z);
      SpawnLog log = SpawnLog.of(host);
      List<FaunaSpawner.Candidate> passing = new ArrayList<>();
      List<SpawnRule> rules = SpawnRules.of(host);
      Placement.Column column = new Placement.Column(x, z);

      for (SpawnRule rule : rules) {
         if (rule.beacon() == null) {
            BlockPos at = rule.placement().point(level, rule, vanilla, surface, column, random);
            String why = at == null ? noPoint(rule.placement()) : refusal(level, rule, at, census, settings, players);
            if (why == null) {
               passing.add(new FaunaSpawner.Candidate(rule.resolve(rule.held(contextAt(level, rule, at, census, players))), at));
            } else {
               log.refused(rule.id().toString(), why);
            }
         }
      }

      if (passing.isEmpty()) {
         log.tried(false, "column " + vanilla.getX() + " " + vanilla.getZ() + " - none of " + rules.size() + " rules passed");
      } else {
         FaunaSpawner.Candidate pick = draw(passing, random);
         SpawnRule rule = pick.rule();
         String where = pick.at().getX() + " " + pick.at().getY() + " " + pick.at().getZ();
         if (random.nextFloat() >= rule.chance()) {
            log.refused(rule.id().toString(), "its chance did not come up");
            log.tried(true, where + " - " + rule.id() + ": its chance did not come up");
         } else {
            int spawned = spawnGroup(level, rule, pick.at(), census, true);
            log.refused(rule.id().toString(), spawned > 0 ? "SPAWNED" : "the group found no room");
            log.tried(true, where + " - " + rule.id() + ": spawned " + spawned + " " + rule.typeId());
         }
      }
   }

   private static FaunaSpawner.Candidate draw(List<FaunaSpawner.Candidate> passing, RandomSource random) {
      int total = 0;

      for (FaunaSpawner.Candidate c : passing) {
         total += c.rule().weight();
      }

      int roll = random.nextInt(total);

      for (FaunaSpawner.Candidate c : passing) {
         roll -= c.rule().weight();
         if (roll < 0) {
            return c;
         }
      }

      return passing.get(passing.size() - 1);
   }

   private static SpawnContext contextAt(ServerLevel level, SpawnRule rule, BlockPos at, Census census, List<ServerPlayer> players) {
      Vec3 point = Vec3.atBottomCenterOf(at);
      ServerPlayer nearest = null;
      double best = Double.MAX_VALUE;

      for (ServerPlayer player : players) {
         double d = player.position().distanceTo(point);
         if (d < best) {
            best = d;
            nearest = player;
         }
      }

      return new SpawnContext(level, at, rule, SpawnSpecies.profileOf(rule.type(), level), census, nearest, level.random);
   }

   @Nullable
   static String capRefusal(SpawnRule rule, Vec3 point, Census census, SpawnSettings settings, List<Vec3> players, long seed, long gameTime) {
      double tide = AbundanceField.tide(seed, rule.host(), gameTime, settings.abundance());
      int roostless = SpawnSettings.scaled(settings.roostlessCap(players.size()), tide);
      if (census.roostless(rule.host()) >= roostless) {
         return "roostless cap " + roostless + " full";
      }

      double abundance = AbundanceField.of(seed, rule.typeId(), point.x, point.z, gameTime, settings.abundance());
      if (abundance <= 0.0) {
         return "the kind is absent here now";
      }

      int speciesRoostless = SpawnSettings.scaled(settings.speciesRoostlessCap(players.size()), abundance);
      if (census.roostlessOfType(rule.typeId()) >= speciesRoostless) {
         return "species share " + speciesRoostless + " of the roostless cap full";
      }

      if (rule.worldCap() >= 0 && census.roostlessOf(rule) >= rule.worldCap()) {
         return "world cap " + rule.worldCap() + " full";
      }

      if (rule.radiusCap() > 0 && census.nearOf(rule, point, rule.radiusCap()) >= rule.radiusMax()) {
         return "radius cap " + rule.radiusMax() + " within " + rule.radiusCap() + " full";
      }

      for (Vec3 player : players) {
         if (!(player.distanceTo(point) > settings.densityRadius())) {
            if (census.near(rule.host(), player, settings.densityRadius()) >= settings.densityCap()) {
               return "density cap " + settings.densityCap() + " full near a player";
            }

            int speciesDensity = SpawnSettings.scaled(settings.speciesDensityCap(), abundance);
            if (census.nearOfType(rule.typeId(), player, settings.densityRadius()) >= speciesDensity) {
               return "species share " + speciesDensity + " of the density cap full near a player";
            }
         }
      }

      return null;
   }

   private static List<Vec3> positions(List<ServerPlayer> players) {
      List<Vec3> out = new ArrayList<>(players.size());

      for (ServerPlayer player : players) {
         out.add(player.position());
      }

      return out;
   }

   @Nullable
   public static String refusal(ServerLevel level, SpawnRule rule, BlockPos at, Census census, SpawnSettings settings, List<ServerPlayer> players) {
      Vec3 point = Vec3.atBottomCenterOf(at);
      ServerPlayer nearest = null;
      double best = Double.MAX_VALUE;

      for (ServerPlayer player : players) {
         double d = player.position().distanceTo(point);
         if (d < best) {
            best = d;
            nearest = player;
         }
      }

      SpeciesProfile species = SpawnSpecies.profileOf(rule.type(), level);
      double keep = settings.guard().playerDistance(species == null ? 0.0 : species.senses().sightRadius());
      if (best < keep) {
         return "within " + (int)keep + " blocks of a player";
      }

      SpawnContext context = new SpawnContext(level, at, rule, species, census, nearest, level.random);

      for (SpawnCondition condition : rule.conditions()) {
         if (WHERE.contains(condition.name()) && !condition.test(context)) {
            return "condition " + condition.name();
         }
      }

      if (Math.sqrt(level.getSharedSpawnPos().distToCenterSqr(point)) < 24.0) {
         return "within 24 blocks of the world spawn";
      }

      if (nearHome(level, point, settings.guard())) {
         return "within " + (int)settings.guard().homeRadius() + " blocks of a player's home";
      }

      String capped = capRefusal(rule, point, census, settings, positions(players), level.getSeed(), level.getGameTime());
      if (capped != null) {
         return capped;
      }

      for (ServerPlayer player : players) {
         if (seenBy(level, player, point.add(0.0, rule.type().getHeight() * 0.5, 0.0))) {
            return "in a player's view";
         }
      }

      String placed = rule.placement().refusal(level, rule.type(), at, level.random);
      if (placed != null) {
         return placed;
      }

      if (rule.placement() == Placement.AIR && species != null && !SpawnSpecies.flies(species)) {
         return "an air spawn for a species that does not fly";
      }

      for (SpawnCondition condition : rule.conditions()) {
         if (!WHERE.contains(condition.name()) && !condition.test(context)) {
            return "condition " + condition.name();
         }
      }

      return null;
   }

   static boolean nearHome(ServerLevel level, Vec3 point, SpawnSettings.Guard guard) {
      if (guard.homeRadius() <= 0.0) {
         return false;
      }

      for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
         BlockPos home = player.getRespawnPosition();
         if (home != null
            && player.getRespawnDimension() == level.dimension()
            && guard.guards(point.x, point.z, home.getX() + 0.5, home.getZ() + 0.5)) {
            return true;
         }
      }

      return false;
   }

   static boolean seenBy(ServerLevel level, ServerPlayer player, Vec3 point) {
      Vec3 eye = player.getEyePosition();
      Vec3 to = point.subtract(eye);
      double distance = to.length();
      if (!(distance > 128.0) && !(distance < 1.0E-6)) {
         double cos = player.getViewVector(1.0F).dot(to.scale(1.0 / distance));
         return cos < Math.cos(Math.toRadians(70.0))
            ? false
            : level.clip(new ClipContext(eye, point, Block.VISUAL, Fluid.NONE, player)).getType() == Type.MISS;
      } else {
         return false;
      }
   }

   public static int spawnGroup(ServerLevel level, SpawnRule rule, BlockPos at, Census census, boolean capped) {
      RandomSource random = level.random;
      SpeciesProfile species = SpawnSpecies.profileOf(rule.type(), level);
      int max = species != null && !SpawnSpecies.livesInGroups(species) ? 1 : rule.groupMax();
      int min = Math.min(rule.groupMin(), max);
      int count = Mth.randomBetweenInclusive(random, min, max);
      SpawnSettings settings = SpawnSettings.of(rule.host());
      List<Vec3> players = positions(watchers(level));
      long seed = level.getSeed();
      long time = level.getGameTime();
      if (capped && capRefusal(rule, Vec3.atBottomCenterOf(at), census, settings, players, seed, time) != null) {
         return 0;
      }

      int spawned = spawnOne(level, rule, at, census) ? 1 : 0;
      if (spawned == 0) {
         return 0;
      }

      for (int tries = 0; tries < 3 && spawned < count; tries++) {
         int x = at.getX();
         int z = at.getZ();

         for (int hop = 0; hop < 4 && spawned < count; hop++) {
            x += random.nextInt(6) - random.nextInt(6);
            z += random.nextInt(6) - random.nextInt(6);
            BlockPos next = rule.placement().hop(level, rule.type(), new BlockPos(x, at.getY(), z));
            if (next != null && rule.placement().refusal(level, rule.type(), next, random) == null) {
               if (capped && capRefusal(rule, Vec3.atBottomCenterOf(next), census, settings, players, seed, time) != null) {
                  return spawned;
               }

               if (spawnOne(level, rule, next, census)) {
                  spawned++;
               }
            }
         }
      }

      return spawned;
   }

   private static boolean spawnOne(ServerLevel level, SpawnRule rule, BlockPos at, Census census) {
      Entity made = rule.type().create(level);
      if (made instanceof Mob mob) {
         mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
         if (mob instanceof FaunaBase creature) {
            creature.presetSpawnGenetics(rule.genetics());
            if (rule.variant() != null) {
               CreatureVariant variant = CreatureVariants.get(rule.variant());
               if (variant != null && variant.type() == rule.type()) {
                  creature.applyVariant(variant);
               }
            }
         }

         Entities.finalizeSpawn(mob, level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL);
         level.addFreshEntityWithPassengers(mob);
         if (mob instanceof FaunaBase creature && rule.placement() == Placement.AIR) {
            creature.getLocomotionOrchestrator().requestTransition(LocomotionTransition.TAKEOFF, ModeChangeCause.SPAWN);
         }

         census.add(Census.entryOf(mob, SpeciesAssets.hostOf(mob)));
         return true;
      } else {
         if (made != null) {
            made.discard();
         }

         return false;
      }
   }

   public static List<String> explain(ServerLevel level, SpawnRule rule, BlockPos at) {
      List<String> out = new ArrayList<>();
      Census census = Census.of(level);
      SpawnSettings settings = SpawnSettings.of(rule.host());
      List<Vec3> players = positions(watchers(level));
      double abundance = AbundanceField.of(level.getSeed(), rule.typeId(), at.getX(), at.getZ(), level.getGameTime(), settings.abundance());
      out.add(
         "roostless "
            + census.roostless(rule.host())
            + "/"
            + roostlessCapNow(level, rule.host(), settings, players.size())
            + " | "
            + rule.typeId()
            + " "
            + census.roostlessOfType(rule.typeId())
            + "/"
            + SpawnSettings.scaled(settings.speciesRoostlessCap(players.size()), abundance)
            + String.format(Locale.ROOT, " | abundance here %.0f%%", 100.0 * abundance)
            + (settings.enabled() ? "" : " (spawning off for " + rule.host() + ")")
      );
      if (rule.beacon() != null) {
         out.add(
            "beacon: spawns only within "
               + rule.beacon().radius()
               + " of a wild "
               + rule.typeId()
               + ", once in every "
               + rule.beacon().everyTicks()
               + " ticks of its life, up to "
               + rule.beacon().maxKin()
               + " kin"
         );
      }

      String capped = capRefusal(rule, Vec3.atBottomCenterOf(at), census, settings, players, level.getSeed(), level.getGameTime());
      out.add("caps: " + (capped == null ? "room" : capped));
      String placed = rule.placement().refusal(level, rule.type(), at, level.random);
      out.add("placement " + rule.placement().name().toLowerCase(Locale.ROOT) + ": " + (placed == null ? "ok" : placed));
      out.add("home guard " + (int)settings.guard().homeRadius() + ": " + (nearHome(level, Vec3.atBottomCenterOf(at), settings.guard()) ? "FAIL" : "clear"));
      SpeciesProfile species = SpawnSpecies.profileOf(rule.type(), level);
      ServerPlayer nearest = level.getNearestPlayer(at.getX(), at.getY(), at.getZ(), -1.0, false) instanceof ServerPlayer p ? p : null;
      SpawnContext context = new SpawnContext(level, at, rule, species, census, nearest, level.random);

      for (SpawnCondition condition : rule.conditions()) {
         out.add(condition.name() + ": " + (condition.test(context) ? "pass" : "FAIL"));
      }

      if (rule.conditions().isEmpty()) {
         out.add("no conditions");
      }

      List<SpawnRule.Special> held = rule.held(context);

      for (SpawnRule.Special special : rule.specials()) {
         boolean holds = held.contains(special);
         out.add(
            "special "
               + special.condition().name()
               + (special.weight() > 0 ? " +" + special.weight() : "")
               + (special.set().equals(SpawnRule.Overrides.NONE) ? "" : " (sets)")
               + ": "
               + (holds ? "holds" : "not now")
         );
      }

      SpawnRule now = rule.resolve(held);
      out.add(
         String.format(
            Locale.ROOT,
            "weight %d -> %d: %.0f in 100 | group %d-%d%s",
            rule.weight(),
            now.weight(),
            100.0F * now.chance(),
            now.groupMin(),
            now.groupMax(),
            now.variant() == null ? "" : " | variant " + now.variant()
         )
      );
      if (now.variant() != null) {
         CreatureVariant variant = CreatureVariants.get(now.variant());
         out.add(
            "variant "
               + now.variant()
               + ": "
               + (variant == null ? "not loaded (spawns plain)" : (variant.type() != rule.type() ? "of another entity type (spawns plain)" : "loaded"))
         );
      }

      return out;
   }

   private record Candidate(SpawnRule rule, BlockPos at) {
   }
}

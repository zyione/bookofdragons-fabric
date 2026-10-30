package net.magister.liberanimalium.spawn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.api.SpeciesProfiles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class SpawnSpecies {
   static final String OWN_KIND = "#fauna:same_species";
   private static final Map<EntityType<?>, String> IDS = new ConcurrentHashMap<>();

   private SpawnSpecies() {
   }

   @Nullable
   public static String idOf(EntityType<?> type, ServerLevel level) {
      String known = IDS.get(type);
      if (known != null) {
         return known.isEmpty() ? null : known;
      }

      Entity probe = type.create(level);
      String id = probe instanceof FaunaBase creature ? creature.speciesId() : "";
      if (probe != null) {
         probe.discard();
      }

      IDS.put(type, id);
      return id.isEmpty() ? null : id;
   }

   @Nullable
   public static SpeciesProfile profileOf(EntityType<?> type, ServerLevel level) {
      String id = idOf(type, level);
      return id == null ? null : SpeciesProfiles.get(id);
   }

   public static boolean livesInGroups(@Nullable SpeciesProfile profile) {
      if (profile == null) {
         return false;
      }

      for (SpeciesProfile.RelationRule rule : profile.relations()) {
         if ("#fauna:same_species".equals(rule.targets())) {
            return rule.type() == RelationType.PACK;
         }
      }

      return false;
   }

   public static boolean flies(@Nullable SpeciesProfile profile) {
      return profile != null && profile.movement().cost().get(MoveMode.AIR) != null;
   }
}

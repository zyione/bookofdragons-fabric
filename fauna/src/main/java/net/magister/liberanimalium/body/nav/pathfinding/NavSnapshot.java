package net.magister.liberanimalium.body.nav.pathfinding;

import java.util.EnumMap;
import java.util.Map;
import net.magister.liberanimalium.body.locomotion.DiveModule;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.move.MoveTuning;
import net.magister.liberanimalium.interop.Decks;
import net.magister.liberanimalium.interop.SubLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

public final class NavSnapshot {
   private NavSnapshot() {
   }

   public static FaunaNeighborSource of(FaunaBase body, BlockPos goal) {
      Vec3 bodyAt = body.position();
      Object deck = Decks.of(body);
      if (deck != null && SubLevels.inPlot(body.level(), Vec3.atBottomCenterOf(goal))) {
         Vec3 place = Decks.place(deck, bodyAt);
         if (place != null) {
            bodyAt = place;
         }
      }

      BlockPos from = BlockPos.containing(bodyAt);
      int radius = (int)Math.ceil(body.navRange()) + 24;
      BlockPos min = from.offset(-radius, -radius, -radius);
      BlockPos max = from.offset(radius, radius, radius);
      PathNavigationRegion region = new PathNavigationRegion(body.level(), min, max);
      Map<MoveMode, Float> costs = body.effectiveModeCost();
      return new FaunaNeighborSource(
         body,
         region,
         min,
         max,
         body.currentMode(),
         Float.isFinite(costs.get(MoveMode.AIR)),
         Float.isFinite(costs.get(MoveMode.WATER)),
         body.componentRegistry.get(DiveModule.class) != null,
         body.profile().opensDoors,
         body.hopHeight(),
         withWater(body.species().movement().hazards(), costs),
         bodyAt
      );
   }

   static Map<BlockPathTypes, Float> withWater(Map<BlockPathTypes, Float> hazards, Map<MoveMode, Float> costs) {
      if (hazards.containsKey(BlockPathTypes.WATER)) {
         return hazards;
      } else {
         float water = costs.getOrDefault(MoveMode.WATER, Float.POSITIVE_INFINITY);
         float ground = costs.getOrDefault(MoveMode.GROUND, Float.POSITIVE_INFINITY);
         if (Float.isFinite(water) && Float.isFinite(ground) && !(ground <= 0.0F)) {
            Map<BlockPathTypes, Float> out = new EnumMap<>(BlockPathTypes.class);
            out.putAll(hazards);
            out.put(BlockPathTypes.WATER, MoveTuning.hazardMalus(Math.max(0.0F, water / ground - 1.0F)));
            return out;
         } else {
            return hazards;
         }
      }
   }
}

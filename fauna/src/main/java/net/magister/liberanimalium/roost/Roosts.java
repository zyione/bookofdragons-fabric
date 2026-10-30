package net.magister.liberanimalium.roost;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;

public final class Roosts {
   private static final Set<ResourceKey<PoiType>> TYPES = ConcurrentHashMap.newKeySet();

   private Roosts() {
   }

   public static void announce(ResourceKey<PoiType> type) {
      TYPES.add(type);
   }

   static boolean isRoost(Holder<PoiType> type) {
      return type.unwrapKey().map(TYPES::contains).orElse(false);
   }

   public static List<BlockPos> nearest(ServerLevel level, BlockPos origin) {
      return nearest(level, origin, 96, 4);
   }

   public static List<BlockPos> nearest(ServerLevel level, BlockPos origin, int radius, int limit) {
      return TYPES.isEmpty()
         ? List.of()
         : level.getPoiManager().findAllClosestFirstWithType(Roosts::isRoost, pos -> true, origin, radius, Occupancy.ANY).limit(limit).<BlockPos>map(Pair::getSecond).toList();
   }
}

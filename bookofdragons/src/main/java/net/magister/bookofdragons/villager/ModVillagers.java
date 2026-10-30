package net.magister.bookofdragons.villager;

import com.google.common.collect.ImmutableSet;
import net.magister.bookofdragons.block.ModBlocks;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;

public class ModVillagers {
   public static final Registrar<PoiType> POI_TYPES = Services.registries().create(Registries.POINT_OF_INTEREST_TYPE, "bookofdragons");
   public static final Registrar<VillagerProfession> VILLAGER_PROFESSIONS = Services.registries().create(Registries.VILLAGER_PROFESSION, "bookofdragons");
   public static final RegEntry<PoiType> VIKING_POI = POI_TYPES.register(
      "viking_poi", () -> new PoiType(ImmutableSet.copyOf(((Block)ModBlocks.VIKING_ANVIL.get()).getStateDefinition().getPossibleStates()), 1, 1)
   );
   public static final RegEntry<PoiType> ROOST_POI = POI_TYPES.register(
      "roost", () -> new PoiType(ImmutableSet.copyOf(((Block)ModBlocks.ROOST_CONTROLLER.get()).getStateDefinition().getPossibleStates()), 0, 1)
   );
   public static final RegEntry<VillagerProfession> HOOLIGAN = VILLAGER_PROFESSIONS.register(
      "hooligan",
      () -> new VillagerProfession(
         "hooligan",
         holder -> holder.value() == VIKING_POI.get(),
         holder -> holder.value() == VIKING_POI.get(),
         ImmutableSet.of(),
         ImmutableSet.of(),
         SoundEvents.VILLAGER_WORK_WEAPONSMITH
      )
   );

   public static void register() {
   }
}

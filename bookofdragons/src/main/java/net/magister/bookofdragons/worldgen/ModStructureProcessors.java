package net.magister.bookofdragons.worldgen;

import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

public class ModStructureProcessors {
   public static final Registrar<StructureProcessorType<?>> PROCESSORS = Services.registries().create(Registries.STRUCTURE_PROCESSOR, "bookofdragons");
   public static final RegEntry<StructureProcessorType<DragonSpawnProcessor>> DRAGON_SPAWN = PROCESSORS.register(
      "dragon_spawn", () -> () -> DragonSpawnProcessor.CODEC
   );

   public static void register() {
   }
}

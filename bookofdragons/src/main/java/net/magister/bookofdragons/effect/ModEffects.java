package net.magister.bookofdragons.effect;

import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

public final class ModEffects {
   public static final Registrar<MobEffect> MOB_EFFECTS = Services.registries().create(Registries.MOB_EFFECT, "bookofdragons");
   public static final RegEntry<MobEffect> PARALYSIS = MOB_EFFECTS.register("paralysis", ParalysisEffect::new);

   private ModEffects() {
   }

   public static void register() {
   }
}

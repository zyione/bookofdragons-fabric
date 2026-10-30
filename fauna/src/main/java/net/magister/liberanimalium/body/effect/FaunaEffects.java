package net.magister.liberanimalium.body.effect;

import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

public final class FaunaEffects {
   public static final Registrar<MobEffect> MOB_EFFECTS = Services.registries().create(Registries.MOB_EFFECT, "liberanimalium");
   public static final RegEntry<MobEffect> INJURY = MOB_EFFECTS.register("injury", InjuryEffect::new);

   public static void register() {
   }

   private FaunaEffects() {
   }
}

package net.magister.bookofdragons.effect;

import net.magister.bookofdragons.compat.Stillness;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class ParalysisEffect extends MobEffect {
   public ParalysisEffect() {
      super(MobEffectCategory.HARMFUL, 13217850);
      Stillness.addTo(this, "paralysis");
   }
}

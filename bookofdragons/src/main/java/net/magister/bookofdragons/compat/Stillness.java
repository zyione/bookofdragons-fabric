package net.magister.bookofdragons.compat;

import net.magister.bookofdragons.compat.v20.StillnessImpl;
import net.minecraft.world.effect.MobEffect;

public final class Stillness {
   private Stillness() {
   }

   public static void addTo(MobEffect effect, String key) {
      StillnessImpl.addTo(effect, key);
   }
}

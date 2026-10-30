package net.magister.liberanimalium.attack.impact;

import java.util.LinkedHashMap;
import java.util.Map;
import net.magister.liberanimalium.attack.data.ImpactConfig;
import net.magister.liberanimalium.attack.entity.AttackProjectileEntity;
import net.magister.liberanimalium.attack.impact.builtin.ExplosionEffect;
import net.magister.liberanimalium.attack.impact.builtin.ExtinguishEffect;
import net.magister.liberanimalium.attack.impact.builtin.NoneEffect;
import net.magister.liberanimalium.attack.impact.builtin.SlideEffect;
import net.minecraft.world.phys.HitResult;

public interface ImpactEffect {
   Map<String, ImpactEffect> REGISTRY = init();

   boolean apply(AttackProjectileEntity var1, HitResult var2, ImpactConfig var3);

   private static Map<String, ImpactEffect> init() {
      LinkedHashMap<String, ImpactEffect> m = new LinkedHashMap<>();
      m.put("explosion", new ExplosionEffect());
      m.put("extinguish", new ExtinguishEffect());
      m.put("slide", new SlideEffect());
      m.put("none", new NoneEffect());
      return m;
   }

   static ImpactEffect byType(String type) {
      return REGISTRY.get(type);
   }

   static void register(String type, ImpactEffect effect) {
      REGISTRY.put(type, effect);
   }
}

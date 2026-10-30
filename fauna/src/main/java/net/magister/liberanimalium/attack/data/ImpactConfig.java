package net.magister.liberanimalium.attack.data;

import com.mojang.serialization.Codec;
import java.util.LinkedHashMap;
import java.util.Map;
import net.magister.liberanimalium.attack.data.impact.ExplosionImpact;
import net.magister.liberanimalium.attack.data.impact.ExtinguishImpact;
import net.magister.liberanimalium.attack.data.impact.NoneImpact;
import net.magister.liberanimalium.attack.data.impact.SlideImpact;

public interface ImpactConfig {
   Map<String, Codec<? extends ImpactConfig>> REGISTRY = init();
   Codec<ImpactConfig> CODEC = Codec.STRING.dispatch("type", ImpactConfig::type, key -> {
      Codec<? extends ImpactConfig> c = REGISTRY.get(key);
      if (c == null) {
         throw new IllegalArgumentException("Unknown impact type: " + key);
      } else {
         return c;
      }
   });

   String type();

   private static Map<String, Codec<? extends ImpactConfig>> init() {
      LinkedHashMap<String, Codec<? extends ImpactConfig>> m = new LinkedHashMap<>();
      m.put("explosion", ExplosionImpact.CODEC);
      m.put("extinguish", ExtinguishImpact.CODEC);
      m.put("slide", SlideImpact.CODEC);
      m.put("none", NoneImpact.CODEC);
      return m;
   }

   static void register(String type, Codec<? extends ImpactConfig> codec) {
      REGISTRY.put(type, codec);
   }
}

package net.magister.liberanimalium.attack.movement;

import com.mojang.serialization.Codec;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface MovementModifier {
   Codec<MovementModifier> CODEC = Codec.STRING.dispatch("type", MovementModifier::type, key -> {
      Codec<? extends MovementModifier> c = MovementModifier.Types.BY_NAME.get(key);
      if (c == null) {
         throw new IllegalArgumentException("Unknown movement modifier type: " + key);
      } else {
         return c;
      }
   });

   String type();

   Vec3 apply(Vec3 var1, @Nullable Entity var2, int var3);

   default Vec3 apply(Vec3 velocity, @Nullable Entity entity, int tick, RandomSource random) {
      return this.apply(velocity, entity, tick);
   }

   static void register(String type, Codec<? extends MovementModifier> codec) {
      MovementModifier.Types.BY_NAME.put(type, codec);
   }

   final class Types {
      static final Map<String, Codec<? extends MovementModifier>> BY_NAME = init();

      private Types() {
      }

      private static Map<String, Codec<? extends MovementModifier>> init() {
         LinkedHashMap<String, Codec<? extends MovementModifier>> m = new LinkedHashMap<>();
         m.put("drag", DragMod.CODEC);
         m.put("gravity", GravityMod.CODEC);
         m.put("buoyancy", BuoyancyMod.CODEC);
         m.put("vortex", VortexMod.CODEC);
         m.put("ring", RingMod.CODEC);
         m.put("turbulence", TurbulenceMod.CODEC);
         return m;
      }
   }
}

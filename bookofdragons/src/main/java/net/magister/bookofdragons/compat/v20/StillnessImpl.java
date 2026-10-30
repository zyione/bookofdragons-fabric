package net.magister.bookofdragons.compat.v20;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class StillnessImpl {
   private StillnessImpl() {
   }

   public static void addTo(MobEffect effect, String key) {
      Operation total = Operation.MULTIPLY_TOTAL;
      effect.addAttributeModifier(Attributes.MOVEMENT_SPEED, uuid(key, "movement_speed"), -1.0, total);
      effect.addAttributeModifier(Attributes.FLYING_SPEED, uuid(key, "flying_speed"), -1.0, total);
   }

   private static String uuid(String key, String attribute) {
      String name = "bookofdragons:" + key + "/" + attribute;
      return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
   }
}

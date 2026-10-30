package net.magister.liberanimalium.attack.data.delivery;

import com.mojang.serialization.Codec;

public sealed interface DeliveryConfig permits StreamDelivery, ChargedDelivery, InstantDelivery, BurstDelivery {
   Codec<DeliveryConfig> CODEC = Codec.STRING.dispatch("type", DeliveryConfig::type, key -> {
      return switch (key) {
         case "stream" -> StreamDelivery.CODEC;
         case "charged" -> ChargedDelivery.CODEC;
         case "instant" -> InstantDelivery.CODEC;
         case "burst" -> BurstDelivery.CODEC;
         default -> throw new IllegalArgumentException("Unknown delivery type: " + key);
      };
   });

   String type();

   int cooldownTicks();
}

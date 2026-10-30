package net.magister.liberanimalium.network;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class ControlMessageSecondaryAbility {
   public static void encode(ControlMessageSecondaryAbility message, FriendlyByteBuf buffer) {
   }

   public static ControlMessageSecondaryAbility decode(FriendlyByteBuf buffer) {
      return new ControlMessageSecondaryAbility();
   }

   public static void handle(ControlMessageSecondaryAbility message, PacketContext context) {
      context.enqueueWork(() -> {
         FaunaBase body = RiderPackets.driven(context.sender());
         if (body != null) {
            body.activateSecondaryAbility(context.sender());
         }
      });
   }
}

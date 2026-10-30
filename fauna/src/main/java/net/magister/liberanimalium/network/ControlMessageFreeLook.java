package net.magister.liberanimalium.network;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.RiderInputComponent;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class ControlMessageFreeLook {
   private final boolean isFreeLooking;

   public ControlMessageFreeLook(boolean isFreeLooking) {
      this.isFreeLooking = isFreeLooking;
   }

   public static void encode(ControlMessageFreeLook message, FriendlyByteBuf buffer) {
      buffer.writeBoolean(message.isFreeLooking);
   }

   public static ControlMessageFreeLook decode(FriendlyByteBuf buffer) {
      return new ControlMessageFreeLook(buffer.readBoolean());
   }

   public static void handle(ControlMessageFreeLook message, PacketContext context) {
      context.enqueueWork(() -> {
         FaunaBase body = RiderPackets.driven(context.sender());
         if (body != null) {
            RiderInputComponent riderInput = body.componentRegistry.get(RiderInputComponent.class);
            if (riderInput != null) {
               riderInput.setFreeLooking(message.isFreeLooking);
            }
         }
      });
   }
}

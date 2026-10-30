package net.magister.liberanimalium.network;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.RiderInputComponent;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class ControlMessageBrake {
   private final boolean isBraking;

   public ControlMessageBrake(boolean isBraking) {
      this.isBraking = isBraking;
   }

   public static void encode(ControlMessageBrake message, FriendlyByteBuf buffer) {
      buffer.writeBoolean(message.isBraking);
   }

   public static ControlMessageBrake decode(FriendlyByteBuf buffer) {
      return new ControlMessageBrake(buffer.readBoolean());
   }

   public static void handle(ControlMessageBrake message, PacketContext context) {
      context.enqueueWork(() -> {
         FaunaBase body = RiderPackets.driven(context.sender());
         if (body != null) {
            RiderInputComponent riderInput = body.componentRegistry.get(RiderInputComponent.class);
            if (riderInput != null) {
               riderInput.setBraking(message.isBraking);
            }
         }
      });
   }
}

package net.magister.liberanimalium.network;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.RiderInputComponent;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class ControlMessageBoost {
   private final boolean isBoosting;

   public ControlMessageBoost(boolean isBoosting) {
      this.isBoosting = isBoosting;
   }

   public static void encode(ControlMessageBoost message, FriendlyByteBuf buffer) {
      buffer.writeBoolean(message.isBoosting);
   }

   public static ControlMessageBoost decode(FriendlyByteBuf buffer) {
      return new ControlMessageBoost(buffer.readBoolean());
   }

   public static void handle(ControlMessageBoost message, PacketContext context) {
      context.enqueueWork(() -> {
         FaunaBase body = RiderPackets.driven(context.sender());
         if (body != null) {
            RiderInputComponent riderInput = body.componentRegistry.get(RiderInputComponent.class);
            if (riderInput != null) {
               riderInput.setBoosting(message.isBoosting);
            }
         }
      });
   }
}

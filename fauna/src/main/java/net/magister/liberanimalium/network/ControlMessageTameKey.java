package net.magister.liberanimalium.network;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.RiderInputComponent;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class ControlMessageTameKey {
   private final int letter;

   public ControlMessageTameKey(int letter) {
      this.letter = letter;
   }

   public static void encode(ControlMessageTameKey message, FriendlyByteBuf buffer) {
      buffer.writeByte(message.letter);
   }

   public static ControlMessageTameKey decode(FriendlyByteBuf buffer) {
      return new ControlMessageTameKey(buffer.readByte());
   }

   public static void handle(ControlMessageTameKey message, PacketContext context) {
      context.enqueueWork(() -> {
         FaunaBase body = RiderPackets.ridden(context.sender());
         if (body != null) {
            RiderInputComponent riderInput = body.componentRegistry.get(RiderInputComponent.class);
            if (riderInput != null) {
               riderInput.pressTameKey(message.letter);
            }
         }
      });
   }
}

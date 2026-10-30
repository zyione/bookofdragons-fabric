package net.magister.liberanimalium.network;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.RollComponent;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class ControlMessageDodge {
   private final int side;
   private final float gain;

   public ControlMessageDodge(int side, float gain) {
      this.side = side;
      this.gain = gain;
   }

   public static void encode(ControlMessageDodge message, FriendlyByteBuf buffer) {
      buffer.writeByte(message.side);
      buffer.writeFloat(message.gain);
   }

   public static ControlMessageDodge decode(FriendlyByteBuf buffer) {
      return new ControlMessageDodge(buffer.readByte(), net.magister.liberanimalium.platform.net.PacketValues.finiteFloat(buffer));
   }

   public static void handle(ControlMessageDodge message, PacketContext context) {
      context.enqueueWork(() -> {
         FaunaBase body = RiderPackets.driven(context.sender());
         if (body != null) {
            RollComponent roll = body.componentRegistry.get(RollComponent.class);
            if (roll != null) {
               roll.serverDodge(message.side, message.gain);
            }
         }
      });
   }
}

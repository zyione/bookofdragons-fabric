package net.magister.liberanimalium.network;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.GripComponent;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class ControlMessageGrab {
   private final boolean down;

   public ControlMessageGrab(boolean down) {
      this.down = down;
   }

   public static void encode(ControlMessageGrab message, FriendlyByteBuf buffer) {
      buffer.writeBoolean(message.down);
   }

   public static ControlMessageGrab decode(FriendlyByteBuf buffer) {
      return new ControlMessageGrab(buffer.readBoolean());
   }

   public static void handle(ControlMessageGrab message, PacketContext context) {
      context.enqueueWork(() -> {
         FaunaBase body = RiderPackets.driven(context.sender());
         if (body != null) {
            GripComponent grip = body.grip();
            if (grip != null) {
               grip.riderKey(message.down);
            }
         }
      });
   }
}

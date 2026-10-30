package net.magister.liberanimalium.network;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class ControlMessageRoll {
   private final boolean isRolling;
   private final float roll;

   public ControlMessageRoll(boolean isRolling, float roll) {
      this.isRolling = isRolling;
      this.roll = roll;
   }

   public boolean isRolling() {
      return this.isRolling;
   }

   public float getRoll() {
      return this.roll;
   }

   public static void encode(ControlMessageRoll message, FriendlyByteBuf buffer) {
      buffer.writeBoolean(message.isRolling);
      buffer.writeFloat(message.roll);
   }

   public static ControlMessageRoll decode(FriendlyByteBuf buffer) {
      return new ControlMessageRoll(buffer.readBoolean(), net.magister.liberanimalium.platform.net.PacketValues.finiteFloat(buffer));
   }

   public static void handle(ControlMessageRoll message, PacketContext context) {
      context.enqueueWork(() -> {
         FaunaBase body = RiderPackets.driven(context.sender());
         if (body != null) {
            FaunaFlightRotationComponent rotComp = body.componentRegistry.get(FaunaFlightRotationComponent.class);
            if (rotComp != null) {
               rotComp.setRolling(message.isRolling());
               rotComp.setRoll(message.getRoll());
            }
         }
      });
   }
}

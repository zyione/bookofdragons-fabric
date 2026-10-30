package net.magister.liberanimalium.network;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class ControlMessageThrottleSync {
   private final float throttleLevel;
   private final boolean isAccelerating;
   private final boolean isDecelerating;

   public ControlMessageThrottleSync(float throttleLevel, boolean isAccelerating, boolean isDecelerating) {
      this.throttleLevel = throttleLevel;
      this.isAccelerating = isAccelerating;
      this.isDecelerating = isDecelerating;
   }

   public static void encode(ControlMessageThrottleSync message, FriendlyByteBuf buffer) {
      buffer.writeFloat(message.throttleLevel);
      buffer.writeBoolean(message.isAccelerating);
      buffer.writeBoolean(message.isDecelerating);
   }

   public static ControlMessageThrottleSync decode(FriendlyByteBuf buffer) {
      return new ControlMessageThrottleSync(net.magister.liberanimalium.platform.net.PacketValues.finiteFloat(buffer), buffer.readBoolean(), buffer.readBoolean());
   }

   public static void handle(ControlMessageThrottleSync message, PacketContext context) {
      context.enqueueWork(() -> {
         FaunaBase body = RiderPackets.driven(context.sender());
         if (body != null) {
            body.setThrottleLevel(message.throttleLevel);
            body.setAccelerating(message.isAccelerating);
            body.setDecelerating(message.isDecelerating);
         }
      });
   }
}

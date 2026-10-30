package net.magister.liberanimalium.network;

import net.magister.liberanimalium.body.state.RiderMotion;
import net.magister.liberanimalium.body.state.TransportMode;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class FlightTellsSyncMessage {
   private final int entityId;
   private final int transportOrdinal;
   private final float pitch;
   private final boolean isDiveBreaking;
   private final float motionX;
   private final float motionY;
   private final float motionZ;

   public FlightTellsSyncMessage(int entityId, int transportOrdinal, float pitch, boolean isDiveBreaking, float motionX, float motionY, float motionZ) {
      this.entityId = entityId;
      this.transportOrdinal = transportOrdinal;
      this.pitch = pitch;
      this.isDiveBreaking = isDiveBreaking;
      this.motionX = motionX;
      this.motionY = motionY;
      this.motionZ = motionZ;
   }

   public static void encode(FlightTellsSyncMessage message, FriendlyByteBuf buffer) {
      buffer.writeInt(message.entityId);
      buffer.writeByte(message.transportOrdinal);
      buffer.writeFloat(message.pitch);
      buffer.writeBoolean(message.isDiveBreaking);
      buffer.writeFloat(message.motionX);
      buffer.writeFloat(message.motionY);
      buffer.writeFloat(message.motionZ);
   }

   public static FlightTellsSyncMessage decode(FriendlyByteBuf buffer) {
      return new FlightTellsSyncMessage(
         buffer.readInt(), buffer.readByte(), net.magister.liberanimalium.platform.net.PacketValues.finiteFloat(buffer), buffer.readBoolean(), net.magister.liberanimalium.platform.net.PacketValues.finiteFloat(buffer), net.magister.liberanimalium.platform.net.PacketValues.finiteFloat(buffer), net.magister.liberanimalium.platform.net.PacketValues.finiteFloat(buffer)
      );
   }

   public static void handle(FlightTellsSyncMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         ServerPlayer player = context.sender();
         if (player != null && player.level().getEntity(message.entityId) instanceof FaunaBase body && body.getControllingPassenger() == player) {
            body.getLocomotionOrchestrator().acceptClientMode(TransportMode.fromOrdinal(message.transportOrdinal));
            body.setIsDiveBreaking(message.isDiveBreaking);
            body.getEntityData().set(FaunaBase.DATA_SYNCED_PITCH, message.pitch);
            if (Float.isFinite(message.motionX) && Float.isFinite(message.motionY) && Float.isFinite(message.motionZ)) {
               RiderMotion.set(body, message.motionX, message.motionY, message.motionZ);
            }

            body.setHorizontalSpeed((float)RiderMotion.of(body).horizontalDistance());
         }
      });
   }
}

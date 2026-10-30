package net.magister.liberanimalium.network;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public class RidePlaceMessage {
   @Nullable
   private final Vec3 place;

   public RidePlaceMessage(@Nullable Vec3 place) {
      this.place = place;
   }

   public static void encode(RidePlaceMessage message, FriendlyByteBuf buffer) {
      buffer.writeBoolean(message.place != null);
      if (message.place != null) {
         buffer.writeDouble(message.place.x);
         buffer.writeDouble(message.place.y);
         buffer.writeDouble(message.place.z);
      }
   }

   public static RidePlaceMessage decode(FriendlyByteBuf buffer) {
      return !buffer.readBoolean() ? new RidePlaceMessage(null) : new RidePlaceMessage(new Vec3(net.magister.liberanimalium.platform.net.PacketValues.finiteDouble(buffer), net.magister.liberanimalium.platform.net.PacketValues.finiteDouble(buffer), net.magister.liberanimalium.platform.net.PacketValues.finiteDouble(buffer)));
   }

   public static void handle(RidePlaceMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         FaunaBase body = RiderPackets.driven(context.sender());
         if (body != null) {
            body.acceptRidePlace(message.place);
         }
      });
   }
}

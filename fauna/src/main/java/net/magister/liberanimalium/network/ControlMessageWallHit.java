package net.magister.liberanimalium.network;

import net.magister.liberanimalium.body.BodyPlans;
import net.magister.liberanimalium.body.locomotion.RideTuning;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

public class ControlMessageWallHit {
   private final float severity;

   public ControlMessageWallHit(float severity) {
      this.severity = severity;
   }

   public static void encode(ControlMessageWallHit message, FriendlyByteBuf buffer) {
      buffer.writeFloat(message.severity);
   }

   public static ControlMessageWallHit decode(FriendlyByteBuf buffer) {
      return new ControlMessageWallHit(net.magister.liberanimalium.platform.net.PacketValues.finiteFloat(buffer));
   }

   public static void handle(ControlMessageWallHit message, PacketContext context) {
      context.enqueueWork(() -> {
         FaunaBase body = RiderPackets.driven(context.sender());
         if (body != null) {
            float severity = Mth.clamp(message.severity, 0.0F, 2.5F);
            if (!(severity <= 0.0F)) {
               int hold = BodyPlans.pushOffTicks(body.animSpeciesId());
               body.wallHitPoseFor(hold + RideTuning.pushOffTicks(body));
            }
         }
      });
   }
}

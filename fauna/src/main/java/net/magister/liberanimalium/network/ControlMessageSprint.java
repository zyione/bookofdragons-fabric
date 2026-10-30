package net.magister.liberanimalium.network;

import net.magister.liberanimalium.body.data.CrippledFeature;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.IncapacitationComponent;
import net.magister.liberanimalium.chassis.component.RiderInputComponent;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class ControlMessageSprint {
   private final boolean isSprinting;

   public ControlMessageSprint(boolean isSprinting) {
      this.isSprinting = isSprinting;
   }

   public static void encode(ControlMessageSprint message, FriendlyByteBuf buffer) {
      buffer.writeBoolean(message.isSprinting);
   }

   public static ControlMessageSprint decode(FriendlyByteBuf buffer) {
      return new ControlMessageSprint(buffer.readBoolean());
   }

   public static void handle(ControlMessageSprint message, PacketContext context) {
      context.enqueueWork(
         () -> {
            FaunaBase body = RiderPackets.driven(context.sender());
            if (body != null) {
               RiderInputComponent riderInput = body.componentRegistry.get(RiderInputComponent.class);
               if (riderInput != null) {
                  riderInput.setSprinting(
                     message.isSprinting
                        && body.getNeedsSystem() != null
                        && body.getNeedsSystem().canSprint()
                        && !IncapacitationComponent.refuses(body, context.sender(), CrippledFeature.RUNNING)
                  );
               }
            }
         }
      );
   }
}

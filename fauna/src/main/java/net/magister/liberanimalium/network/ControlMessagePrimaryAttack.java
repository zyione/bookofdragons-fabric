package net.magister.liberanimalium.network;

import net.magister.liberanimalium.body.data.CrippledFeature;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.IncapacitationComponent;
import net.magister.liberanimalium.chassis.component.ranged.IPrimaryAttackHandler;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class ControlMessagePrimaryAttack {
   private final boolean pressed;

   public ControlMessagePrimaryAttack(boolean pressed) {
      this.pressed = pressed;
   }

   public static void encode(ControlMessagePrimaryAttack msg, FriendlyByteBuf buf) {
      buf.writeBoolean(msg.pressed);
   }

   public static ControlMessagePrimaryAttack decode(FriendlyByteBuf buf) {
      return new ControlMessagePrimaryAttack(buf.readBoolean());
   }

   public static void handle(ControlMessagePrimaryAttack msg, PacketContext ctx) {
      ctx.enqueueWork(() -> {
         FaunaBase body = RiderPackets.driven(ctx.sender());
         if (body != null) {
            if (msg.pressed) {
               IncapacitationComponent.refuses(body, ctx.sender(), CrippledFeature.BREATH);
            }

            IPrimaryAttackHandler handler = body.componentRegistry.get(IPrimaryAttackHandler.class);
            if (handler instanceof OmniAttackHandler) {
               body.holdTrigger(msg.pressed);
            } else if (handler != null) {
               handler.handlePlayerAttack(msg.pressed);
            }
         }
      });
   }
}

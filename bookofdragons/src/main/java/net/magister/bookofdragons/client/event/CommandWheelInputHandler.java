package net.magister.bookofdragons.client.event;

import net.magister.liberanimalium.body.part.FaunaPartEntity;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.network.CycleStanceMessage;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.platform.event.ClientEvents.UseKeyResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

public final class CommandWheelInputHandler {
   private static boolean latched = false;

   private CommandWheelInputHandler() {
   }

   public static void clientInit() {
      net.magister.liberanimalium.platform.event.ClientEvents.USE_KEY.register(CommandWheelInputHandler::onUseInteraction);
      net.magister.liberanimalium.platform.event.ClientEvents.CLIENT_TICK_END.register(CommandWheelInputHandler::onClientTick);
   }

   public static UseKeyResult onUseInteraction(InteractionHand hand) {
      if (hand != InteractionHand.MAIN_HAND) {
         return UseKeyResult.PASS;
      }

      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null || mc.level == null) {
         return UseKeyResult.PASS;
      }

      if (!player.isShiftKeyDown()) {
         return UseKeyResult.PASS;
      }

      if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
         return UseKeyResult.PASS;
      }

      if (player.isPassenger()) {
         return UseKeyResult.PASS;
      }

      FaunaBase dragon = pickedOwnedDragon(mc, player);
      if (dragon == null) {
         return UseKeyResult.PASS;
      }

      if (!latched) {
         latched = true;
         FaunaNetwork.sendToServer(new CycleStanceMessage(dragon.getUUID()));
      }

      return UseKeyResult.CANCEL_WITHOUT_SWING;
   }

   public static void onClientTick() {
      if (latched) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level == null || mc.player == null || !mc.options.keyUse.isDown()) {
            latched = false;
         }
      }
   }

   private static FaunaBase pickedOwnedDragon(Minecraft mc, LocalPlayer player) {
      if (mc.hitResult instanceof EntityHitResult ehr) {
         Entity e = ehr.getEntity();
         if (e instanceof FaunaPartEntity part) {
            e = part.getParent();
         }

         if (e instanceof FaunaBase dragon) {
            if (!dragon.isTame()) {
               return null;
            } else {
               return !dragon.isOwnedBy(player) ? null : dragon;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }
}

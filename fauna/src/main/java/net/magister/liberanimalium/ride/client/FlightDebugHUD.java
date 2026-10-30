package net.magister.liberanimalium.ride.client;

import net.magister.liberanimalium.body.locomotion.LocomotionOrchestrator;
import net.magister.liberanimalium.body.locomotion.transition.WallHitTransition;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.ride.flight.rotation.FaunaCameraState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class FlightDebugHUD {
   private static boolean enabled = false;

   public static void toggle() {
      enabled = !enabled;
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void render(GuiGraphics guiGraphics, float partialTick) {
      if (enabled) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            if (mc.player.getVehicle() instanceof FaunaBase body) {
               FaunaFlightRotationComponent var16 = body.componentRegistry.get(FaunaFlightRotationComponent.class);
               byte x = 5;
               int y = 5;
               int color = 16777215;
               int headerColor = 5635925;
               int warnColor = 16733525;
               guiGraphics.drawString(mc.font, "=== 3D FLIGHT DEBUG ===", x, y, headerColor);
               y += 12;
               guiGraphics.drawString(mc.font, String.format("Body  P:%.1f  Y:%.1f", body.getXRot(), body.getYRot()), x, y, color);
               y += 10;
               if (var16 != null) {
                  float roll = var16.getRoll(partialTick);
                  guiGraphics.drawString(
                     mc.font, String.format("Roll: %.1f  isRolling: %s", roll, var16.isRolling()), x, y, var16.isRolling() ? headerColor : warnColor
                  );
               } else {
                  guiGraphics.drawString(mc.font, "RotComp: NULL", x, y, warnColor);
               }

               y += 10;
               guiGraphics.drawString(mc.font, String.format("Player  P:%.1f  Y:%.1f", mc.player.getXRot(), mc.player.getYRot()), x, y, color);
               y += 10;
               float cameraRoll = FaunaCameraState.getCurrentRoll();
               guiGraphics.drawString(mc.font, String.format("CamState Roll: %.1f", cameraRoll), x, y, color);
               y += 10;
               double speed = body.getDeltaMovement().length();
               guiGraphics.drawString(mc.font, String.format("Speed: %.3f", speed), x, y, color);
               y += 10;
               LocomotionOrchestrator orch = body.getLocomotionOrchestrator();
               WallHitTransition hit = orch.activeWallHit();
               String ride = "Ride  onGround:"
                  + (body.onGround() ? "Y" : "-")
                  + "  transition:"
                  + orch.activeTransitionName()
                  + (orch.attitudeOwned() ? "(attitude)" : "")
                  + "  mode:"
                  + body.getTransportMode()
                  + "  branch:"
                  + body.lastTravelBranch();
               guiGraphics.drawString(mc.font, ride, x, y, color);
               y += 10;
               if (hit != null) {
                  guiGraphics.drawString(
                     mc.font,
                     String.format(
                        "WallHit  sev:%.2f  hold:%d/%d  %s  %s  heading:%.0f",
                        hit.severity(),
                        hit.holdTicks(),
                        hit.holdTotal(),
                        hit.pushing() ? "PUSH" : "HOLD",
                        hit.choice(),
                        hit.targetYaw()
                     ),
                     x,
                     y,
                     warnColor
                  );
               }
            }
         }
      }
   }
}

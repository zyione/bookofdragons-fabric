package net.magister.liberanimalium.debug.client;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.chassis.component.FaunaSpineCascadeComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

public final class CascadeDebugHUD {
   private static final int COL_X = 210;
   private static final int COLOR_OK = 5635925;
   private static final int COLOR_WARN = 16733525;
   private static final int COLOR_INFO = 16777215;
   private static final int COLOR_DIM = 11184810;

   private CascadeDebugHUD() {
   }

   public static void render(GuiGraphics g, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if (mc.player.getVehicle() instanceof FaunaBase body) {
            short var17 = 210;
            int y = 5;
            g.drawString(mc.font, "=== CASCADE DEBUG ===", var17, y, 5635925);
            y += 12;
            FaunaSpineCascadeComponent cascade = body.componentRegistry.get(FaunaSpineCascadeComponent.class);
            if (cascade == null) {
               g.drawString(mc.font, "Q1 Comp: NULL  <-- BROKEN", var17, y, 16733525);
            } else {
               g.drawString(mc.font, "Q1 Comp: FOUND  segs=" + cascade.getSegments().size(), var17, y, 5635925);
               y += 10;
               g.drawString(mc.font, "Q2 ticks=" + cascade.debugTickCount, var17, y, cascade.debugTickCount > 0 ? 5635925 : 16733525);
               y += 10;
               Vec3 omega = cascade.debugLastOmega;
               boolean omegaOk = Math.abs(omega.x) > 0.01 || Math.abs(omega.y) > 0.01 || Math.abs(omega.z) > 0.01;
               g.drawString(
                  mc.font,
                  String.format("Q3 ω P:%.2f Y:%.2f R:%.2f", omega.x, omega.y, omega.z),
                  var17,
                  y,
                  omegaOk ? 16777215 : 16733525
               );
               y += 10;
               FaunaFlightRotationComponent rotComp = body.componentRegistry.get(FaunaFlightRotationComponent.class);
               if (rotComp != null) {
                  g.drawString(
                     mc.font,
                     String.format(
                        "   relRot P:%.2f Y:%.2f R:%.2f",
                        rotComp.getRelRotPitch(partialTick),
                        rotComp.getRelRotYaw(partialTick),
                        rotComp.getRelRotRoll(partialTick)
                     ),
                     var17,
                     y,
                     11184810
                  );
                  y += 10;
               }

               y += 2;
               g.drawString(mc.font, "Q4 accumulators (yaw° / pitch°):", var17, y, 11184810);
               y += 10;
               String[] probe = new String[]{"neck1", "neck6", "head0", "tail", "tail6", "tail11"};

               for (String bone : probe) {
                  Vec3 a = cascade.getAccumForBone(bone, partialTick);
                  boolean nonZero = Math.abs(a.x) > 0.05 || Math.abs(a.y) > 0.05 || Math.abs(a.z) > 0.05;
                  g.drawString(
                     mc.font,
                     String.format("  %-6s P:%5.2f Y:%5.2f R:%5.2f", bone, a.x, a.y, a.z),
                     var17,
                     y,
                     nonZero ? 5635925 : 11184810
                  );
                  y += 10;
               }

               y += 2;
               g.drawString(mc.font, "Q5/Q6: see console for bone-found log", var17, y, 11184810);
            }
         }
      }
   }
}

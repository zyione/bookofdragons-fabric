package net.magister.liberanimalium.render;

import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.body.grip.GripSpec;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.GripComponent;
import net.magister.liberanimalium.chassis.component.LimbReachComponent;
import net.magister.liberanimalium.debug.client.GripWatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Quaternionf;

final class GripPass implements RigPass {
   @Override
   public String name() {
      return "grip";
   }

   @Override
   public void solve(RigFrame frame) {
      if (frame.body instanceof FaunaBase body) {
         GripComponent grip = body.grip();
         if (grip != null) {
            GripSpec spec = grip.spec();
            if (spec.grips()) {
               float weight = grip.shownWeight(frame.partialTick);
               if (!(weight <= 0.0F)) {
                  float forced = GripWatch.weight;
                  if (!Float.isNaN(forced)) {
                     weight = forced;
                  }

                  Quaternionf attitude = frame.modelToWorld.getNormalizedRotation(new Quaternionf());
                  if (grip.box(frame.partialTick, attitude) != null) {
                     LimbReachComponent reach = LimbReachComponent.of(body);
                     if (reach != null) {
                        for (String bone : reach.applyGrip(
                           frame.model,
                           frame.drawnPosition,
                           frame.modelToWorld,
                           spec,
                           () -> grip.box(frame.partialTick, attitude),
                           weight,
                           grip.closing(),
                           frame.partialTick
                        )) {
                           FaunaProceduralAnimator.capture(frame, bone);
                        }

                        LocalPlayer me = Minecraft.getInstance().player;
                        if (me != null) {
                           grip.reportStance(me.getId());
                        }
                     }
                  }
               }
            }
         }
      }
   }
}

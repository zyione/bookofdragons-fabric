package net.magister.liberanimalium.render;

import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.body.grip.Grip;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.LimbReachComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

final class LimbsPass implements RigPass {
   @Override
   public String name() {
      return "limbs";
   }

   @Override
   public void solve(RigFrame frame) {
      if (frame.body instanceof FaunaBase) {
         if (!RigLod.of((FaunaBase)frame.body).legsActive()) {
            return;
         }
      } else if (frame.body.animEntity() != null && !RigLod.of(frame.body.animEntity()).legsActive()) {
         return;
      }

      Entity carried = frame.body.animEntity();
      if (carried == null || (!carried.isPassenger() || carried.getVehicle() instanceof Player) && !Grip.held(carried)) {
         LimbReachComponent reach = LimbReachComponent.of(frame.body);
         if (reach != null) {
            for (String bone : reach.applyReach(frame.model, frame.drawnStand(), frame.modelToStand(), frame.partialTick)) {
               FaunaProceduralAnimator.capture(frame, bone);
            }
         }
      }
   }
}

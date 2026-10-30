package net.magister.liberanimalium.render;

import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;

final class BodyPass implements RigPass {
   @Override
   public String name() {
      return "body";
   }

   @Override
   public void solve(RigFrame frame) {
      AttitudePass.applyBody(frame);
   }
}

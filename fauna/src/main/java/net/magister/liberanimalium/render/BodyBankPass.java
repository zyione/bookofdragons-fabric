package net.magister.liberanimalium.render;

import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.body.anim.data.BoneAccessCache;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.FlightWatch;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class BodyBankPass implements RigPass {
   @Override
   public String name() {
      return "body_bank";
   }

   @Override
   public void solve(RigFrame frame) {
      applyBodyBanking(frame);
   }

   private static void applyBodyBanking(RigFrame frame) {
      FaunaBase body = (FaunaBase)frame.body;
      String root = FaunaProceduralAnimator.first(frame, "root");
      if (root != null && frame.model.getBoneOrNull(root) != null) {
         BoneAccessCache cache = body.getBoneAccessCache();
         Vec3 currentSmoothed = cache.getSmoothedRotation("body_banking");
         float currentSmoothedPitch = (float)currentSmoothed.x;
         float currentSmoothedRoll = (float)currentSmoothed.z;
         FaunaFlightRotationComponent rotComp2D = body.componentRegistry.get(FaunaFlightRotationComponent.class);
         if (body.inThreeDMedium()) {
            float pitch = rotComp2D != null ? rotComp2D.getCurrentPitchRadians() : 0.0F;
            float roll = rotComp2D != null ? rotComp2D.getRoll(frame.partialTick) * (float) (Math.PI / 180.0) : 0.0F;
            cache.setSmoothedRotation("body_banking", new Vec3(pitch, 0.0, roll));
            FlightWatch.clientFrame(
               body,
               pitch,
               roll,
               pitch,
               roll,
               rotComp2D != null && rotComp2D.isRolling(),
               rotComp2D == null ? null : rotComp2D.getRenderOrientation(frame.partialTick)
            );
         } else if (body.isClimbing()) {
            cache.setSmoothedRotation("body_banking", new Vec3(0.0, 0.0, 0.0));
         } else {
            FlightWatch.clientFrame(
               body,
               Float.NaN,
               Float.NaN,
               currentSmoothedPitch,
               currentSmoothedRoll,
               rotComp2D != null && rotComp2D.isRolling(),
               rotComp2D == null ? null : rotComp2D.getRenderOrientation(frame.partialTick)
            );
            float newSmoothedPitch = Mth.lerp(0.1F, currentSmoothedPitch, 0.0F);
            float newSmoothedRoll = Mth.lerp(0.1F, currentSmoothedRoll, 0.0F);
            cache.setSmoothedRotation("body_banking", new Vec3(newSmoothedPitch, 0.0, newSmoothedRoll));
            if (rotComp2D != null && !rotComp2D.isRolling()) {
               rotComp2D.flush2DIntent(body.getYRot(), newSmoothedPitch, newSmoothedRoll);
            }
         }
      }
   }
}

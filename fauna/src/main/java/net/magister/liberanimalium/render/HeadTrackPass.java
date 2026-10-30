package net.magister.liberanimalium.render;

import java.util.ArrayList;
import java.util.List;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.FaunaHeadTrackingSystem;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import org.joml.Matrix3f;

final class HeadTrackPass implements RigPass {
   @Override
   public String name() {
      return "head_track";
   }

   @Override
   public void solve(RigFrame frame) {
      applyHeadTracking(frame);
   }

   private static void applyHeadTracking(RigFrame frame) {
      FaunaBase body = (FaunaBase)frame.body;
      List<String> neck = frame.spec.chain("neck");
      List<String> head = frame.spec.chain("head");
      if (!neck.isEmpty() && !head.isEmpty()) {
         String root = FaunaProceduralAnimator.first(frame, "root");
         String bodyBone = root != null && frame.model.getBoneOrNull(root) != null ? root : null;
         List<String> heads = new ArrayList<>();

         for (String prefix : frame.spec.prefixes()) {
            if (frame.model.getBoneOrNull(prefix + head.get(0)) != null) {
               heads.add(prefix);
            }
         }

         FaunaHeadTrackingSystem tracking = body.getHeadTrackingSystem();
         tracking.gazes().heads(heads, head.get(0));
         float bodyPitch = AttitudePass.pitchDegrees(body) + flightPitchDegrees(body, frame.partialTick);

         for (String prefix : heads) {
            AzBone[] neckBones = new AzBone[neck.size()];
            int segmentCount = 0;

            for (int i = 0; i < neck.size(); i++) {
               AzBone bone = frame.model.getBoneOrNull(prefix + neck.get(i));
               if (bone != null) {
                  neckBones[i] = bone;
                  segmentCount = i + 1;
               }
            }

            AzBone headBone = frame.model.getBoneOrNull(prefix + head.get(0));
            float[] own = tracking.gazes().angles(prefix, frame.partialTick);
            if (own == null) {
               tracking.applyToBones(neckBones, headBone, segmentCount, frame.spec.headTrack(), bodyPitch, frame.partialTick, bodyBone, frame.model);
            } else {
               tracking.applyAngles(neckBones, headBone, segmentCount, frame.spec.headTrack(), bodyPitch, own[0], own[1], bodyBone, frame.model);
            }

            for (int i = 0; i < segmentCount; i++) {
               FaunaProceduralAnimator.capture(frame, prefix + neck.get(i));
            }

            FaunaProceduralAnimator.capture(frame, prefix + head.get(0));
         }
      }
   }

   static float flightPitchDegrees(FaunaBase body, float partial) {
      if (!body.inThreeDMedium()) {
         return 0.0F;
      }

      FaunaFlightRotationComponent rotComp = body.componentRegistry.get(FaunaFlightRotationComponent.class);
      if (rotComp == null) {
         return 0.0F;
      }

      Matrix3f m = new Matrix3f().set(rotComp.getRenderOrientation(partial));
      return FaunaFlightRotationComponent.decomposeSwingTwist(m)[0];
   }
}

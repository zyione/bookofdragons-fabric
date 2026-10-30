package net.magister.liberanimalium.render;

import net.magister.liberanimalium.anim.rig.BoneAxis;
import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.body.anim.data.BoneAccessCache;
import net.magister.liberanimalium.body.state.FlightAnimationState;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.chassis.component.RollComponent;
import net.magister.liberanimalium.config.RideSettings;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class WingsPass implements RigPass {
   private static final float DODGE_FADE = 0.25F;
   private static final float DAMPENING_2D_FACTOR = 0.6F;
   private static final float DAMPENING_2D_DEAD_ZONE = 0.01F;
   private static final float DAMPENING_2D_BLEND_IN = 0.1F;
   private static final float DAMPENING_2D_BLEND_OUT = 0.08F;

   @Override
   public String name() {
      return "wings";
   }

   @Override
   public void solve(RigFrame frame) {
      applyWings(frame);
   }

   private static float diveShare(RigFrame frame, BoneAccessCache cache) {
      String layer = frame.spec.wings().diveLayer();
      return layer == null ? 0.0F : (float)cache.getSmoothedRotation(layer).x;
   }

   private static void applyWings(RigFrame frame) {
      FaunaBase body = (FaunaBase)frame.body;
      FlightAnimationState animState = FaunaProceduralAnimator.currentAnimState();
      if (animState != null) {
         FaunaFlightRotationComponent rotComp = body.componentRegistry.get(FaunaFlightRotationComponent.class);
         if (rotComp != null && rotComp.isRolling()) {
            applyWingRollCompensation(frame, animState);
         } else {
            apply2DWingRollDampening(frame, animState);
         }
      }
   }

   private static void applyWingRollCompensation(RigFrame frame, FlightAnimationState animState) {
      FaunaBase body = (FaunaBase)frame.body;
      boolean is3DFlight = false;
      FaunaFlightRotationComponent rotComp = body.componentRegistry.get(FaunaFlightRotationComponent.class);
      if (rotComp != null && rotComp.isRolling()) {
         is3DFlight = true;
      }

      boolean canBarrelRoll = RideSettings.isBarrelRollEnabled();
      boolean isGliding = body.isFlying() && !body.isAccelerating() && !body.isBoosting() && !body.getStateContext().isDiveBreaking();
      boolean conditionsValid = isGliding && (!body.isActivelyRolling() || is3DFlight || !canBarrelRoll);
      BoneAccessCache cache = body.getBoneAccessCache();
      float currentBlend = (float)cache.getSmoothedRotation("wing_roll_blend").x;
      if (!conditionsValid && currentBlend > 0.001F && body.getWingRollDeblendTicks() <= 0) {
         body.setWingRollDeblendTicks(20);
      }

      float blendFactor;
      if (conditionsValid) {
         body.setWingRollDeblendTicks(0);
         blendFactor = Mth.lerp(0.1F, currentBlend, 1.0F);
      } else if (body.getWingRollDeblendTicks() > 0) {
         int remaining = body.getWingRollDeblendTicks();
         float decayStep = currentBlend / Math.max(remaining, 1);
         blendFactor = Math.max(0.0F, currentBlend - decayStep);
         body.setWingRollDeblendTicks(remaining - 1);
      } else {
         blendFactor = 0.0F;
      }

      cache.setSmoothedRotation("wing_roll_blend", new Vec3(blendFactor, 0.0, 0.0));
      if (!(blendFactor <= 0.001F)) {
         float effectiveRollPitch = Mth.wrapDegrees(rotComp.getRoll()) * (float) (Math.PI / 180.0);
         if (!(Math.abs(effectiveRollPitch) < 0.001F)) {
            float rawCompensationFactor = 0.5F;
            float diveBlend = diveShare(frame, cache);
            float diveFade = 1.0F - Mth.clamp(diveBlend * 2.0F, 0.0F, 1.0F);
            float targetProximityScale;
            if (!animState.isUpsideDown() && !animState.isDiving()) {
               targetProximityScale = diveFade;
            } else {
               targetProximityScale = 0.0F;
            }

            float currentSmoothedScale = cache.getSmoothedProximityScale();
            float smoothingSpeed = 0.05F;
            float finalProximityScale = Mth.lerp(smoothingSpeed, currentSmoothedScale, targetProximityScale);
            cache.setSmoothedProximityScale(finalProximityScale);
            float compensationFactor = rawCompensationFactor * finalProximityScale;
            float antiRoll = effectiveRollPitch * compensationFactor * blendFactor * invertedFade(effectiveRollPitch) * dodgeFade(body, cache);
            float finalAntiRoll = antiRoll;
            String wing = FaunaProceduralAnimator.first(frame, effectiveRollPitch > 0.0F ? "right_wing" : "left_wing");
            if (wing != null) {
               frame.write.add(wing, BoneAxis.ROT_Z, finalAntiRoll);
            }
         }
      }
   }

   static float invertedFade(float rollRadians) {
      float degrees = Math.abs(rollRadians) * (180.0F / (float)Math.PI);
      float t = Mth.clamp((degrees - 90.0F) / 50.0F, 0.0F, 1.0F);
      return 1.0F - t * t * (3.0F - 2.0F * t);
   }

   private static float dodgeFade(FaunaBase body, BoneAccessCache cache) {
      RollComponent rolls = body.componentRegistry.get(RollComponent.class);
      float gone = (float)cache.getSmoothedRotation("wing_roll_dodge").x;
      gone = Mth.lerp(0.25F, gone, rolls != null && rolls.rollingOver() ? 1.0F : 0.0F);
      cache.setSmoothedRotation("wing_roll_dodge", new Vec3(gone, 0.0, 0.0));
      return 1.0F - gone;
   }

   private static void apply2DWingRollDampening(RigFrame frame, FlightAnimationState animState) {
      FaunaBase body = (FaunaBase)frame.body;
      FaunaFlightRotationComponent rotComp = body.componentRegistry.get(FaunaFlightRotationComponent.class);
      if (rotComp != null) {
         boolean isGliding = body.isFlying() && !body.isAccelerating() && !body.isBoosting() && !body.getStateContext().isDiveBreaking();
         boolean conditionsValid = isGliding && !animState.isUpsideDown() && !animState.isDiving();
         BoneAccessCache cache = body.getBoneAccessCache();
         float currentBlend = (float)cache.getSmoothedRotation("wing_roll_2d_blend").x;
         float blendFactor;
         if (conditionsValid) {
            blendFactor = Mth.lerp(0.1F, currentBlend, 1.0F);
         } else {
            blendFactor = Mth.lerp(0.08F, currentBlend, 0.0F);
         }

         cache.setSmoothedRotation("wing_roll_2d_blend", new Vec3(blendFactor, 0.0, 0.0));
         if (!(blendFactor < 0.001F)) {
            float rollRadians = Mth.wrapDegrees(rotComp.getRoll()) * (float) (Math.PI / 180.0);
            if (!(Math.abs(rollRadians) < 0.01F)) {
               float diveBlend = diveShare(frame, cache);
               float diveFade = 1.0F - Mth.clamp(diveBlend * 2.0F, 0.0F, 1.0F);
               float antiRoll = -rollRadians * 0.6F * blendFactor * diveFade * invertedFade(rollRadians) * dodgeFade(body, cache);
               String wing = FaunaProceduralAnimator.first(frame, rollRadians > 0.0F ? "left_wing" : "right_wing");
               if (wing != null) {
                  frame.write.add(wing, BoneAxis.ROT_Z, antiRoll);
               }
            }
         }
      }
   }
}

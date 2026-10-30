package net.magister.liberanimalium.body.locomotion.transition;

import net.magister.liberanimalium.body.locomotion.LocomotionMode;
import net.magister.liberanimalium.body.locomotion.ModeChangeCause;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.DiveBreakComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class LandTransition implements TransitionPrimitive {
   private int ticksRemaining;
   private float attitude;
   private float groundDistance = -1.0F;
   private boolean diveBreaking;

   private static int landableDepth(FaunaBase body) {
      PhysicsSheet.Landing landing = PhysicsSheet.of(body).landing();
      return (int)(landing.windowTicks() * landing.terminalBase());
   }

   public float attitude() {
      return this.attitude;
   }

   public float groundDistance() {
      return this.groundDistance;
   }

   public boolean diveBreaking() {
      return this.diveBreaking;
   }

   @Override
   public boolean precondition(FaunaBase body) {
      return groundBelowWithin(body, landableDepth(body));
   }

   @Override
   public boolean ownsPhysics() {
      return true;
   }

   @Override
   public boolean ownsAttitude() {
      return true;
   }

   @Override
   public void begin(FaunaBase body) {
      this.ticksRemaining = PhysicsSheet.of(body).landing().windowTicks();
      this.attitude = body.getXRot();
      this.groundDistance = -1.0F;
      this.diveBreaking = false;
      Vec3 velocity = body.getDeltaMovement();
      double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
      double cap = body.getStatSheet().airSpeed() * PhysicsSheet.of(body).landing().flareEntry();
      if (horizontal > cap && horizontal > 1.0E-4) {
         double scale = cap / horizontal;
         body.setDeltaMovement(velocity.x * scale, velocity.y, velocity.z * scale);
      }
   }

   @Override
   public boolean tick(FaunaBase body) {
      if (body.onGround()) {
         body.getLocomotionOrchestrator().requestMode(LocomotionMode.WALK, ModeChangeCause.LANDING);
         body.resetFallDistance();
         this.diveBreaking = false;
         body.setIsDiveBreaking(false);
         return true;
      } else if (--this.ticksRemaining <= 0) {
         this.diveBreaking = false;
         body.setIsDiveBreaking(false);
         return true;
      } else {
         Vec3 velocity = body.getDeltaMovement();
         DiveBreakComponent diveBreak = body.componentRegistry.get(DiveBreakComponent.class);
         this.groundDistance = diveBreak == null ? -1.0F : diveBreak.calculateGroundDistance();
         float ease = diveBreak != null && !(this.groundDistance < 0.0F) ? diveBreak.getDiveBreakSpeedMultiplier(this.groundDistance) : 1.0F;
         this.diveBreaking = this.groundDistance >= 0.0F && this.groundDistance < DiveBreakComponent.tuning(body).range();
         body.setIsDiveBreaking(this.diveBreaking);
         PhysicsSheet.Landing landing = PhysicsSheet.of(body).landing();
         double terminal = landing.terminalBase() + body.getBbHeight() * landing.terminalPerHeight();
         double descent = Math.max(velocity.y - body.getStatSheet().gravity(), -terminal);
         body.setDeltaMovement(velocity.x * landing.flareDecay(), descent, velocity.z * landing.flareDecay());
         flyTheNose(body, ease);
         this.attitude = body.getXRot();
         body.move(MoverType.SELF, body.getDeltaMovement());
         return false;
      }
   }

   @Override
   public void interrupt(FaunaBase body) {
      this.diveBreaking = false;
      body.setIsDiveBreaking(false);
   }

   @Override
   public String animSignal() {
      return "LAND";
   }

   @Override
   public LocomotionMode fallbackMode() {
      return LocomotionMode.FLY;
   }

   private static void flyTheNose(FaunaBase body, float ease) {
      Vec3 velocity = body.getDeltaMovement();
      double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
      float path = (float)Math.toDegrees(Math.atan2(-velocity.y, horizontal));
      float wanted = Mth.clamp(path, -65.0F, 65.0F) * ease;
      float rate = Math.max(
         1.0F,
         PhysicsSheet.of(body).flight().pitchRate()
            * body.getStatSheet().agility()
            * PhysicsSheet.of(body).chase().pitchScale()
            * PhysicsSheet.of(body).chase().diveFactor()
      );
      body.setXRot(Mth.approach(body.getXRot(), wanted, rate));
   }

   private static boolean groundBelowWithin(FaunaBase body, int depth) {
      BlockPos at = BlockPos.containing(body.position());

      for (int step = 0; step <= depth; step++) {
         BlockPos probe = at.below(step);
         if (!body.level().getBlockState(probe).getCollisionShape(body.level(), probe).isEmpty()) {
            return true;
         }
      }

      return false;
   }
}

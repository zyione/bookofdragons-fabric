package net.magister.liberanimalium.body.locomotion.transition;

import net.magister.liberanimalium.body.BodyPlans;
import net.magister.liberanimalium.body.locomotion.LocomotionMode;
import net.magister.liberanimalium.body.locomotion.RideTuning;
import net.magister.liberanimalium.body.locomotion.WallHit;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.chassis.component.RiderInputComponent;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.network.ControlMessageThrottleSync;
import net.magister.liberanimalium.network.ControlMessageWallHit;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public class WallHitTransition implements TransitionPrimitive {
   private Vec3 impact = Vec3.ZERO;
   private float severity;
   private WallHit.Choice forcedChoice;
   private int holdTicks;
   private int holdTotal;
   private int pushTicks;
   private boolean pushing;
   private WallHit.Choice choice = WallHit.Choice.NONE;
   private Vec3 launch = Vec3.ZERO;
   private float targetYaw;
   private int ejectedAt = Integer.MIN_VALUE;

   public boolean ejectGrace(FaunaBase body) {
      return this.ejectedAt != Integer.MIN_VALUE && body.tickCount - this.ejectedAt < RideTuning.ejectGraceTicks(body);
   }

   public void arm(Vec3 impact, float severity) {
      this.arm(impact, severity, null);
   }

   public void arm(Vec3 impact, float severity, WallHit.Choice forced) {
      this.impact = impact;
      this.severity = severity;
      this.forcedChoice = forced;
   }

   public int holdTicks() {
      return this.holdTicks;
   }

   public int holdTotal() {
      return this.holdTotal;
   }

   public float severity() {
      return this.severity;
   }

   public boolean pushing() {
      return this.pushing;
   }

   public WallHit.Choice choice() {
      return this.choice;
   }

   public float targetYaw() {
      return this.targetYaw;
   }

   @Override
   public boolean precondition(FaunaBase body) {
      return WallHit.isHit(this.impact);
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
      this.holdTotal = BodyPlans.pushOffTicks(body.animSpeciesId());
      this.holdTicks = this.holdTotal;
      this.pushTicks = 0;
      this.pushing = false;
      this.choice = WallHit.Choice.NONE;
      this.launch = Vec3.ZERO;
      this.targetYaw = body.getYRot();
      writeThrottle(body, 0.0F);
      RiderInputComponent input = body.componentRegistry.get(RiderInputComponent.class);
      if (input != null) {
         input.setCruiseRequested(false);
      }

      if (body.level().isClientSide() && body.isControlledByLocalInstance()) {
         body.predictFaunaPose(PoseFlag.WALL_HIT, true);
         FaunaNetwork.sendToServer(new ControlMessageWallHit(this.severity));
      }
   }

   @Override
   public boolean tick(FaunaBase body) {
      if (this.holdTicks > 0) {
         this.holdTicks--;
         body.setDeltaMovement(Vec3.ZERO);
         return false;
      }

      if (!this.pushing) {
         this.pushing = true;
         RiderInputComponent input = body.componentRegistry.get(RiderInputComponent.class);
         this.choice = this.forcedChoice != null
            ? this.forcedChoice
            : (input == null ? WallHit.Choice.NONE : WallHit.choose(input.getStrafe(), input.getForward()));
         this.targetYaw = WallHit.launchHeading(body, this.impact, this.choice, body.getYRot());
         this.launch = WallHit.launchVelocity(body, this.choice, this.targetYaw, body.getAttributeValue(Attributes.FLYING_SPEED));
         writeThrottle(body, WallHit.launchThrottle(body, this.choice));
         FaunaFlightRotationComponent rot = body.componentRegistry.get(FaunaFlightRotationComponent.class);
         if (rot != null && rot.isRolling()) {
            rot.commandHeading(this.targetYaw);
         }
      }

      body.setDeltaMovement(this.launch);
      if (this.launch.lengthSqr() > 0.0) {
         body.move(MoverType.SELF, body.getDeltaMovement());
      }

      turn(body, this.targetYaw);
      this.pushTicks++;
      boolean done = this.pushTicks >= RideTuning.pushOffTicks(body);
      if (done) {
         this.end(body);
      }

      return done;
   }

   private static void turn(FaunaBase body, float targetYaw) {
      FaunaFlightRotationComponent rot = body.componentRegistry.get(FaunaFlightRotationComponent.class);
      if (rot == null || !rot.isRolling()) {
         float yaw = WallHit.turnToward(body.getYRot(), targetYaw, RideTuning.pushTurnRate(body));
         float dYaw = Mth.wrapDegrees(yaw - body.getYRot());
         body.setYRot(body.getYRot() + dYaw);
         body.yRotO += dYaw;
         body.yBodyRot = body.getYRot();
         body.yBodyRotO += dYaw;
         body.yHeadRot = body.getYRot();
         body.yHeadRotO += dYaw;
         LivingEntity rider = body.getControllingPassenger();
         if (rider != null) {
            float riderDYaw = Mth.wrapDegrees(body.getYRot() - rider.getYRot());
            rider.setYRot(rider.getYRot() + riderDYaw);
            rider.yRotO += riderDYaw;
         }
      }
   }

   @Override
   public void interrupt(FaunaBase body) {
      this.end(body);
   }

   private void end(FaunaBase body) {
      this.ejectedAt = body.tickCount;
      this.impact = Vec3.ZERO;
      this.launch = Vec3.ZERO;
      this.forcedChoice = null;
      FaunaFlightRotationComponent rot = body.componentRegistry.get(FaunaFlightRotationComponent.class);
      if (rot != null) {
         rot.clearCommandedHeading();
      }

      if (body.level().isClientSide() && body.isControlledByLocalInstance()) {
         body.predictFaunaPose(PoseFlag.WALL_HIT, false);
      }
   }

   private static void writeThrottle(FaunaBase body, float level) {
      body.setThrottleLevel(level);
      body.setAccelerating(false);
      body.setDecelerating(false);
      if (body.level().isClientSide() && body.isControlledByLocalInstance()) {
         FaunaNetwork.sendToServer(new ControlMessageThrottleSync(level, false, false));
      }
   }

   @Override
   public String animSignal() {
      return "WALL_HIT";
   }

   @Override
   public LocomotionMode fallbackMode() {
      return LocomotionMode.FLY;
   }
}

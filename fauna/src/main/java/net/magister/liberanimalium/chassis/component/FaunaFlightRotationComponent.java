package net.magister.liberanimalium.chassis.component;

import net.magister.liberanimalium.body.locomotion.DiveModule;
import net.magister.liberanimalium.body.locomotion.FlightEngine;
import net.magister.liberanimalium.body.locomotion.RideTuning;
import net.magister.liberanimalium.body.locomotion.SwimSteer;
import net.magister.liberanimalium.body.locomotion.WalkEngine;
import net.magister.liberanimalium.body.locomotion.WaterEngine;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.config.RideSettings;
import net.magister.liberanimalium.network.ControlMessageRoll;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.ride.flight.rotation.FaunaRotationContext;
import net.magister.liberanimalium.ride.flight.rotation.FaunaRotationInstant;
import net.magister.liberanimalium.ride.flight.rotation.FaunaRotationModifiers;
import net.magister.liberanimalium.ride.flight.rotation.RollDeadzone;
import net.magister.liberanimalium.util.SaveKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.SmoothDouble;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FaunaFlightRotationComponent implements IFaunaComponent {
   private final Matrix3f orientation = new Matrix3f();
   private final Matrix3f prevOrientation = new Matrix3f();
   private final Matrix3f prevTickOrientation = new Matrix3f();
   private boolean orientationInitialized = false;
   private int normalizeCounter = 0;
   private float roll = 0.0F;
   private float prevRoll = 0.0F;
   private float lastRelVelX = 0.0F;
   private float lastRelVelY = 0.0F;
   private float lastRelVelZ = 0.0F;
   private float prevRelVelX = 0.0F;
   private float prevRelVelY = 0.0F;
   private float prevRelVelZ = 0.0F;
   private float lastRelRotPitch = 0.0F;
   private float lastRelRotYaw = 0.0F;
   private float lastRelRotRoll = 0.0F;
   private float prevRelRotPitch = 0.0F;
   private float prevRelRotYaw = 0.0F;
   private float prevRelRotRoll = 0.0F;
   private static final int OMEGA_BUFFER_SIZE = 40;
   private final Vec3[] localOmegaHistory = new Vec3[40];
   private int omegaHistoryPointer = 0;
   protected FaunaBase body;
   private boolean isRolling = false;
   private final SmoothDouble pitchSmoother = new SmoothDouble();
   private final SmoothDouble yawSmoother = new SmoothDouble();
   private final SmoothDouble rollSmoother = new SmoothDouble();
   private final RollDeadzone rollDeadzone = new RollDeadzone();
   private int syncCounter = 0;
   private int freeLookExitTimer = 0;
   private boolean wasFreeLooking = false;
   private boolean localPlayerDriving = false;
   private double pendingInputPitch = 0.0;
   private double pendingInputYaw = 0.0;
   private double pendingInputRoll = 0.0;
   private boolean intentSnapshotInitialized = false;
   private float strafeLean = 0.0F;
   private float prevStrafeLean = 0.0F;
   private float sinkLean = 0.0F;
   private float prevSinkLean = 0.0F;
   private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
   private static final float RAD_TO_DEG = 180.0F / (float)Math.PI;
   private boolean eulerDrawn = false;
   private final Vector3f spinAxis = new Vector3f(1.0F, 0.0F, 0.0F);
   private float spinRate;
   private static final double HOVER_EDGE_SHARE = 0.999;
   private int rollSteppedTick = Integer.MIN_VALUE;
   private float strafeLevel;
   private float commandedHeading = Float.NaN;

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;

      for (int i = 0; i < 40; i++) {
         this.localOmegaHistory[i] = Vec3.ZERO;
      }
   }

   private void initializeOrientation() {
      float mcYaw = this.body.getYRot();
      float mcPitch = this.body.getXRot();
      float mcRoll = this.roll;
      this.orientation.identity();
      this.orientation.rotateY(-(mcYaw - 180.0F) * (float) (Math.PI / 180.0));
      this.orientation.rotateX(-mcPitch * (float) (Math.PI / 180.0));
      this.orientation.rotateZ(mcRoll * (float) (Math.PI / 180.0));
      this.prevOrientation.set(this.orientation);
      this.orientationInitialized = true;
   }

   private void normalizeOrientation() {
      Quaternionf q = new Quaternionf().setFromUnnormalized(this.orientation);
      q.normalize();
      q.get(this.orientation);
   }

   public void changeFlightLook(double pitch, double yaw, double rollInput, double mouseDelta) {
      if (this.body.supportsThreeDMedium()) {
         FaunaRotationInstant rotDelta = new FaunaRotationInstant(pitch, yaw, rollInput);
         float currentRoll = this.getRoll();
         FaunaRotationInstant currentRotation = new FaunaRotationInstant(this.body.getXRot(), this.body.getYRot(), currentRoll);
         FaunaRotationContext context = new FaunaRotationContext(currentRotation, rotDelta, mouseDelta, this.body);
         context.useModifier(FaunaRotationModifiers.fixNaN("INPUT"));
         context.useModifier(FaunaRotationModifiers.configureAxes(false));
         context.useModifier(FaunaRotationModifiers.fixNaN("AXES"));
         double bodySens = RideSettings.getMouseSensitivity();
         context.useModifier(FaunaRotationModifiers.applySensitivity(bodySens, 1.0, bodySens));
         if (this.body.isAquatic()) {
            PhysicsSheet.Water water = PhysicsSheet.of(this.body).water();
            context.useModifier(FaunaRotationModifiers.applySensitivity(water.sensitivity(), water.sensitivity(), water.sensitivity() * water.roll()));
         }

         context.useModifier(FaunaRotationModifiers.fixNaN("SENSITIVITY"));
         rotDelta = context.getRotationDelta();
         this.pendingInputPitch = this.pendingInputPitch + rotDelta.pitch();
         this.pendingInputYaw = this.pendingInputYaw + rotDelta.yaw();
         this.pendingInputRoll = this.pendingInputRoll + rotDelta.roll();
      }
   }

   public void flushPendingInput(float frameDelta) {
      if (this.body.supportsThreeDMedium()) {
         if (!this.isRolling) {
            this.dropPendingInput();
            this.rollDeadzone.reset();
         } else {
            this.localPlayerDriving = true;
            if (this.body.getLocomotionOrchestrator().attitudeOwned()) {
               this.dropPendingInput();
               this.rollDeadzone.reset();
               this.turnToCommandedHeading(frameDelta);
            } else {
               boolean folded = this.folded();
               boolean ballistic = folded || this.stalled();
               if (folded) {
                  this.dropPendingInput();
                  this.pitchSmoother.reset();
                  this.yawSmoother.reset();
                  this.rollSmoother.reset();
                  this.rollDeadzone.reset();
                  this.spin(frameDelta);
                  this.applyBallistic(frameDelta);
                  this.syncRoll();
               } else {
                  double pitchIn = this.pendingInputPitch;
                  double yawIn = this.pendingInputYaw;
                  double rollIn = this.pendingInputRoll;
                  this.dropPendingInput();
                  rollIn = this.rollDeadzone.filter(rollIn, frameDelta, PhysicsSheet.of(this.body).attitude());
                  FaunaRotationInstant rotDelta = this.shapedDelta(pitchIn, yawIn, rollIn, frameDelta, ballistic);
                  if (!this.orientationInitialized) {
                     this.initializeOrientation();
                  }

                  float pitchBefore = decomposeSwingTwist(this.orientation)[0];
                  float pitchCap = FlightEngine.pitchLimit(this.body) * 20.0F * frameDelta;
                  this.applyRotation((float)rotDelta.pitch(), (float)rotDelta.yaw(), (float)rotDelta.roll(), pitchCap);
                  this.spin(frameDelta);
                  if (ballistic) {
                     this.applyBallistic(frameDelta);
                  } else if (this.body.isAquatic()) {
                     this.applyWaterBox(frameDelta, pitchBefore);
                  } else {
                     this.applyHoverBox(frameDelta, pitchBefore);
                  }

                  this.syncRoll();
               }
            }
         }
      }
   }

   private void dropPendingInput() {
      this.pendingInputPitch = 0.0;
      this.pendingInputYaw = 0.0;
      this.pendingInputRoll = 0.0;
   }

   private void turnToCommandedHeading(float frameDelta) {
      if (!Float.isNaN(this.commandedHeading)) {
         if (!this.orientationInitialized) {
            this.initializeOrientation();
         }

         float perFrame = RideTuning.pushTurnRate(this.body) * 20.0F * frameDelta;
         float step = Mth.wrapDegrees(this.commandedHeading - this.body.getYRot());
         step = Mth.clamp(step, -perFrame, perFrame);
         if (Math.abs(step) > 1.0E-4F) {
            this.orientation.rotateLocalY(-step * (float) (Math.PI / 180.0));
            this.syncEntityFromOrientation();
         }
      }
   }

   private FaunaRotationInstant shapedDelta(double pitchIn, double yawIn, double rollIn, float frameDelta, boolean ballistic) {
      float currentRoll = this.getRoll();
      FaunaRotationInstant currentRotation = new FaunaRotationInstant(this.body.getXRot(), this.body.getYRot(), currentRoll);
      FaunaRotationInstant rotDelta = new FaunaRotationInstant(pitchIn, yawIn, rollIn);
      FaunaRotationContext context = new FaunaRotationContext(currentRotation, rotDelta, frameDelta, this.body);
      context.useModifier(FaunaRotationModifiers.applyTurnRateLimitNormalised(this.body, frameDelta));
      RollComponent rolls = this.body.componentRegistry.get(RollComponent.class);
      if (!ballistic && (rolls == null || !rolls.rollingOver())) {
         PhysicsSheet.Flight flight = this.body.flightNumbers();
         double bankingStrength = flight.banking() * this.body.getStatSheet().agility() * this.body.getStatSheet().bankYaw();
         context.useModifier(FaunaRotationModifiers.banking(bankingStrength, this.body));
         if (!this.body.isAquatic()) {
            double autoLevel = this.body.getStatSheet().autoLevel();
            context.useModifier(FaunaRotationModifiers.autoLevel(flight.autoLevelPitch() * autoLevel, flight.autoLevelRoll() * autoLevel));
         }
      }

      PhysicsSheet.Attitude attitude = this.body.attitudeNumbers();
      double rollLag = attitude.rollInertia() / Math.max(0.001, rollWeight(this.body.getStatSheet().agility(), attitude));
      context.useModifier(FaunaRotationModifiers.smoothing(this.pitchSmoother, this.yawSmoother, this.rollSmoother, attitude.smoothing(), rollLag));
      context.useModifier(FaunaRotationModifiers.fixNaN("FINAL"));
      return context.getRotationDelta();
   }

   private void syncRoll() {
      this.syncCounter++;
      if (this.syncCounter >= 3) {
         this.syncCounter = 0;
         FaunaNetwork.sendToServer(new ControlMessageRoll(this.isRolling, -this.roll));
      }
   }

   private void spin(float frameDelta) {
      RollComponent rolls = this.body.componentRegistry.get(RollComponent.class);
      if (rolls != null && rolls.spinning()) {
         if (!this.orientationInitialized) {
            this.initializeOrientation();
         }

         this.orientation.rotateZ(rolls.spinStep(frameDelta, this.dodgeRollGain()) * (float) (Math.PI / 180.0));
         if (!rolls.spinning()) {
            this.normalizeOrientation();
         }

         this.syncEntityFromOrientation();
      }
   }

   private void applyBallistic(float frameDelta) {
      if (!this.orientationInitialized) {
         this.initializeOrientation();
      }

      PhysicsSheet.Flight flight = PhysicsSheet.of(this.body).flight();
      float ticks = 20.0F * frameDelta;
      if (this.spinRate > 1.0E-5F) {
         this.orientation.rotate(this.spinRate * ticks, this.spinAxis.x, this.spinAxis.y, this.spinAxis.z);
         this.spinRate = this.spinRate * (float)Math.pow(flight.spinDrag(), ticks);
      }

      Vec3 v = this.body.getDeltaMovement();
      double speed = v.length();
      if (speed > 0.001) {
         Vector3f local = this.orientation.transformTranspose(new Vector3f((float)v.x, (float)v.y, (float)v.z), new Vector3f());
         float ahead = -local.z;
         float pitchErr = (float)Math.atan2(local.y, ahead);
         float yawErr = 0.0F;
         if (ahead < 0.0F) {
            float pitchSpin = this.spinAxis.x * this.spinRate;
            if (pitchSpin >= -1.0E-4F && pitchErr < 0.0F) {
               pitchErr += (float) (Math.PI * 2);
            } else if (pitchSpin < -1.0E-4F && pitchErr > 0.0F) {
               pitchErr -= (float) (Math.PI * 2);
            }
         } else {
            yawErr = (float)Math.atan2(local.x, ahead);
         }

         float k = Math.min(1.0F, flight.vane() * (float)Math.min(1.0, speed / flight.vaneSpeed()) * ticks);
         this.orientation.rotateX(pitchErr * k);
         this.orientation.rotateY(-yawErr * k);
      }

      this.normalizeOrientation();
      this.syncEntityFromOrientation();
   }

   private void applyHoverBox(float frameDelta, float pitchBefore) {
      if (!(this.body.getDeltaMovement().length() >= 0.15F)) {
         PhysicsSheet.Flight flight = PhysicsSheet.of(this.body).flight();
         float pitch = decomposeSwingTwist(this.orientation)[0];
         float step = flight.hoverBoxRate() * 20.0F * frameDelta;
         float limit = flight.hoverPitchLimit();
         float boxedPitch = Math.abs(pitchBefore) <= limit ? softLimit(pitchBefore, pitch, limit) : towardBox(pitch, limit, step);
         this.pitchAboutLevel(boxedPitch - pitch);
      }
   }

   public Vec3 bodyAxis(float x, float y, float z) {
      if (!this.orientationInitialized) {
         this.initializeOrientation();
      }

      Vector3f axis = this.orientation.transform(new Vector3f(x, y, z), new Vector3f());
      return new Vec3(axis.x, axis.y, axis.z);
   }

   private void applyWaterBox(float frameDelta, float pitchBefore) {
      WaterEngine swim = this.body.componentRegistry.get(WaterEngine.class);
      PhysicsSheet.Water water = PhysicsSheet.of(this.body).water();
      boolean surface = swim != null && swim.atSurface();
      float box = water.righting() * frameDelta;
      float roll = decomposeSwingTwist(this.orientation)[2];
      if (!Float.isNaN(roll)) {
         float next = roll - SwimSteer.righting(roll, water.righting(), frameDelta);
         if (surface) {
            next = towardBox(next, water.surfaceRoll(), box);
         }

         this.orientation.rotateZ((roll - next) * (float) (Math.PI / 180.0));
         this.normalizeOrientation();
         this.syncEntityFromOrientation();
      }

      float pitch = decomposeSwingTwist(this.orientation)[0];
      float next = pitch - SwimSteer.righting(pitch, water.pitchRighting(), frameDelta);
      float limit = water.surfacePitch();
      if (!this.diver()) {
         next = Math.abs(pitchBefore) <= limit ? softLimit(pitchBefore, next, limit) : towardBox(next, limit, box);
      } else if (surface && this.body.getDeltaMovement().length() < water.breachSpeed() && next < -limit) {
         next = Math.min(-limit, next + box);
      }

      this.pitchAboutLevel(next - pitch);
   }

   private void pitchAboutLevel(float correction) {
      if (correction != 0.0F) {
         Vector3f nose = this.orientation.transform(new Vector3f(0.0F, 0.0F, -1.0F), new Vector3f());
         Vector3f across = new Vector3f(nose).cross(0.0F, 1.0F, 0.0F);
         if (!(across.lengthSquared() < 1.0E-6F)) {
            across.normalize();
            this.orientation.rotateLocal(-correction * (float) (Math.PI / 180.0), across.x, across.y, across.z);
            this.normalizeOrientation();
            this.syncEntityFromOrientation();
         }
      }
   }

   static float softLimit(float before, float after, float limit) {
      if (Math.abs(after) <= Math.abs(before)) {
         return after;
      }

      float from = Math.signum(after) == Math.signum(before) ? before : 0.0F;
      double ratio = Math.max(-0.999999, Math.min(0.999999, (double)from / limit));
      double effort = limit * 0.5 * Math.log((1.0 + ratio) / (1.0 - ratio)) + (after - from);
      double share = Math.max(-0.999, Math.min(0.999, Math.tanh(effort / limit)));
      return (float)(limit * share);
   }

   static float towardBox(float value, float limit, float step) {
      if (value > limit) {
         return Math.max(limit, value - step);
      } else {
         return value < -limit ? Math.min(-limit, value + step) : value;
      }
   }

   public void flush2DIntent(float yawDeg, float pitchRad, float rollRad) {
      this.orientation.identity();
      this.orientation.rotateY(-(yawDeg - 180.0F) * (float) (Math.PI / 180.0));
      this.orientation.rotateX(pitchRad);
      this.orientation.rotateZ(rollRad);
      if (!this.orientationInitialized) {
         this.prevOrientation.set(this.orientation);
      }

      this.orientationInitialized = true;
      this.eulerDrawn = true;
   }

   public void captureTickIntent() {
      if (!this.intentSnapshotInitialized) {
         this.intentSnapshotInitialized = true;
         this.prevTickOrientation.set(this.orientation);
      } else {
         Vec3 dVel = this.body.getDeltaMovement();
         Vector3f worldVel = new Vector3f((float)dVel.x, (float)dVel.y, (float)dVel.z);
         Vector3f localVel = new Vector3f();
         this.orientation.transformTranspose(worldVel, localVel);
         this.prevRelVelX = this.lastRelVelX;
         this.prevRelVelY = this.lastRelVelY;
         this.prevRelVelZ = this.lastRelVelZ;
         this.lastRelVelX = localVel.x;
         this.lastRelVelY = -localVel.y;
         this.lastRelVelZ = localVel.z;
         Matrix3f localDelta = new Matrix3f();
         this.prevTickOrientation.transpose(localDelta);
         localDelta.mul(this.orientation);
         float localPitch = (float)Math.asin(Mth.clamp(-localDelta.m21(), -1.0, 1.0)) * (180.0F / (float)Math.PI);
         float localYaw = (float)Math.atan2(localDelta.m20(), localDelta.m22()) * (180.0F / (float)Math.PI);
         float localRoll = (float)Math.atan2(localDelta.m01(), localDelta.m11()) * (180.0F / (float)Math.PI);
         this.prevRelRotPitch = this.lastRelRotPitch;
         this.prevRelRotYaw = this.lastRelRotYaw;
         this.prevRelRotRoll = this.lastRelRotRoll;
         this.lastRelRotPitch = localPitch;
         this.lastRelRotYaw = localYaw;
         this.lastRelRotRoll = localRoll;
         this.localOmegaHistory[this.omegaHistoryPointer] = new Vec3(localPitch, localYaw, localRoll);
         this.omegaHistoryPointer = (this.omegaHistoryPointer + 1) % 40;
         this.prevTickOrientation.set(this.orientation);
      }
   }

   public float getRelVelX(float pt) {
      return Mth.lerp(pt, this.prevRelVelX, this.lastRelVelX);
   }

   public float getRelVelY(float pt) {
      return Mth.lerp(pt, this.prevRelVelY, this.lastRelVelY);
   }

   public float getRelVelZ(float pt) {
      return Mth.lerp(pt, this.prevRelVelZ, this.lastRelVelZ);
   }

   public float getRelRotPitch(float pt) {
      return Mth.lerp(pt, this.prevRelRotPitch, this.lastRelRotPitch);
   }

   public float getRelRotYaw(float pt) {
      return Mth.lerp(pt, this.prevRelRotYaw, this.lastRelRotYaw);
   }

   public float getRelRotRoll(float pt) {
      return Mth.lerp(pt, this.prevRelRotRoll, this.lastRelRotRoll);
   }

   public Vec3 getLocalOmegaAtLag(int ticksBack, float partialTick) {
      int curr = (this.omegaHistoryPointer - ticksBack - 1 + 40) % 40;
      int prev = (this.omegaHistoryPointer - ticksBack - 2 + 40) % 40;
      Vec3 cV = this.localOmegaHistory[curr];
      Vec3 pV = this.localOmegaHistory[prev];
      return new Vec3(
         Mth.lerp(partialTick, pV.x, cV.x),
         Mth.lerp(partialTick, pV.y, cV.y),
         Mth.lerp(partialTick, pV.z, cV.z)
      );
   }

   public void applyRotation(float pitch, float yaw, float rollInput) {
      this.applyRotation(pitch, yaw, rollInput, Float.MAX_VALUE);
   }

   static double rollWeight(float agility, PhysicsSheet.Attitude attitude) {
      return attitude.rollAgility() == 0.0F ? 1.0 : Math.pow(Math.max(0.001, agility), attitude.rollAgility());
   }

   static double rollGain(double rollAuthority, double speedPenalty, double rollWeight) {
      return rollAuthority * speedPenalty * rollWeight;
   }

   public double rollGain() {
      PhysicsSheet.Flight flight = this.body.flightNumbers();
      PhysicsSheet.Attitude attitude = this.body.attitudeNumbers();
      double envelope = FaunaRotationModifiers.computeAirspeedEnvelope(this.body);
      double authority = attitude.rollAuthorityHover() + envelope * (1.0 - attitude.rollAuthorityHover());
      double penalty = 1.0 / (1.0 + this.body.getThrottleLevel() * flight.turnSpeedPenalty() * attitude.rotationPenaltyScale());
      return rollGain(authority, penalty, rollWeight(this.body.getStatSheet().agility(), attitude));
   }

   private double airspeed() {
      double top = this.body.getAttributeValue(Attributes.FLYING_SPEED);
      return Math.max(this.body.getThrottleLevel() * top, this.body.getDeltaMovement().length());
   }

   public double dodgeAuthority() {
      PhysicsSheet.Attitude attitude = this.body.attitudeNumbers();
      double envelope = FaunaRotationModifiers.envelopeAt(this.body, this.airspeed() / PhysicsSheet.of(this.body).flight().rollReferenceSpeed());
      double authority = attitude.rollAuthorityHover() + envelope * (1.0 - attitude.rollAuthorityHover());
      return authority * rollWeight(this.body.getStatSheet().agility(), attitude);
   }

   public double dodgeRollGain() {
      PhysicsSheet.Flight flight = this.body.flightNumbers();
      PhysicsSheet.Attitude attitude = this.body.attitudeNumbers();
      double top = this.body.getAttributeValue(Attributes.FLYING_SPEED);
      double share = top <= 0.0 ? 1.0 : Math.min(1.0, this.airspeed() / top);
      double penalty = 1.0 / (1.0 + share * flight.turnSpeedPenalty() * attitude.rotationPenaltyScale());
      return this.dodgeAuthority() * penalty;
   }

   public void applyRotation(float pitch, float yaw, float rollInput, float maxPitchDegrees) {
      if (!this.orientationInitialized) {
         this.initializeOrientation();
      }

      PhysicsSheet.Flight flight = this.body.flightNumbers();
      float agility = this.body.getStatSheet().agility();
      float pitchRate = flight.pitchRate() * agility;
      float yawRate = flight.yawRate() * agility;
      double speedEnvelope = FaunaRotationModifiers.computeAirspeedEnvelope(this.body);
      PhysicsSheet.Attitude attitude = this.body.attitudeNumbers();
      double pitchAeroFactor = attitude.pitchAuthorityHover() + speedEnvelope * (1.0 - attitude.pitchAuthorityHover());
      double yawAeroFactor = attitude.yawAuthorityHover() + speedEnvelope * (1.0 - attitude.yawAuthorityHover());
      double rollAeroFactor = attitude.rollAuthorityHover() + speedEnvelope * (1.0 - attitude.rollAuthorityHover());
      double throttle = this.body.getThrottleLevel();
      double speedPenalty = 1.0 / (1.0 + throttle * flight.turnSpeedPenalty() * attitude.rotationPenaltyScale());
      double actualPitchRate = pitchRate * pitchAeroFactor * speedPenalty;
      double actualYawRate = yawRate * yawAeroFactor * speedPenalty;
      double actualRollRate = rollGain(rollAeroFactor, speedPenalty, rollWeight(agility, attitude));
      float pitchRad = (float)(-Mth.clamp(actualPitchRate * pitch, -maxPitchDegrees, maxPitchDegrees) * (float) (Math.PI / 180.0));
      float yawRad = (float)(-actualYawRate * yaw * (float) (Math.PI / 180.0));
      float rollRad = (float)(-actualRollRate * rollInput * (float) (Math.PI / 180.0));
      this.orientation.rotateX(pitchRad);
      this.orientation.rotateY(yawRad);
      this.orientation.rotateZ(rollRad);
      this.normalizeCounter++;
      if (this.normalizeCounter >= 20) {
         this.normalizeCounter = 0;
         this.normalizeOrientation();
      }

      this.syncEntityFromOrientation();
   }

   private void syncEntityFromOrientation() {
      float currentPitch = this.body.getXRot();
      float currentYaw = this.body.getYRot();
      float[] pyr = decomposeSwingTwist(this.orientation);
      float newPitch = pyr[0];
      float newYaw = Float.isNaN(pyr[1]) ? currentYaw : pyr[1];
      float newRoll = Float.isNaN(pyr[2]) ? this.roll : pyr[2];
      float newPitchClamped = Mth.clamp(newPitch, -90.0F, 90.0F);
      float deltaPitch = newPitchClamped - currentPitch;
      float deltaYaw = Mth.wrapDegrees(newYaw - currentYaw);
      this.body.setXRot(newPitchClamped);
      this.body.xRotO += deltaPitch;
      this.body.setYRot(currentYaw + deltaYaw);
      this.body.yRotO += deltaYaw;
      this.body.yBodyRot = this.body.getYRot();
      this.body.yBodyRotO += deltaYaw;
      this.body.yHeadRot += deltaYaw;
      this.body.yHeadRotO += deltaYaw;
      if (this.body.getControllingPassenger() instanceof Player rider) {
         boolean isFreeLooking = false;
         if (this.body.supportsThreeDMedium()) {
            isFreeLooking = this.body.isFreeLooking();
         }

         if (!isFreeLooking) {
            if (this.freeLookExitTimer > 0) {
               float exit = PhysicsSheet.of(this.body).attitude().freeLookExitLerp();
               float smoothY = Mth.rotLerp(exit, rider.getYRot(), this.body.getYRot());
               float smoothX = Mth.rotLerp(exit, rider.getXRot(), this.body.getXRot());
               rider.setYRot(smoothY);
               rider.setXRot(smoothX);
            } else {
               rider.setXRot(this.body.getXRot());
               rider.setYRot(this.body.getYRot());
               rider.xRotO = this.body.xRotO;
               rider.yRotO = this.body.yRotO;
            }
         }
      }

      this.prevRoll = this.roll;
      this.roll = Mth.wrapDegrees(newRoll);
   }

   public void tick() {
      if (this.rollSteppedTick != this.body.tickCount) {
         this.prevRoll = this.roll;
      }

      if (this.orientationInitialized) {
         this.measureSpin();
         this.prevOrientation.set(this.orientation);
      }

      this.prevStrafeLean = this.strafeLean;
      boolean threeD = this.isRolling && this.body.inThreeDMedium();
      RiderInputComponent riderInput = this.body.componentRegistry.get(RiderInputComponent.class);
      float held = threeD && riderInput != null ? Mth.clamp(riderInput.getStrafe(), -1.0F, 1.0F) : 0.0F;
      this.strafeLevel = Mth.approach(this.strafeLevel, held, 0.1F);
      float lean = 0.0F;
      if (threeD) {
         PhysicsSheet.Flight air = PhysicsSheet.of(this.body).flight();
         if (this.body.isAquatic()) {
            lean = this.strafeDrive() * air.strafeLean();
         } else {
            double top = this.body.getAttributeValue(Attributes.FLYING_SPEED);
            lean = air.strafeLean()
               * FlightEngine.strafeShare(this.strafeDrive(), air.strafeSpeed(), this.body.getThrottleLevel() * top, top)
               / Math.max(0.001F, air.strafeSpeed());
         }

         RollComponent rolls = this.body.componentRegistry.get(RollComponent.class);
         if (rolls != null && rolls.dodging() && !rolls.spinning()) {
            lean = rolls.dodgeSide() * air.strafeLean() * rolls.dodgeSpeedShare();
         } else if (rolls != null && rolls.rollingOver()) {
            lean = this.strafeLean * 0.5F;
         }
      }

      this.strafeLean = lean;
      this.prevSinkLean = this.sinkLean;
      float sinkTarget = 0.0F;
      LivingEntity rider = this.body.getControllingPassenger();
      if (this.body.isAquatic() && this.body.steeredByRider() && rider != null && rider.isShiftKeyDown() && this.body.getStatSheet() != null) {
         double full = WaterEngine.topSpeed(this.body, this.body.getStatSheet()) * PhysicsSheet.of(this.body).water().sink();
         if (full > 1.0E-4) {
            sinkTarget = 30.0F * (float)Mth.clamp((this.body.yo - this.body.getY()) / full, 0.0, 1.0);
         }
      }

      this.sinkLean = this.sinkLean + (sinkTarget - this.sinkLean) * PhysicsSheet.of(this.body).flight().strafeLeanRate();
      if (this.body.supportsThreeDMedium()) {
         boolean isFreeLooking = this.body.isFreeLooking();
         if (this.wasFreeLooking && !isFreeLooking) {
            this.freeLookExitTimer = PhysicsSheet.of(this.body).attitude().freeLookExitTicks();
         }

         this.wasFreeLooking = isFreeLooking;
      }

      if (this.freeLookExitTimer > 0) {
         this.freeLookExitTimer--;
      }
   }

   public Quaternionf getRenderOrientation(float partialTick) {
      if (!this.orientationInitialized) {
         return new Quaternionf()
            .rotationYXZ(
               -(this.body.getYRot() + 180.0F) * (float) (Math.PI / 180.0),
               -this.body.getXRot() * (float) (Math.PI / 180.0),
               this.roll * (float) (Math.PI / 180.0)
            );
      } else if (!this.localPlayerDriving && !this.eulerDrawn) {
         Quaternionf qOld = new Quaternionf().setFromUnnormalized(this.prevOrientation);
         Quaternionf qNew = new Quaternionf().setFromUnnormalized(this.orientation);
         return qOld.slerp(qNew, partialTick);
      } else {
         return new Quaternionf().setFromUnnormalized(this.orientation);
      }
   }

   public float getCurrentPitchRadians() {
      if (!this.orientationInitialized) {
         return -this.body.getXRot() * (float) (Math.PI / 180.0);
      }

      float[] pyr = decomposeSwingTwist(this.orientation);
      return -pyr[0] * (float) (Math.PI / 180.0);
   }

   public float getRoll(float tickDelta) {
      return tickDelta != 1.0F && (!this.isRolling || !this.localPlayerDriving) ? Mth.rotLerp(tickDelta, this.prevRoll, this.getRoll()) : this.getRoll();
   }

   public float getRoll() {
      return this.roll;
   }

   public static float[] decomposeSwingTwist(Matrix3f m) {
      Vector3f forward = m.transform(new Vector3f(0.0F, 0.0F, -1.0F), new Vector3f());
      float pitch = (float)Math.toDegrees(Math.asin(Mth.clamp(-forward.y(), -1.0F, 1.0F)));
      double horizLen = Math.sqrt(forward.x() * forward.x() + forward.z() * forward.z());
      if (horizLen <= 0.08) {
         return new float[]{pitch, Float.NaN, Float.NaN};
      }

      float yaw = (float)Math.toDegrees(Math.atan2(-forward.x(), forward.z()));
      Vector3f up = m.transform(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f());
      Vector3f worldUp = new Vector3f(0.0F, 1.0F, 0.0F);
      Vector3f neutralUp = new Vector3f(worldUp).sub(new Vector3f(forward).mul(worldUp.dot(forward))).normalize();
      if (neutralUp.lengthSquared() < 0.001F) {
         return new float[]{pitch, yaw, Float.NaN};
      }

      Vector3f neutralRight = new Vector3f(forward).cross(worldUp).normalize();
      float roll = (float)Math.toDegrees(Math.atan2(up.dot(neutralRight), up.dot(neutralUp)));
      return new float[]{pitch, yaw, roll};
   }

   public float[] getStableCameraAngles() {
      if (!this.orientationInitialized) {
         return new float[]{this.body.getYRot(), this.body.getXRot(), this.roll};
      }

      float[] pyr = decomposeSwingTwist(this.orientation);
      float stableYaw = Float.isNaN(pyr[1]) ? this.body.getYRot() : pyr[1];
      float stablePitch = pyr[0];
      float stableRoll = Float.isNaN(pyr[2]) ? this.roll : pyr[2];
      return new float[]{stableYaw, stablePitch, stableRoll};
   }

   public void setRoll(float roll) {
      if (Float.isFinite(roll)) {
         this.roll = Mth.wrapDegrees(roll);
         this.prevRoll = this.roll;
      }
   }

   public void stepRoll(float roll) {
      if (Float.isFinite(roll)) {
         this.prevRoll = this.roll;
         this.roll = Mth.wrapDegrees(roll);
         this.rollSteppedTick = this.body.tickCount;
      }
   }

   public float getStrafeLean(float partialTick) {
      return Mth.lerp(partialTick, this.prevStrafeLean, this.strafeLean);
   }

   public float strafeDrive() {
      float a = Math.abs(this.strafeLevel);
      return Math.signum(this.strafeLevel) * a * a * (3.0F - 2.0F * a);
   }

   public float getSinkLean(float partialTick) {
      return Mth.lerp(partialTick, this.prevSinkLean, this.sinkLean);
   }

   public boolean isRolling() {
      return this.isRolling;
   }

   public void commandHeading(float yaw) {
      this.commandedHeading = yaw;
   }

   public void clearCommandedHeading() {
      this.commandedHeading = Float.NaN;
   }

   public void setRolling(boolean rolling) {
      if (rolling && !this.isRolling) {
         this.orientationInitialized = false;
         this.eulerDrawn = false;
         if (this.body.isAquatic()) {
            this.roll = 0.0F;
            this.prevRoll = 0.0F;
         }
      }

      if (!rolling && this.isRolling) {
         this.pendingInputPitch = 0.0;
         this.pendingInputYaw = 0.0;
         this.pendingInputRoll = 0.0;
         this.intentSnapshotInitialized = false;
         this.localPlayerDriving = false;
      }

      this.isRolling = rolling;
   }

   @Override
   public void serverTick() {
      this.tick();
   }

   private void measureSpin() {
      if (this.isRolling) {
         if (!this.folded() && !this.stalled()) {
            Matrix3f delta = this.prevOrientation.transpose(new Matrix3f()).mul(this.orientation);
            AxisAngle4f aa = new AxisAngle4f().set(delta);
            if (Float.isFinite(aa.angle) && aa.angle > 1.0E-5F) {
               this.spinAxis.set(aa.x, aa.y, aa.z).normalize();
               this.spinRate = aa.angle;
            } else {
               this.spinRate = 0.0F;
            }
         }
      }
   }

   private boolean folded() {
      if (this.body.isFlying()) {
         FlightEngine engine = this.body.componentRegistry.get(FlightEngine.class);
         return engine != null && engine.isFolded();
      }

      if (!this.body.isAquatic()) {
         return false;
      }

      WaterEngine swim = this.body.componentRegistry.get(WaterEngine.class);
      return swim != null && swim.breaching();
   }

   private boolean stalled() {
      if (!this.body.isFlying()) {
         return false;
      }

      FlightEngine engine = this.body.componentRegistry.get(FlightEngine.class);
      return engine != null && engine.isStalled();
   }

   private boolean diver() {
      return this.body.componentRegistry.get(DiveModule.class) != null;
   }

   @Override
   public void clientTick() {
      if (!this.body.inThreeDMedium()) {
         this.advanceGroundOrientation();
      } else {
         if (this.body.supportsThreeDMedium()) {
            WalkEngine walkEngine = this.body.componentRegistry.get(WalkEngine.class);
            if (walkEngine != null) {
               walkEngine.setWasGroundLastTick(false);
            }
         }

         if (!this.isRolling) {
            this.captureTickIntent();
         }
      }
   }

   private void advanceGroundOrientation() {
      float currentYaw = this.body.getYRot();
      boolean firstGroundTick = false;
      if (this.body.supportsThreeDMedium()) {
         WalkEngine walkEngine = this.body.componentRegistry.get(WalkEngine.class);
         if (walkEngine != null && !walkEngine.isWasGroundLastTick()) {
            firstGroundTick = true;
            walkEngine.setWasGroundLastTick(true);
         }
      }

      if (this.orientationInitialized && !firstGroundTick) {
         this.prevOrientation.set(this.orientation);
         this.prevTickOrientation.set(this.orientation);
         this.orientation.identity();
         this.orientation.rotateY(-(currentYaw - 180.0F) * (float) (Math.PI / 180.0));
         Matrix3f localDelta = new Matrix3f();
         this.prevTickOrientation.transpose(localDelta);
         localDelta.mul(this.orientation);
         float localYaw = (float)Math.atan2(localDelta.m20(), localDelta.m22()) * (180.0F / (float)Math.PI);
         this.localOmegaHistory[this.omegaHistoryPointer] = new Vec3(0.0, localYaw, 0.0);
         this.omegaHistoryPointer = (this.omegaHistoryPointer + 1) % 40;
         this.prevRelRotYaw = this.lastRelRotYaw;
         this.lastRelRotYaw = localYaw;
         this.roll = 0.0F;
         this.prevRoll = 0.0F;
      } else {
         this.orientation.identity();
         this.orientation.rotateY(-(currentYaw - 180.0F) * (float) (Math.PI / 180.0));
         this.prevOrientation.set(this.orientation);
         this.prevTickOrientation.set(this.orientation);
         this.roll = 0.0F;
         this.prevRoll = 0.0F;
         this.orientationInitialized = true;
         this.localOmegaHistory[this.omegaHistoryPointer] = Vec3.ZERO;
         this.omegaHistoryPointer = (this.omegaHistoryPointer + 1) % 40;
      }
   }

   @Override
   public void save(CompoundTag tag) {
      tag.putFloat("FaunaRoll", this.roll);
      tag.putBoolean("FaunaIsRolling", this.isRolling);
      if (this.orientationInitialized) {
         Quaternionf q = new Quaternionf().setFromUnnormalized(this.orientation);
         tag.putFloat("OrientX", q.x());
         tag.putFloat("OrientY", q.y());
         tag.putFloat("OrientZ", q.z());
         tag.putFloat("OrientW", q.w());
      }
   }

   @Override
   public void load(CompoundTag tag) {
      this.roll = tag.getFloat(SaveKeys.of(tag, "FaunaRoll", "DragonRoll"));
      this.prevRoll = this.roll;
      this.isRolling = tag.getBoolean(SaveKeys.of(tag, "FaunaIsRolling", "DragonIsRolling"));
      if (tag.contains("OrientW")) {
         Quaternionf q = new Quaternionf(tag.getFloat("OrientX"), tag.getFloat("OrientY"), tag.getFloat("OrientZ"), tag.getFloat("OrientW"));
         q.normalize();
         q.get(this.orientation);
         this.prevOrientation.set(this.orientation);
         this.orientationInitialized = true;
      }
   }
}

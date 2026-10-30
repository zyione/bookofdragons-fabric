package net.magister.liberanimalium.body.locomotion;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.body.stats.FaunaStatSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.chassis.component.ThrottleComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WaterEngine implements ModeEngine {
   private static final float AIR_KEEP = 0.98F;
   private static final float GROUND_KEEP = 0.6F;
   private static final float MAX_PITCH = 70.0F;
   private static final float LEAN_PER_DEGREE = 3.0F;
   private static final float MAX_LEAN = 25.0F;
   private static final int FLOP_TICKS = 16;
   private static final double FLOP_UP = 0.32;
   private static final double FLOP_REACH = 0.18;
   private static final int STRANDED_LOOK = 8;
   private static final int STRANDED_LOOK_TICKS = 20;
   private static final double HAUL_PROBE = 0.5;
   private FaunaBase body;
   private boolean breaching;
   private boolean atSurface;
   private int porpoise;
   private int underTicks;
   private boolean duckSpent;
   private int flopCooldown;
   private int lookCooldown;
   private boolean leapAsked;
   @Nullable
   private BlockPos waterSeen;

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   @Override
   public LocomotionMode mode() {
      return LocomotionMode.SWIM;
   }

   public void askLeap() {
      this.leapAsked = true;
   }

   public boolean breaching() {
      return this.breaching;
   }

   public boolean atSurface() {
      return this.atSurface;
   }

   @Override
   public void tick(SteeringIntent intent) {
      FaunaStatSheet stats = this.body.getStatSheet();
      if (stats != null) {
         PhysicsSheet sheet = PhysicsSheet.of(this.body);
         PhysicsSheet.Water water = sheet.water();
         PhysicsSheet.Weight weight = sheet.weight(this.body.getWeightClass());
         float floatLine = weight.floatLine();
         double height = Math.max(0.1, this.body.getBbHeight());
         double share = WaterDepth.share(this.body);
         LivingEntity rider = this.body.steeredByRider() ? this.body.getControllingPassenger() : null;
         boolean diver = this.body.componentRegistry.get(DiveModule.class) != null;
         this.atSurface = share > 0.0 && share <= floatLine + water.surfaceMargin();
         float top = topSpeed(this.body, stats);
         Vec3 v = this.body.getDeltaMovement();
         FaunaFlightRotationComponent attitude = this.body.componentRegistry.get(FaunaFlightRotationComponent.class);
         double strafe = 0.0;
         boolean sink = false;
         double throttle;
         if (rider != null) {
            ThrottleComponent controls = this.body.componentRegistry.get(ThrottleComponent.class);
            throttle = controls != null ? controls.tickThrottleMode(intent.forward()) : Math.max(0.0F, intent.forward());
            strafe = attitude != null ? attitude.strafeDrive() : intent.strafe();
            sink = rider.isShiftKeyDown();
         } else {
            throttle = Math.max(0.0F, intent.forward()) * (intent.sprint() ? water.sprint() : 1.0F);
            float pitchTarget = diver ? swimPitch(intent.vertical(), Math.max(0.0F, intent.forward())) : 0.0F;
            if (this.body.getAirSupply() < water.airLow() * this.body.getMaxAirSupply()) {
               pitchTarget = diver ? -70.0F : 0.0F;
               throttle = 1.0;
            }

            this.steerMind(intent.yawTarget(), !this.breaching && !(share <= 0.0) ? pitchTarget : pathPitch(v), v, attitude);
         }

         double holdOff = 0.0;
         if (!diver) {
            this.underTicks = !this.atSurface && !(share <= 0.0) ? this.underTicks + 1 : 0;
            if (!sink) {
               this.duckSpent = false;
            }

            if (sink && this.underTicks >= water.duckTicks()) {
               this.duckSpent = true;
            }

            if (this.duckSpent) {
               sink = false;
            }

            if (sink) {
               holdOff = 1.0;
            }
         }

         double flat = Math.sqrt(v.x * v.x + v.z * v.z);
         boolean leap = false;
         if (this.atSurface && !this.breaching && this.leapAsked) {
            leap = true;
            this.leapAsked = false;
         } else if (this.atSurface && !this.breaching && rider == null && intent.sprint() && flat >= 0.8 * top) {
            this.porpoise++;
            leap = this.porpoise >= water.porpoiseTicks();
         } else {
            this.porpoise = 0;
         }

         double vx = v.x;
         double vy = v.y;
         double vz = v.z;
         double haul = 0.0;
         if (leap) {
            this.porpoise = 0;
            this.breaching = true;
            vy = Math.sqrt(0.16 * water.leapHeight()) * this.body.profile().proficiency(LocomotionMode.SWIM);
         } else if (share > 0.0) {
            if (this.breaching && vy <= 0.0) {
               this.breaching = false;
            }

            Vec3 nose = attitude != null && rider != null && attitude.isRolling()
               ? attitude.bodyAxis(0.0F, 0.0F, -1.0F)
               : Vec3.directionFromRotation(this.body.getXRot(), this.body.getYRot());
            Vec3 left = new Vec3(1.0, 0.0, 0.0).yRot(-this.body.getYRot() * (float) (Math.PI / 180.0));
            Vec3 target = nose.scale(top * throttle)
               .add(left.scale(strafe * top * water.strafe()))
               .add(0.0, sink ? -top * water.sink() : 0.0, 0.0);
            double inertia = Math.max(0.1, weight.inertia());
            Vec3 next = SwimSteer.servo(v, target, nose, water.axial() / inertia, water.cross() / inertia);
            vx = next.x;
            vz = next.z;
            vy = next.y + buoyancy(share, floatLine, diver ? water.diverLift() : water.lift(), holdOff) - 0.08;
            vy = target.y + (vy - target.y) * floatKeep(share, floatLine, surfaceKeep(floatLine, height, water.damping()));
            vy = Mth.clamp(vy, -0.8, 0.8);
            if (throttle > 0.05 && this.body.horizontalCollision && !this.breaching) {
               haul = this.bankRise(nose, share * height + this.body.maxUpStep());
            }

            if (haul > 0.0) {
               vy = Math.min(haul, this.body.maxUpStep());
            } else if (vy > water.breachSpeed() && share < floatLine && !this.breaching) {
               this.breaching = true;
               double kick = Math.sqrt(0.16 * water.leapHeight())
                  * this.body.profile().proficiency(LocomotionMode.SWIM)
                  * Math.min(1.0, Math.sqrt(vx * vx + vy * vy + vz * vz) / Math.max(0.001, top));
               vy = Math.max(vy, kick);
            }
         } else if (this.body.onGround()) {
            Vec3 next = this.stranded(vx, vy, vz, intent);
            vx = next.x;
            vy = next.y;
            vz = next.z;
         } else {
            vx *= 0.98F;
            vz *= 0.98F;
            vy = WalkEngine.fall(vy);
         }

         this.body.setDeltaMovement(vx, vy, vz);
         this.body.move(MoverType.SELF, this.body.getDeltaMovement());
         if (haul > 0.0) {
            this.body.setDeltaMovement(this.body.getDeltaMovement().multiply(1.0, 0.0, 1.0));
         }
      }
   }

   private double bankRise(Vec3 nose, double reach) {
      Vec3 flat = new Vec3(nose.x, 0.0, nose.z);
      if (flat.lengthSqr() < 1.0E-6) {
         return 0.0;
      }

      Vec3 ahead = flat.normalize().scale(0.5);
      AABB raised = this.body.getBoundingBox().move(ahead.x, reach, ahead.z);
      if (!this.body.level().noCollision(this.body, raised)) {
         return 0.0;
      }

      double drop = Entity.collideBoundingBox(this.body, new Vec3(0.0, -reach, 0.0), raised, this.body.level(), List.of()).y;
      double rise = reach + drop;
      return drop > -reach + 1.0E-4 && rise > 0.001 ? rise : 0.0;
   }

   private Vec3 stranded(double vx, double vy, double vz, SteeringIntent intent) {
      this.breaching = false;
      vx *= 0.6F;
      vz *= 0.6F;
      vy = WalkEngine.fall(vy);
      if (this.flopCooldown > 0) {
         this.flopCooldown--;
      }

      boolean walks = this.body.profile().hasMode(LocomotionMode.WALK);
      if (!walks && --this.lookCooldown <= 0) {
         this.waterSeen = nearestWater(this.body);
         this.lookCooldown = 20;
      }

      float effort = Math.max(Math.abs(intent.forward()), Math.abs(intent.strafe()));
      if (!walks && this.waterSeen != null) {
         double dx = this.waterSeen.getX() + 0.5 - this.body.getX();
         double dz = this.waterSeen.getZ() + 0.5 - this.body.getZ();
         this.body.setYRot((float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F);
         this.body.yBodyRot = this.body.getYRot();
         effort = 1.0F;
      }

      if (effort > 0.05F && this.flopCooldown == 0 && !walks) {
         float yawRad = this.body.getYRot() * (float) (Math.PI / 180.0);
         vx = -Mth.sin(yawRad) * 0.18;
         vz = Mth.cos(yawRad) * 0.18;
         vy = 0.32;
         this.flopCooldown = 16;
      }

      return new Vec3(vx, vy, vz);
   }

   @Nullable
   private static BlockPos nearestWater(FaunaBase body) {
      BlockPos at = body.blockPosition();
      MutableBlockPos scan = new MutableBlockPos();
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;

      for (int dx = -8; dx <= 8; dx++) {
         for (int dz = -8; dz <= 8; dz++) {
            for (int dy = -2; dy <= 1; dy++) {
               scan.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
               if (body.level().getFluidState(scan).is(FluidTags.WATER)) {
                  double distance = dx * dx + dz * dz + dy * dy;
                  if (distance < bestDistance) {
                     bestDistance = distance;
                     best = scan.immutable();
                  }
               }
            }
         }
      }

      return best;
   }

   private void steerMind(float yawTarget, float pitchTarget, Vec3 v, FaunaFlightRotationComponent attitude) {
      float rate = FlightEngine.maxTurnSpeed(this.body, v.length());
      float yawStep = Mth.clamp(Mth.wrapDegrees(yawTarget - this.body.getYRot()), -rate, rate);
      this.body.setYRot(this.body.getYRot() + yawStep);
      this.body.setXRot(this.body.getXRot() + Mth.clamp(pitchTarget - this.body.getXRot(), -rate, rate));
      this.body.yBodyRot = this.body.getYRot();
      this.body.yHeadRot = this.body.yBodyRot;
      if (attitude != null) {
         FaunaStatSheet stats = this.body.getStatSheet();
         double speedShare = Math.min(1.0, v.length() / Math.max(0.001, stats == null ? 1.0 : topSpeed(this.body, stats)));
         float lean = (float)(-Mth.clamp(yawStep * 3.0F, -25.0F, 25.0F) * speedShare);
         attitude.stepRoll(attitude.getRoll() + (lean - attitude.getRoll()) * 0.2F);
      }
   }

   private static float pathPitch(Vec3 v) {
      double flat = Math.sqrt(v.x * v.x + v.z * v.z);
      return (float)Mth.clamp(-Math.toDegrees(Math.atan2(v.y, Math.max(0.001, flat))), -70.0, 70.0);
   }

   public static float topSpeed(FaunaBase body, FaunaStatSheet stats) {
      return stats.waterSpeed() * body.profile().proficiency(LocomotionMode.SWIM);
   }

   static float swimPitch(float vertical, float forward) {
      if (Math.abs(vertical) < 0.001F) {
         return 0.0F;
      }

      float angle = (float)Math.toDegrees(Math.atan2(vertical, Math.max(forward, 0.05F)));
      return Mth.clamp(-angle, -70.0F, 70.0F);
   }

   static double buoyancy(double share, double floatLine, double lift, double effort) {
      double g = 0.08;
      return share <= floatLine ? g * share / floatLine : g * (1.0 + lift * (share - floatLine) / (1.0 - floatLine) * (1.0 - effort));
   }

   static double floatKeep(double share, double floatLine, double surface) {
      return share <= floatLine ? 1.0 - (1.0 - surface) * share / floatLine : Mth.lerp((share - floatLine) / (1.0 - floatLine), surface, 1.0);
   }

   public static double vertical(double vy, double share, double floatLine, double height, double lift, double effort, double drag, double damping) {
      vy += buoyancy(share, floatLine, lift, effort) - 0.08;
      double surface = surfaceKeep(floatLine, height, damping);
      double keep = share <= floatLine ? 1.0 - (1.0 - surface) * share / floatLine : Mth.lerp((share - floatLine) / (1.0 - floatLine), surface, drag);
      return Mth.clamp(vy * keep, -0.8, 0.8);
   }

   public static double surfaceKeep(double floatLine, double height, double damping) {
      double omega = Math.sqrt(0.08 / (Math.max(0.05, floatLine) * Math.max(0.1, height)));
      return Math.exp(-2.0 * damping * omega);
   }
}

package net.magister.liberanimalium.anim.rig;

import net.minecraft.world.phys.Vec3;

public final class FootStep {
   public Vec3 pin;
   public FootStep.Mode mode = FootStep.Mode.PINNED;
   public FootStep.Trigger trigger = FootStep.Trigger.NONE;
   public Vec3 sole;
   public double lift;
   public double progress;
   public int sinceLanded = 1048576;
   public int sinceLift = 1048576;
   public int stanceTicks;
   public int swingTicks;
   private boolean radiusCrossed;
   public Vec3 start;
   public Vec3 target;

   public void place(Vec3 solePoint) {
      this.pin = solePoint;
      this.sole = solePoint;
      this.mode = FootStep.Mode.PINNED;
      this.lift = 0.0;
      this.progress = 0.0;
   }

   public void release() {
      this.pin = null;
      this.sole = null;
      this.mode = FootStep.Mode.PINNED;
      this.lift = 0.0;
      this.progress = 0.0;
   }

   public boolean grounded() {
      return this.pin != null && this.mode == FootStep.Mode.PINNED;
   }

   public void tickClocks() {
      if (this.sinceLanded < 1048576) {
         this.sinceLanded++;
      }

      if (this.sinceLift < 1048576) {
         this.sinceLift++;
      }
   }

   public FootStep.Trigger trigger(Vec3 home, double radius, double leash, boolean supported, boolean pairDown, boolean pairRested) {
      if (!supported) {
         return FootStep.Trigger.SUPPORT;
      } else {
         double d = horizontal(this.pin, home);
         if (d > leash && pairDown) {
            return FootStep.Trigger.LEASH;
         } else {
            return d > radius && pairDown && pairRested ? FootStep.Trigger.DISTANCE : FootStep.Trigger.NONE;
         }
      }
   }

   public FootStep.Trigger stray(Vec3 home, Vec3 travel, double radius, double leash, boolean supported, boolean adjacentDown, boolean adjacentRested) {
      if (!supported) {
         return FootStep.Trigger.SUPPORT;
      }

      double t = Math.hypot(travel.x, travel.z);
      double behind;
      double side;
      if (t < 1.0E-6) {
         behind = horizontal(this.pin, home);
         side = 0.0;
      } else {
         double dx = this.pin.x - home.x;
         double dz = this.pin.z - home.z;
         behind = -(dx * travel.x + dz * travel.z) / t;
         side = Math.abs(dx * travel.z - dz * travel.x) / t;
      }

      if (behind > radius && !this.radiusCrossed && this.sinceLanded < 524288) {
         this.radiusCrossed = true;
         this.stanceTicks = this.sinceLanded;
      }

      if (behind > leash || side > leash) {
         return FootStep.Trigger.LEASH;
      } else {
         return behind > radius && adjacentDown && adjacentRested ? FootStep.Trigger.GAIT : FootStep.Trigger.NONE;
      }
   }

   public int trotWait() {
      return Math.max(0, (this.stanceTicks - this.swingTicks) / 2);
   }

   public static double half(GaitSpec g, double reach, double speed) {
      return half(g, reach, reach, speed);
   }

   public static double half(GaitSpec g, double floorReach, double capReach, double speed) {
      double bySpeed = speed * g.strideTicks() / 2.0;
      return Math.max(g.strideFloor() * floorReach, Math.min(g.strideShare() * capReach, bySpeed));
   }

   public static Vec3 led(GaitSpec g, Vec3 home, Vec3 homeVel, double leadMax) {
      double lx = homeVel.x * g.leadTicks();
      double lz = homeVel.z * g.leadTicks();
      double d = Math.hypot(lx, lz);
      if (d > leadMax) {
         lx *= leadMax / d;
         lz *= leadMax / d;
      }

      return new Vec3(home.x + lx, home.y, home.z + lz);
   }

   public static Vec3 ahead(Vec3 home, Vec3 homeVel, double half) {
      double d = Math.hypot(homeVel.x, homeVel.z);
      return d < 1.0E-6 ? home : new Vec3(home.x + homeVel.x / d * half, home.y, home.z + homeVel.z / d * half);
   }

   public static double along(double hx, double hz, double dx, double dz, double reach) {
      double hd = hx * dx + hz * dz;
      double disc = hd * hd + reach * reach - (hx * hx + hz * hz);
      return disc <= 0.0 ? 0.0 : Math.max(0.0, -hd + Math.sqrt(disc));
   }

   public static double strideLeash(GaitSpec g, double zone, double radius, double half, double cap) {
      return Math.min(Math.max(leash(g, zone), radius + (half + radius) / 2.0), cap);
   }

   public static double leash(GaitSpec g, double zone) {
      return zone * g.leashShare();
   }

   public static double leadMax(GaitSpec g, double zone) {
      return zone * g.leadShare();
   }

   public void hold() {
      this.sole = this.pin;
   }

   public void begin(Vec3 target, FootStep.Trigger why) {
      this.mode = FootStep.Mode.PACED;
      this.trigger = why;
      this.start = this.pin;
      this.target = target;
      this.progress = 0.0;
      this.sinceLift = 0;
      this.sole = this.pin;
      this.lift = 0.0;
   }

   public void retarget(Vec3 estimate, double rate) {
      this.target = this.target.add(estimate.subtract(this.target).scale(rate));
   }

   public void carry(Vec3 bodyVel, double yawDeltaRad, Vec3 pivot) {
      if (this.mode == FootStep.Mode.PACED) {
         if (this.start != null) {
            this.start = carryPoint(this.start, bodyVel, yawDeltaRad, pivot);
         }

         if (this.target != null) {
            this.target = carryPoint(this.target, bodyVel, yawDeltaRad, pivot);
         }

         if (this.sole != null) {
            this.sole = carryPoint(this.sole, bodyVel, yawDeltaRad, pivot);
         }
      }
   }

   private static Vec3 carryPoint(Vec3 pt, Vec3 vel, double yawDeltaRad, Vec3 pivot) {
      Vec3 p = pt;
      if (pivot != null && Math.abs(yawDeltaRad) > 1.0E-6) {
         Vec3 rel = p.subtract(pivot);
         p = pivot.add(rel.yRot((float)yawDeltaRad));
      }

      if (vel != null) {
         p = p.add(vel);
      }

      return p;
   }

   public boolean advance(GaitSpec g, double liftHeight, double homeSpeed) {
      return this.moveTo(this.paced(g, homeSpeed), liftHeight);
   }

   private double paced(GaitSpec g, double homeSpeed) {
      double dist = Math.max(horizontal(this.start, this.target), 0.05);
      double pace = Math.max(Math.max(g.stepSpeed(), dist / g.stepTicks()), g.paceShare() * homeSpeed);
      return this.progress + pace / dist;
   }

   private boolean moveTo(double to, double liftHeight) {
      this.progress = Math.min(1.0, to);
      double s = this.progress * this.progress * (3.0 - 2.0 * this.progress);
      this.sole = this.start.lerp(this.target, s);
      this.lift = liftHeight * 4.0 * s * (1.0 - s);
      if (this.progress >= 1.0) {
         this.place(this.target);
         if (this.sinceLift < 524288) {
            this.swingTicks = this.sinceLift;
         }

         this.sinceLanded = 0;
         this.radiusCrossed = false;
         return true;
      } else {
         return false;
      }
   }

   public static double horizontal(Vec3 a, Vec3 b) {
      return Math.hypot(a.x - b.x, a.z - b.z);
   }

   public enum Mode {
      PINNED,
      PACED;
   }

   public enum Trigger {
      NONE,
      DISTANCE,
      LEASH,
      SUPPORT,
      GAIT;
   }
}

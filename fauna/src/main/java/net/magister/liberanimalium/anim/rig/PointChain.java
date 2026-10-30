package net.magister.liberanimalium.anim.rig;

import java.util.function.UnaryOperator;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class PointChain {
   private static final double REST_SPEED = 0.01;
   private final int bones;
   private final int total;
   private final Vec3[] q;
   private final Vec3[] qPrev;
   private final Vec3[] offset;
   private final Vec3[] prevOffset;
   private final Vec3[] targetDir;
   private final Vec3[] lastDir;
   private final Vec3[] clipDir;
   private final Vec3[] coneDir;
   private final double[] support;
   private final boolean[] contact;
   private final Vec3[] pushed;
   private final double[] slide;
   private final double[] radius;
   private final Vec3[] worldFinal;
   private final double[] motion;
   private final int[] restTicks;
   private final boolean[] asleep;
   private final double[] prevSupport;
   private final double[] knownSupport;
   private Vec3 rootPrev;
   private double flowShare;
   private double flowStiffness;
   private boolean headHeld;
   private boolean primed;
   private int reprimes;
   private double lastReprime;
   private int spanTicks = 1;
   private boolean missed;
   private boolean settleTouched;
   private final int[] branch;
   private static final PerfClock.Section CLOCK_SEARCH = PerfClock.client("rig.chain.search");
   private double bestAngle;
   private double upBias;
   private final double[] ringCos = new double[13];
   private final double[] ringSin = new double[13];
   private static final int AZIMUTH_HALVINGS = 4;
   private static final int AZIMUTHS = 16;
   private static final int RINGS = 12;
   private static final int BISECTIONS = 6;

   static Vec3 waterDrag(Vec3 velocity, Vec3 segment, PointChain.Water water, double submerged) {
      Vec3 axis = unit(segment);
      if (axis != null && !(submerged <= 0.0)) {
         Vec3 along = axis.scale(velocity.dot(axis));
         Vec3 across = velocity.subtract(along);
         return along.scale(1.0 - water.dragAlong() * submerged).add(across.scale(1.0 - water.dragAcross() * submerged));
      } else {
         return velocity;
      }
   }

   public PointChain(int bones) {
      this.bones = bones;
      this.total = bones + 1;
      this.q = new Vec3[this.total];
      this.qPrev = new Vec3[this.total];
      this.offset = new Vec3[this.total];
      this.prevOffset = new Vec3[this.total];
      this.targetDir = new Vec3[this.total];
      this.lastDir = new Vec3[this.total];
      this.clipDir = new Vec3[this.total];
      this.coneDir = new Vec3[this.total];
      this.support = new double[this.total];
      this.contact = new boolean[this.total];
      this.pushed = new Vec3[this.total];
      this.slide = new double[this.total];
      this.radius = new double[this.total];
      this.worldFinal = new Vec3[this.total];
      this.motion = new double[this.total];
      this.restTicks = new int[this.total];
      this.asleep = new boolean[this.total];
      this.prevSupport = new double[this.total];
      this.knownSupport = new double[this.total];
      this.branch = new int[this.total];

      for (int i = 0; i < this.total; i++) {
         this.q[i] = Vec3.ZERO;
         this.qPrev[i] = Vec3.ZERO;
         this.offset[i] = Vec3.ZERO;
         this.prevOffset[i] = Vec3.ZERO;
         this.support[i] = Double.NaN;
         this.prevSupport[i] = Double.NaN;
         this.knownSupport[i] = Double.NaN;
         this.pushed[i] = Vec3.ZERO;
      }
   }

   public int count() {
      return this.bones;
   }

   public int total() {
      return this.total;
   }

   public void reset(Vec3[] worldTargets, PointChain.Params params) {
      Vec3[] t = this.frameTargets(worldTargets, params);

      for (int i = 0; i < this.total; i++) {
         this.q[i] = t[i];
         this.qPrev[i] = t[i];
         this.offset[i] = Vec3.ZERO;
         this.prevOffset[i] = Vec3.ZERO;
         this.targetDir[i] = i == 0 ? null : unit(t[i].subtract(t[i - 1]));
         this.lastDir[i] = this.targetDir[i];
         this.clipDir[i] = this.targetDir[i];
         this.coneDir[i] = this.targetDir[i];
         this.support[i] = Double.NaN;
         this.contact[i] = false;
         this.pushed[i] = Vec3.ZERO;
         this.slide[i] = 0.0;
         this.worldFinal[i] = null;
         this.motion[i] = 0.0;
         this.restTicks[i] = 0;
         this.asleep[i] = false;
         this.prevSupport[i] = Double.NaN;
         this.knownSupport[i] = Double.NaN;
      }

      this.rootPrev = worldTargets[0];
      this.flowShare = 0.0;
      this.flowStiffness = 0.0;
      this.headHeld = false;
      this.primed = true;
   }

   public void ride(UnaryOperator<Vec3> ground) {
      if (this.rootPrev != null) {
         Vec3 root = ground.apply(this.rootPrev);

         for (int i = 0; i < this.total; i++) {
            if (this.worldFinal[i] != null) {
               Vec3 moved = ground.apply(this.worldFinal[i]);
               double rise = moved.y - this.worldFinal[i].y;
               this.support[i] = this.support[i] + rise;
               this.knownSupport[i] = this.knownSupport[i] + rise;
               this.worldFinal[i] = moved;
            }

            this.q[i] = this.turned(ground, root, this.q[i]);
            this.qPrev[i] = this.turned(ground, root, this.qPrev[i]);
            this.offset[i] = this.turned(ground, root, this.offset[i]);
            this.prevOffset[i] = this.turned(ground, root, this.prevOffset[i]);
            if (this.lastDir[i] != null) {
               this.lastDir[i] = this.turned(ground, root, this.lastDir[i]);
            }
         }

         this.rootPrev = root;
      }
   }

   private Vec3 turned(UnaryOperator<Vec3> ground, Vec3 root, Vec3 v) {
      return ground.apply(this.rootPrev.add(v)).subtract(root);
   }

   public int reprimes() {
      return this.reprimes;
   }

   public double lastReprime() {
      return this.lastReprime;
   }

   public boolean primed() {
      return this.primed;
   }

   public void spans(int ticks, boolean missedSteps) {
      this.spanTicks = Math.max(1, ticks);
      this.missed = missedSteps;
   }

   public void step(Vec3[] worldTargets, double[] worldSupport, PointChain.PushOut pushOut, PointChain.Params params, double muscle) {
      this.step(worldTargets, null, worldSupport, pushOut, params, muscle, 0.0, null);
   }

   public void step(
      Vec3[] worldTargets,
      Vec3[] carriedTargets,
      double[] worldSupport,
      PointChain.PushOut pushOut,
      PointChain.Params params,
      double muscle,
      double airspeedShare,
      double[] radii
   ) {
      this.step(worldTargets, carriedTargets, worldSupport, pushOut, params, muscle, airspeedShare, radii, null, null);
   }

   public void step(
      Vec3[] worldTargets,
      Vec3[] carriedTargets,
      double[] worldSupport,
      PointChain.PushOut pushOut,
      PointChain.Params params,
      double muscle,
      double airspeedShare,
      double[] radii,
      PointChain.Water water,
      PointChain.Fluid fluid
   ) {
      Vec3 root = worldTargets[0];
      boolean missed = this.missed;
      this.missed = false;
      if (!this.primed || this.rootPrev == null || missed || root.distanceTo(this.rootPrev) > 6.0 * this.spanTicks) {
         this.lastReprime = this.primed && this.rootPrev != null ? root.distanceTo(this.rootPrev) : -1.0;
         this.reprimes++;
         this.reset(worldTargets, params);
         carriedTargets = null;
      }

      Vec3 dRoot = root.subtract(this.rootPrev);
      this.rootPrev = root;

      for (int i = 0; i < this.total; i++) {
         this.radius[i] = radii != null && i < radii.length && !Double.isNaN(radii[i])
            ? radii[i]
            : (i >= this.bones ? params.radiusTip() : params.radiusAt(i, this.bones));
         this.prevSupport[i] = this.support[i];
         this.support[i] = worldSupport[i];
      }

      this.flowShare = this.flowShare + ((water != null ? 0.0 : airspeedShare) - this.flowShare) * 0.3;
      double share = Math.min(1.0, Math.max(0.0, this.flowShare));
      double flow = params.flowStiffness() * share;
      this.flowStiffness = flow;
      double gravity = 0.08 * (1.0 - share);
      Vec3[] t = this.frameTargets(worldTargets, params);
      Vec3[] carried = carriedTargets == null ? t : this.frameTargets(carriedTargets, params);
      double keep = 1.0 - params.damping();
      int head = this.bones - 1;
      boolean headLed = params.headLed() && this.bones >= 2;
      this.upBias = headLed ? 0.5 : 0.0;
      this.headHeld = false;
      this.q[0] = Vec3.ZERO;
      this.qPrev[0] = Vec3.ZERO;
      this.contact[0] = false;
      this.asleep[0] = false;
      double turnMax = Math.toRadians(40.0);
      Quaterniond swing = new Quaterniond();
      Vec3 parentCur = Vec3.ZERO;

      for (int j = 1; j < this.total; j++) {
         boolean touchedLast = this.contact[j];
         this.contact[j] = false;
         this.asleep[j] = false;
         int bone = Math.min(j, this.bones - 1);
         double k = Math.min(1.0, params.stiffnessAt(bone, this.bones) * muscle + flow);
         double cap = Math.toRadians(Math.max(0.0, params.bendAt(bone, this.bones)));
         double capMax = Math.toRadians(params.coneAt(bone, this.bones));
         double rest = t[j].distanceTo(t[j - 1]);
         Vec3 authored = unit(t[j].subtract(t[j - 1]));
         Vec3 parent = this.q[j - 1];
         if (headLed && j == head && k >= 1.0) {
            this.headHeld = true;
         }

         double submerged = fluid != null && water != null ? fluid.submerged(root.add(this.q[j]), this.radius[j]) : 0.0;
         Vec3 clip = t[j].subtract(carried[j]);
         if (submerged > 0.0) {
            clip = clip.scale(1.0 - (1.0 - water.followAt(bone, this.bones)) * submerged);
         }

         Vec3 cur = this.q[j].add(clip);
         Vec3 prev = this.qPrev[j].add(clip);
         Vec3 dirStart = unit(cur.subtract(parentCur));
         parentCur = cur;
         Vec3 bendDir = authored == null ? null : rotate(swing, authored);
         Vec3 target = authored == null ? null : (headLed ? authored : bendDir);
         this.clipDir[j] = authored;
         this.coneDir[j] = bendDir;
         double r = this.radius[j];
         Vec3 p;
         if (this.sleeps(j, clip, parent, root, bendDir, capMax)) {
            this.asleep[j] = true;
            this.contact[j] = true;
            this.branch[j] = 7;
            Vec3 held = this.worldFinal[j].subtract(root);
            Vec3 d = unit(held.subtract(parent));
            p = d == null ? held : parent.add(d.scale(rest));
            this.pushed[j] = Vec3.ZERO;
         } else {
            Vec3 velocity = cur.subtract(prev).scale(1.0 - (1.0 - keep) * (1.0 - submerged));
            double weight = gravity;
            if (fluid != null && water != null) {
               velocity = waterDrag(velocity.add(dRoot), cur.subtract(parent), water, submerged).subtract(dRoot);
               weight = 0.08 * (1.0 - water.buoyancy() * submerged);
            }

            double speed = velocity.length();
            if (speed > 0.6) {
               velocity = velocity.scale(0.6 / speed);
            }

            Vec3 next = cur.add(velocity).add(0.0, -weight * (1.0 - k), 0.0);
            Vec3 dir = unit(next.subtract(parent));
            if (dir == null) {
               dir = target != null ? target : new Vec3(0.0, -1.0, 0.0);
            }

            if (target != null) {
               double angle = angleBetween(dir, target);
               if (k > 0.0 && angle > 1.0E-9) {
                  dir = rotateToward(dir, target, angle * k);
                  angle = angleBetween(dir, target);
               }

               if (angle > cap) {
                  dir = rotateToward(dir, target, angle - cap);
               }
            }

            Vec3 was = this.worldFinal[j] == null ? null : unit(cur.subtract(dRoot).subtract(parent));
            if (was != null && bendDir != null && angleBetween(was, bendDir) > capMax) {
               was = rotateToward(was, bendDir, angleBetween(was, bendDir) - capMax);
            }

            Vec3 d = this.settle(j, parent, dir, was, touchedLast, this.lastDir[j], target, cap, bendDir, capMax, rest, r, root, pushOut);
            if (dirStart != null && angleBetween(dirStart, d) > turnMax) {
               d = rotateToward(dirStart, d, turnMax);
            }

            if (bendDir != null) {
               double angle = angleBetween(d, bendDir);
               if (angle > capMax) {
                  Vec3 clamped = rotateToward(d, bendDir, angle - capMax);
                  if (this.under(j, parent, clamped, rest, r, root) > 0.01 && this.under(j, parent, d, rest, r, root) <= 0.01) {
                     Vec3 lying = this.floored(j, parent, clamped, bendDir, capMax, rest, r, root);
                     if (lying != null && this.under(j, parent, lying, rest, r, root) <= 0.01) {
                        clamped = lying;
                     }
                  }

                  d = clamped;
               }
            }

            p = parent.add(d.scale(rest));
            this.contact[j] = this.settleTouched;
            this.pushed[j] = this.settleTouched ? p.subtract(parent.add(dir.scale(rest))) : Vec3.ZERO;
         }

         this.q[j] = p;
         this.qPrev[j] = cur;
         this.targetDir[j] = target;
         Vec3 ended = unit(p.subtract(parent));
         if (ended != null) {
            this.lastDir[j] = ended;
            if (bendDir != null) {
               swing = new Quaterniond()
                  .rotationTo(bendDir.x, bendDir.y, bendDir.z, ended.x, ended.y, ended.z)
                  .mul(swing);
            }
         }
      }

      double mu = params.friction();

      for (int j = 1; j < this.total; j++) {
         this.slide[j] = 0.0;
         if (this.asleep[j]) {
            this.qPrev[j] = this.q[j].add(dRoot);
         } else {
            Vec3 w = this.q[j].subtract(this.qPrev[j]).add(dRoot);
            if (!this.contact[j]) {
               Vec3 n = unit(this.pushed[j - 1]);
               if (n != null) {
                  this.qPrev[j] = this.q[j].add(dRoot).subtract(w.subtract(n.scale(w.dot(n))));
               }
            } else {
               Vec3 n = unit(this.pushed[j]);
               if (n != null) {
                  w = w.subtract(n.scale(w.dot(n)));
               }

               Vec3 kept = new Vec3(w.x * (1.0 - mu), w.y, w.z * (1.0 - mu));
               this.slide[j] = Math.hypot(kept.x, kept.z);
               this.qPrev[j] = this.q[j].add(dRoot).subtract(kept);
            }
         }
      }

      for (int i = 0; i < this.total; i++) {
         this.prevOffset[i] = this.offset[i];
         this.offset[i] = this.q[i].subtract(t[i]);
         Vec3 w = this.q[i].add(root);
         this.motion[i] = this.worldFinal[i] == null ? Double.POSITIVE_INFINITY : w.distanceTo(this.worldFinal[i]);
         this.restTicks[i] = i > 0 && this.contact[i] && this.motion[i] < 0.002 ? this.restTicks[i] + 1 : 0;
         this.worldFinal[i] = w;
      }
   }

   private Vec3 settle(
      int j,
      Vec3 parent,
      Vec3 want,
      Vec3 was,
      boolean touchedLast,
      Vec3 held,
      Vec3 target,
      double cap,
      Vec3 clip,
      double capMax,
      double rest,
      double r,
      Vec3 root,
      PointChain.PushOut pushOut
   ) {
      this.settleTouched = false;
      pushOut.reach(parent.add(root), rest + r, j);
      double slop = 0.01;
      boolean wantClear = this.depth(j, parent, want, rest, r, root, pushOut) <= slop && !crosses(j, parent, want, rest, root, pushOut);
      boolean bound = touchedLast && was != null;
      this.branch[j] = 0;
      if (!wantClear || bound && !(angleBetween(was, want) <= cap)) {
         this.settleTouched = true;
         if (was == null) {
            this.branch[j] = 6;
            Vec3 best = this.nearestInCones(j, parent, want, target, cap, clip, capMax, rest, r, root, pushOut);
            return best != null ? best : want;
         }

         double wasDepth = this.depth(j, parent, was, rest, r, root, pushOut);
         boolean wasCrosses = crosses(j, parent, was, rest, root, pushOut);
         if (!wasCrosses && !(wasDepth > slop)) {
            if (!wantClear && wasDepth >= -slop) {
               double drift = angleBetween(was, want);
               double keep = Math.toRadians(3.0);
               boolean kept = drift <= keep;
               if (!kept && !Double.isNaN(this.support[j])) {
                  double under = this.support[j] - root.y + r - parent.add(want.scale(rest)).y;
                  kept = under > rest * (drift - keep + 1.0E-9) + 1.0E-9;
               }

               if (kept) {
                  this.branch[j] = 1;
                  return was;
               }
            }

            Vec3 best = wantClear ? want : this.nearestInCones(j, parent, want, target, cap, clip, capMax, rest, r, root, pushOut);
            if (best == null) {
               this.branch[j] = 1;
               if (wasDepth >= -slop) {
                  return was;
               }

               this.branch[j] = 5;
               return this.jammed(j, parent, want, was, held, clip, capMax, rest, r, root, pushOut);
            } else {
               double toBest = angleBetween(was, best);
               boolean touching = wasDepth >= -slop;
               this.branch[j] = 1;
               if (touching && !wantClear && angleBetween(was, want) <= this.bestAngle + Math.toRadians(3.0)) {
                  return was;
               } else {
                  this.branch[j] = 3;
                  if (bound && !(toBest <= cap)) {
                     this.branch[j] = touching ? 1 : 3;
                     return touching ? was : best;
                  } else {
                     return best;
                  }
               }
            }
         } else {
            this.branch[j] = 2;
            Vec3 out = this.nearestInCones(j, parent, was, target, cap, clip, capMax, rest, r, root, pushOut);
            if (out == null || bound && !(angleBetween(was, out) <= cap)) {
               Vec3 toward = out == null ? null : rotateToward(was, out, cap);
               if (toward != null && this.under(j, parent, toward, rest, r, root) <= slop) {
                  this.branch[j] = 4;
                  return toward;
               } else {
                  this.branch[j] = 5;
                  return this.jammed(j, parent, want, was, held, clip, capMax, rest, r, root, pushOut);
               }
            } else {
               return out;
            }
         }
      } else {
         return want;
      }
   }

   public int branch(int i) {
      return this.branch[i];
   }

   private Vec3 nearestInCones(
      int j, Vec3 parent, Vec3 from, Vec3 target, double cap, Vec3 bend, double capMax, double rest, double r, Vec3 root, PointChain.PushOut pushOut
   ) {
      long clock = CLOCK_SEARCH.start();

      try {
         return this.nearestInConesTimed(j, parent, from, target, cap, bend, capMax, rest, r, root, pushOut);
      } finally {
         CLOCK_SEARCH.end(clock);
      }
   }

   private Vec3 nearestInConesTimed(
      int j, Vec3 parent, Vec3 from, Vec3 target, double cap, Vec3 bend, double capMax, double rest, double r, Vec3 root, PointChain.PushOut pushOut
   ) {
      Vec3 best = this.nearest(j, parent, from, target, cap, null, 0.0, rest, r, root, pushOut);
      if (best == null && bend != null) {
         best = this.nearest(j, parent, from, bend, capMax, null, 0.0, rest, r, root, pushOut);
      }

      return best;
   }

   private Vec3 jammed(
      int j, Vec3 parent, Vec3 want, Vec3 was, Vec3 held, Vec3 target, double cap, double rest, double r, Vec3 root, PointChain.PushOut pushOut
   ) {
      Vec3[] candidates = new Vec3[]{
         held,
         was,
         held == null ? null : this.floored(j, parent, held, target, cap, rest, r, root),
         want,
         this.floored(j, parent, want, target, cap, rest, r, root)
      };
      double slop = 0.01;

      for (int pass = 0; pass < 2; pass++) {
         boolean crossingToo = pass == 1;
         if (held != null && (crossingToo || !crosses(j, parent, held, rest, root, pushOut)) && this.under(j, parent, held, rest, r, root) <= slop) {
            this.branch[j] = 50 + pass * 10;
            return held;
         }

         Vec3 pick = this.pickShallowest(j, parent, candidates, crossingToo, rest, r, root, pushOut);
         if (pick != null) {
            for (int c = 0; c < candidates.length; c++) {
               if (candidates[c] == pick) {
                  this.branch[j] = 51 + c + pass * 10;
               }
            }

            return pick;
         }
      }

      this.branch[j] = 59;
      Vec3 pick = null;
      double least = Double.POSITIVE_INFINITY;

      for (Vec3 c : candidates) {
         if (c != null) {
            double u = Math.max(0.0, this.under(j, parent, c, rest, r, root));
            if (pick == null || u < least - slop) {
               pick = c;
               least = u;
            }
         }
      }

      return pick != null ? pick : want;
   }

   private Vec3 pickShallowest(int j, Vec3 parent, Vec3[] candidates, boolean crossingToo, double rest, double r, Vec3 root, PointChain.PushOut pushOut) {
      double slop = 0.01;
      boolean anyAbove = false;

      for (Vec3 c : candidates) {
         if (c != null && this.under(j, parent, c, rest, r, root) <= slop) {
            anyAbove = true;
         }
      }

      Vec3 pick = null;
      double pickDepth = Double.POSITIVE_INFINITY;

      for (Vec3 c : candidates) {
         if (c != null) {
            boolean crossing = crosses(j, parent, c, rest, root, pushOut);
            if ((!crossing || crossingToo) && (!anyAbove || !(this.under(j, parent, c, rest, r, root) > slop))) {
               double d = this.depth(j, parent, c, rest, r, root, pushOut);
               if ((!crossing || !(d <= slop)) && (pick == null || d < pickDepth - slop)) {
                  pick = c;
                  pickDepth = d;
               }
            }
         }
      }

      return pick;
   }

   private Vec3 floored(int j, Vec3 parent, Vec3 want, Vec3 target, double cap, double rest, double r, Vec3 root) {
      double floor = this.floorFor(j);
      if (Double.isNaN(floor)) {
         return null;
      }

      double drop = parent.y - (floor - root.y + r);
      if (!(drop < 0.0) && !(drop > rest)) {
         Vec3 level = unit(new Vec3(want.x, 0.0, want.z));
         if (level == null) {
            return null;
         }

         double pitch = Math.asin(drop / rest);
         Vec3 d = level.scale(Math.cos(pitch)).add(0.0, -Math.sin(pitch), 0.0);
         if (target != null && angleBetween(d, target) > cap) {
            d = rotateToward(d, target, angleBetween(d, target) - cap);
         }

         return d;
      } else {
         return null;
      }
   }

   private double under(int j, Vec3 parent, Vec3 d, double rest, double r, Vec3 root) {
      double floor = this.floorFor(j);
      return Double.isNaN(floor) ? Double.NEGATIVE_INFINITY : floor - root.y + r - parent.add(d.scale(rest)).y;
   }

   private double floorFor(int j) {
      if (!Double.isNaN(this.support[j])) {
         return this.support[j];
      }

      if (!Double.isNaN(this.knownSupport[j])) {
         return this.knownSupport[j];
      }

      for (int step = 1; step < this.total; step++) {
         for (int k : new int[]{j - step, j + step}) {
            if (k >= 1 && k < this.total) {
               if (!Double.isNaN(this.support[k])) {
                  return this.support[k];
               }

               if (!Double.isNaN(this.knownSupport[k])) {
                  return this.knownSupport[k];
               }
            }
         }
      }

      return Double.NaN;
   }

   private Vec3 nearest(
      int j, Vec3 parent, Vec3 from, Vec3 target, double cap, Vec3 axis2, double cap2, double rest, double r, Vec3 root, PointChain.PushOut pushOut
   ) {
      Vec3 e1 = unit(from.cross(new Vec3(0.0, 1.0, 0.0)));
      if (e1 == null) {
         e1 = unit(from.cross(new Vec3(1.0, 0.0, 0.0)));
      }

      Vec3 e2 = from.cross(e1);
      double span = target == null ? Math.PI : Math.min(Math.PI, angleBetween(from, target) + cap);

      for (int ring = 1; ring <= 12; ring++) {
         double a = span * ring / 12.0;
         this.ringCos[ring] = Math.cos(a);
         this.ringSin[ring] = Math.sin(a);
      }

      double bestPhi = Double.NaN;
      this.bestAngle = Double.POSITIVE_INFINITY;
      double bestRank = Double.POSITIVE_INFINITY;
      int bestAz = 16;
      int first = 0;
      Vec3 hint = this.lastDir[j];
      if (hint != null) {
         double u = hint.dot(e1);
         double v = hint.dot(e2);
         if (u * u + v * v > 1.0E-12) {
            first = Math.floorMod((int)Math.round(Math.atan2(v, u) * 16.0 / (Math.PI * 2)), 16);
         }
      }

      for (int n = 0; n < 16; n++) {
         int az = n == 0 ? first : (n <= first ? n - 1 : n);
         double phi = (Math.PI * 2) * az / 16.0;
         boolean before = az < bestAz;
         double a = this.clearAlong(
            j, parent, from, e1, e2, phi, span, target, cap, axis2, cap2, rest, r, root, pushOut, bestRank, this.rankFactor(e1, e2, phi), before
         );
         double rank = this.rank(a, e1, e2, phi);
         if (rank < bestRank || before && rank == bestRank && !Double.isInfinite(rank)) {
            bestRank = rank;
            this.bestAngle = a;
            bestPhi = phi;
            bestAz = az;
         }
      }

      if (Double.isNaN(bestPhi)) {
         return null;
      }

      double step = Math.PI / 16;

      for (int i = 0; i < 4; i++) {
         for (double phi : new double[]{bestPhi - step, bestPhi + step}) {
            double a = this.clearAlong(
               j, parent, from, e1, e2, phi, span, target, cap, axis2, cap2, rest, r, root, pushOut, bestRank, this.rankFactor(e1, e2, phi), false
            );
            double rank = this.rank(a, e1, e2, phi);
            if (rank < bestRank) {
               bestRank = rank;
               this.bestAngle = a;
               bestPhi = phi;
            }
         }

         step *= 0.5;
      }

      return arc(from, e1.scale(Math.cos(bestPhi)).add(e2.scale(Math.sin(bestPhi))), this.bestAngle);
   }

   private double clearAlong(
      int j,
      Vec3 parent,
      Vec3 from,
      Vec3 e1,
      Vec3 e2,
      double phi,
      double span,
      Vec3 target,
      double cap,
      Vec3 axis2,
      double cap2,
      double rest,
      double r,
      Vec3 root,
      PointChain.PushOut pushOut,
      double bound,
      double factor,
      boolean tieWins
   ) {
      Vec3 goal = e1.scale(Math.cos(phi)).add(e2.scale(Math.sin(phi)));
      double lo = 0.0;
      double hi = -1.0;
      if (lost(lo, factor, bound, tieWins)) {
         return Double.POSITIVE_INFINITY;
      }

      for (int ring = 1; ring <= 12; ring++) {
         double a = span * ring / 12.0;
         double c = this.ringCos[ring];
         double s = this.ringSin[ring];
         double dx = from.x * c + goal.x * s;
         double dy = from.y * c + goal.y * s;
         double dz = from.z * c + goal.z * s;
         if (target != null && angle(dx, dy, dz, target) > cap + 1.0E-9 || axis2 != null && angle(dx, dy, dz, axis2) > cap2 + 1.0E-9) {
            break;
         }

         if (!this.underSupport(j, parent, dy, rest, r, root) && this.clearAt(j, parent, new Vec3(dx, dy, dz), rest, r, root, pushOut)) {
            hi = a;
            break;
         }

         lo = a;
         if (lost(lo, factor, bound, tieWins)) {
            return Double.POSITIVE_INFINITY;
         }
      }

      if (hi < 0.0) {
         return Double.POSITIVE_INFINITY;
      }

      for (int i = 0; i < 6; i++) {
         double mid = 0.5 * (lo + hi);
         double c = Math.cos(mid);
         double s = Math.sin(mid);
         double dy = from.y * c + goal.y * s;
         if (!this.underSupport(j, parent, dy, rest, r, root) && this.clearAt(j, parent, arc(from, goal, mid), rest, r, root, pushOut)) {
            hi = mid;
         } else {
            lo = mid;
            if (lost(lo, factor, bound, tieWins)) {
               return Double.POSITIVE_INFINITY;
            }
         }
      }

      return hi;
   }

   private static boolean lost(double lo, double factor, double bound, boolean tieWins) {
      double least = lo * factor;
      return tieWins ? least > bound : least >= bound;
   }

   private boolean clearAt(int j, Vec3 parent, Vec3 d, double rest, double r, Vec3 root, PointChain.PushOut pushOut) {
      Vec3 at = parent.add(d.scale(rest));
      return !Double.isNaN(this.support[j]) && this.support[j] - root.y + r - at.y > 0.0
         ? false
         : pushOut.clear(at.add(root), r, j) && !crosses(j, parent, d, rest, root, pushOut);
   }

   private static double angle(double dx, double dy, double dz, Vec3 b) {
      double c = Math.max(-1.0, Math.min(1.0, dx * b.x + dy * b.y + dz * b.z));
      return Math.acos(c);
   }

   private boolean underSupport(int j, Vec3 parent, double dy, double rest, double r, Vec3 root) {
      return !Double.isNaN(this.support[j]) && this.support[j] - root.y + r - (parent.y + dy * rest) > 0.0;
   }

   private static boolean crosses(int j, Vec3 parent, Vec3 d, double rest, Vec3 root, PointChain.PushOut pushOut) {
      return pushOut.crosses(parent.add(root), parent.add(d.scale(rest)).add(root), j);
   }

   private double depth(int j, Vec3 parent, Vec3 d, double rest, double r, Vec3 root, PointChain.PushOut pushOut) {
      Vec3 at = parent.add(d.scale(rest));
      double deep = pushOut.depth(at.add(root), r, j);
      if (!Double.isNaN(this.support[j])) {
         deep = Math.max(deep, this.support[j] - root.y + r - at.y);
      }

      return deep;
   }

   private double rank(double angle, Vec3 e1, Vec3 e2, double phi) {
      return this.upBias != 0.0 && !Double.isInfinite(angle) ? angle * this.rankFactor(e1, e2, phi) : angle;
   }

   private double rankFactor(Vec3 e1, Vec3 e2, double phi) {
      if (this.upBias == 0.0) {
         return 1.0;
      }

      double up = e1.y * Math.cos(phi) + e2.y * Math.sin(phi);
      return 1.0 - this.upBias * up;
   }

   private static Vec3 arc(Vec3 a, Vec3 goal, double angle) {
      return a.scale(Math.cos(angle)).add(goal.scale(Math.sin(angle)));
   }

   private boolean sleeps(int j, Vec3 clip, Vec3 parent, Vec3 root, Vec3 clipDirection, double capMax) {
      if (this.restTicks[j] >= 10 && this.worldFinal[j] != null && this.worldFinal[j - 1] != null) {
         if (clipDirection != null) {
            Vec3 held = unit(this.worldFinal[j].subtract(root).subtract(parent));
            if (held != null && angleBetween(held, clipDirection) > capMax) {
               return false;
            }
         }

         if (clip.length() >= 0.002) {
            return false;
         }

         if (parent.add(root).distanceTo(this.worldFinal[j - 1]) >= 0.002) {
            return false;
         }

         double s = this.support[j];
         double was = this.prevSupport[j];
         return Double.isNaN(s) != Double.isNaN(was) ? false : Double.isNaN(s) || Math.abs(s - was) < 0.002;
      } else {
         return false;
      }
   }

   private Vec3[] frameTargets(Vec3[] worldTargets, PointChain.Params params) {
      Vec3 root = worldTargets[0];
      Vec3[] t = new Vec3[this.total];

      for (int i = 0; i < this.bones; i++) {
         t[i] = worldTargets[i].subtract(root);
      }

      if (worldTargets.length > this.bones) {
         t[this.bones] = worldTargets[this.bones].subtract(root);
         return t;
      }

      Vec3 dir = this.bones >= 2 ? t[this.bones - 1].subtract(t[this.bones - 2]) : new Vec3(0.0, 0.0, 1.0);
      double last = dir.length();
      double tip = params.tip() > 0.0 ? params.tip() : last;
      if (last < 1.0E-9) {
         dir = new Vec3(0.0, 0.0, 1.0);
         last = 1.0;
      }

      t[this.bones] = t[this.bones - 1].add(dir.scale(tip / last));
      return t;
   }

   private static Vec3 unit(Vec3 v) {
      double l = v.length();
      return l < 1.0E-9 ? null : v.scale(1.0 / l);
   }

   static double angleBetween(Vec3 a, Vec3 b) {
      double c = Math.max(-1.0, Math.min(1.0, a.dot(b)));
      return Math.acos(c);
   }

   static Vec3 rotateToward(Vec3 a, Vec3 b, double angle) {
      if (angle <= 0.0) {
         return a;
      }

      Vec3 axis = a.cross(b);
      double l = axis.length();
      if (l < 1.0E-9) {
         if (a.dot(b) > 0.0) {
            return a;
         }

         axis = unit(a.cross(Math.abs(a.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0)));
      } else {
         axis = axis.scale(1.0 / l);
      }

      return rotate(a, axis, angle);
   }

   private static Vec3 rotate(Quaterniond q, Vec3 v) {
      Vector3d out = q.transform(new Vector3d(v.x, v.y, v.z));
      return new Vec3(out.x, out.y, out.z);
   }

   static Vec3 rotate(Vec3 v, Vec3 axis, double angle) {
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      return v.scale(c).add(axis.cross(v).scale(s)).add(axis.scale(axis.dot(v) * (1.0 - c)));
   }

   public Vec3 offset(int i, float partialTick) {
      return this.prevOffset[i].add(this.offset[i].subtract(this.prevOffset[i]).scale(partialTick));
   }

   public Vector3f offset(int i, float partialTick, Vector3f dest) {
      Vec3 p = this.prevOffset[i];
      Vec3 o = this.offset[i];
      return dest.set(
         (float)(p.x + (o.x - p.x) * partialTick),
         (float)(p.y + (o.y - p.y) * partialTick),
         (float)(p.z + (o.z - p.z) * partialTick)
      );
   }

   public Vec3 point(int i) {
      return this.q[i];
   }

   public Vec3 worldPoint(int i) {
      return this.rootPrev == null ? this.q[i] : this.q[i].add(this.rootPrev);
   }

   public double support(int i) {
      return this.support[i];
   }

   public boolean contact(int i) {
      return this.contact[i];
   }

   public boolean asleep(int i) {
      return this.asleep[i];
   }

   public Vec3 pushed(int i) {
      return this.pushed[i];
   }

   public double motion(int i) {
      return Double.isInfinite(this.motion[i]) ? 0.0 : this.motion[i];
   }

   public double deviationDegrees(int i) {
      return i < 1 ? 0.0 : this.deviation(i);
   }

   public boolean stuck(int i) {
      return this.contact[i] && this.slide[i] < 0.01;
   }

   public double radius(int i) {
      return this.radius[i];
   }

   public boolean anchored() {
      return this.headHeld;
   }

   public double flowStiffness() {
      return this.flowStiffness;
   }

   public double flowShare() {
      return this.flowShare;
   }

   public double baseDeviation() {
      return this.bones < 2 ? 0.0 : this.deviation(1);
   }

   public double worstBend() {
      double worst = 0.0;

      for (int j = 1; j < this.total; j++) {
         worst = Math.max(worst, this.deviation(j));
      }

      return worst;
   }

   public double absoluteDeviationDegrees(int i) {
      if (i >= 1 && this.clipDir[i] != null) {
         Vec3 now = unit(this.q[i].subtract(this.q[i - 1]));
         return now == null ? 0.0 : Math.toDegrees(angleBetween(now, this.clipDir[i]));
      } else {
         return 0.0;
      }
   }

   public double coneDeviationDegrees(int i) {
      if (i >= 1 && this.coneDir[i] != null) {
         Vec3 now = unit(this.q[i].subtract(this.q[i - 1]));
         return now == null ? 0.0 : Math.toDegrees(angleBetween(now, this.coneDir[i]));
      } else {
         return 0.0;
      }
   }

   public double worstConeExcess(PointChain.Params params) {
      double worst = 0.0;

      for (int j = 1; j < this.total; j++) {
         worst = Math.max(worst, this.coneDeviationDegrees(j) - params.coneAt(Math.min(j, this.bones - 1), this.bones));
      }

      return worst;
   }

   private double deviation(int j) {
      Vec3 now = unit(this.q[j].subtract(this.q[j - 1]));
      return now != null && this.targetDir[j] != null ? Math.toDegrees(angleBetween(now, this.targetDir[j])) : 0.0;
   }

   private static double lerp(double a, double b, double t) {
      return a + (b - a) * t;
   }

   @FunctionalInterface
   public interface Fluid {
      double submerged(Vec3 var1, double var2);
   }

   public record Params(
      double damping,
      double[] stiffness,
      double bendBase,
      double bendTip,
      double radiusBase,
      double radiusTip,
      double asleepFactor,
      double friction,
      double flowStiffness,
      double flowSpeed,
      double tip,
      boolean headLed,
      double[] bendRows,
      double[] cone
   ) {
      public Params(
         double damping,
         double[] stiffness,
         double bendBase,
         double bendTip,
         double radiusBase,
         double radiusTip,
         double asleepFactor,
         double friction,
         double flowStiffness,
         double flowSpeed,
         double tip,
         boolean headLed
      ) {
         this(damping, stiffness, bendBase, bendTip, radiusBase, radiusTip, asleepFactor, friction, flowStiffness, flowSpeed, tip, headLed, null, null);
      }

      static double sample(double[] list, int i, int bones) {
         if (list.length == 1) {
            return list[0];
         } else if (list.length == 2) {
            double t = bones <= 1 ? 0.0 : (double)i / (bones - 1);
            return PointChain.lerp(list[0], list[1], t);
         } else {
            return list[Math.min(i, list.length - 1)];
         }
      }

      public double stiffnessAt(int i, int bones) {
         if (this.stiffness != null && this.stiffness.length != 0) {
            if (this.stiffness.length == 2) {
               double t = bones <= 1 ? 0.0 : (double)i / (bones - 1);
               return PointChain.lerp(this.stiffness[0], this.stiffness[1], t);
            } else {
               return this.stiffness[Math.min(i, this.stiffness.length - 1)];
            }
         } else {
            return 0.0;
         }
      }

      public double bendAt(int i, int bones) {
         if (this.bendRows != null && this.bendRows.length > 0) {
            return sample(this.bendRows, i, bones);
         }

         double t = bones <= 1 ? 0.0 : (double)i / (bones - 1);
         return PointChain.lerp(this.bendBase, this.bendTip, t);
      }

      public double bendMax() {
         if (this.cone != null && this.cone.length != 0) {
            double widest = 0.0;

            for (int i = this.cone.length >= 3 ? 1 : 0; i < this.cone.length; i++) {
               widest = Math.max(widest, this.cone[i]);
            }

            return widest;
         } else {
            return 2.0 * Math.max(this.bendBase, this.bendTip);
         }
      }

      public double coneAt(int i, int bones) {
         double bend = this.bendAt(i, bones);
         double c = this.cone != null && this.cone.length != 0 ? sample(this.cone, i, bones) : 2.0 * bend;
         return Math.max(bend, c);
      }

      public double radiusAt(int i, int bones) {
         double t = bones <= 1 ? 0.0 : (double)i / (bones - 1);
         return PointChain.lerp(this.radiusBase, this.radiusTip, t);
      }

      public PointChain.Params scaled(double s) {
         return s == 1.0
            ? this
            : new PointChain.Params(
               this.damping,
               this.stiffness,
               this.bendBase,
               this.bendTip,
               this.radiusBase * s,
               this.radiusTip * s,
               this.asleepFactor,
               this.friction,
               this.flowStiffness,
               this.flowSpeed,
               this.tip > 0.0 ? this.tip * s : this.tip,
               this.headLed,
               this.bendRows,
               this.cone
            );
      }
   }

   @FunctionalInterface
   public interface PushOut {
      PointChain.PushOut NONE = (c, r) -> Vec3.ZERO;

      Vec3 apply(Vec3 var1, double var2);

      default double depth(Vec3 worldCentre, double r, int particle) {
         Vec3 push = this.apply(worldCentre, r);
         return push.lengthSqr() > 0.0 ? push.length() : -r;
      }

      default void reach(Vec3 worldCentre, double radius, int particle) {
      }

      default boolean crosses(Vec3 a, Vec3 b, int particle) {
         return false;
      }

      default boolean clear(Vec3 worldCentre, double r, int particle) {
         return this.depth(worldCentre, r, particle) <= 0.0;
      }
   }

   public record Water(double buoyancy, double dragAlong, double dragAcross, double[] follow) {
      public static final PointChain.Water DEFAULT = new PointChain.Water(1.05, 0.1, 0.45);

      public Water(double buoyancy, double dragAlong, double dragAcross) {
         this(buoyancy, dragAlong, dragAcross, new double[]{1.0});
      }

      public double followAt(int i, int bones) {
         return this.follow != null && this.follow.length != 0 ? PointChain.Params.sample(this.follow, i, bones) : 1.0;
      }
   }
}

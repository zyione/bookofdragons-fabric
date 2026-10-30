package net.magister.liberanimalium.render.limbs;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.BoneFk;
import net.magister.liberanimalium.anim.rig.Ease;
import net.magister.liberanimalium.anim.rig.FootPlant;
import net.magister.liberanimalium.anim.rig.FootStep;
import net.magister.liberanimalium.anim.rig.Footing;
import net.magister.liberanimalium.anim.rig.GaitSpec;
import net.magister.liberanimalium.anim.rig.GatherCache;
import net.magister.liberanimalium.anim.rig.HingeLeg;
import net.magister.liberanimalium.anim.rig.LevelGroundOracle;
import net.magister.liberanimalium.anim.rig.LimbDef;
import net.magister.liberanimalium.anim.rig.Penetration;
import net.magister.liberanimalium.anim.rig.Smoothing;
import net.magister.liberanimalium.anim.rig.StepsSpec;
import net.magister.liberanimalium.anim.rig.ZLeg;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.LimbReachComponent;
import net.magister.liberanimalium.debug.client.FaunaLayers;
import net.magister.liberanimalium.render.AttitudePass;
import net.magister.liberanimalium.render.BonePalettes;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public final class FaunaLegReachSolver {
   private static int lastLogTick = -1;
   private static final PerfClock.Section CLOCK_PROBE = PerfClock.client("rig.limbs.probe");
   static final LimbTrace UNARMED = new LimbTrace();
   private static final Quaternionf HINGE_Q = new Quaternionf();
   private static volatile int configEpoch;

   private FaunaLegReachSolver() {
   }

   private static boolean drawing() {
      return FaunaLayers.drawing(FaunaLayers.RIG);
   }

   public static void invalidate() {
      configEpoch++;
   }

   private static double bodyCeiling(FaunaBase body, LimbRig rig, StepsSpec spec) {
      Footing foot = body.footing();
      if (rig.ceilingTick != body.tickCount) {
         float reach = 0.0F;

         for (LimbDef l : spec.limbs) {
            reach = Math.max(reach, l.reachUp());
         }

         double top = foot.y() + foot.height() - 0.01;
         double hit = foot.oracle().overhead(body, foot.x(), foot.z(), top, reach + 1.0);
         rig.ceilingTick = body.tickCount;
         rig.ceiling = Double.isNaN(hit) ? Double.POSITIVE_INFINITY : hit;
      }

      return rig.ceiling;
   }

   static void walk(LimbRig rig, Limb L, AttitudePass.Applied attitude, Map<String, float[]> bodyWrites) {
      forwardKinematics(rig, L, attitude, bodyWrites);
   }

   public static void resolve(LimbRig rig, AzBakedModel model, StepsSpec spec) {
      int epoch = configEpoch;
      if (rig.model != model || rig.spec != spec || rig.epoch != epoch) {
         rig.resolve(model, spec);
         rig.epoch = epoch;
      }
   }

   public static List<String> apply(FaunaBase body, LimbRig rig, AzBakedModel model, StepsSpec spec, Vec3 drawn, Matrix4f modelToWorld, float partialTick) {
      resolve(rig, model, spec);
      Matrix4f toModel = rig.toModel.set(modelToWorld).invert();
      boolean log = StepsDebug.log && body.tickCount % 20 == 0 && body.tickCount != lastLogTick;
      if (log) {
         lastLogTick = body.tickCount;
         StepsDebug.say(
            "[LegReach] id=%d limbs=%d transport=%s forceDelta=%.2f%n",
            body.getId(),
            spec.limbs.size(),
            body.getStateContext().getTransportMode(),
            StepsDebug.forceDelta
         );
      }

      boolean steps = body.getStateContext().getBindingResolver().stepsAllowed();
      boolean striding = steps && body.getStateContext().getBindingResolver().stepsMoving();
      rig.limbsOff = body.getStateContext().getBindingResolver().limbsOff();
      double bodyCeiling = bodyCeiling(body, rig, spec);
      AttitudePass.Applied attitude = AttitudePass.applied(body);
      Map<String, float[]> bodyWrites = AttitudePass.bodyWrites(body);
      if (attitude != null && !attitude.bone().equals(rig.attitudeBone)) {
         rig.attitudeBone = attitude.bone();

         for (Limb limb : rig.limbs) {
            limb.iAttitude = -1;
            if (limb.ancestors != null) {
               for (int a = 0; a < limb.ancestors.size(); a++) {
                  if (limb.ancestors.get(a).getName().equals(rig.attitudeBone)) {
                     limb.iAttitude = a;
                     break;
                  }
               }
            }
         }
      }

      boolean tickFrame = rig.gaitTick != body.tickCount;
      boolean armed = rig.traceSink != null && tickFrame || LimbWatch.watching(body.getId());
      if (tickFrame) {
         rig.liftSum = 0.0;
         rig.swaySum = 0.0;
         rig.strideSum = 0.0;
         rig.frontLiftSum = 0.0;
         rig.frontSwaySum = 0.0;
         rig.frontStrideSum = 0.0;
         rig.rearLiftSum = 0.0;
         rig.rearSwaySum = 0.0;
         rig.rearStrideSum = 0.0;
         rig.frontCount = 0;
         rig.rearCount = 0;
         rig.homeRightSum = 0.0;
         rig.homeForwardSum = 0.0;
         rig.standRightSum = 0.0;
         rig.standForwardSum = 0.0;
         rig.homeCount = 0;
         rig.standCount = 0;
      }

      for (int i : rig.plan.order) {
         solveLimb(body, rig, rig.limbs[i], attitude, bodyWrites, steps, striding, bodyCeiling, drawn, modelToWorld, toModel, partialTick, log, armed);
      }

      if (tickFrame) {
         rig.gaitTick = body.tickCount;
         int n = rig.limbs.length;
         double meanLift = n == 0 ? 0.0 : rig.liftSum / n;
         double meanSway = n == 0 ? 0.0 : rig.swaySum / n;
         double meanStride = n == 0 ? 0.0 : rig.strideSum / n;
         double fLift = rig.frontCount == 0 ? meanLift : rig.frontLiftSum / rig.frontCount;
         double fSway = rig.frontCount == 0 ? meanSway : rig.frontSwaySum / rig.frontCount;
         double fStride = rig.frontCount == 0 ? meanStride : rig.frontStrideSum / rig.frontCount;
         double rLift = rig.rearCount == 0 ? meanLift : rig.rearLiftSum / rig.rearCount;
         double rSway = rig.rearCount == 0 ? meanSway : rig.rearSwaySum / rig.rearCount;
         double rStride = rig.rearCount == 0 ? meanStride : rig.rearStrideSum / rig.rearCount;
         boolean stands = rig.homeCount > 0 && rig.standCount > 0;
         double supportRight = stands ? rig.standRightSum / rig.standCount - rig.homeRightSum / rig.homeCount : 0.0;
         double supportForward = stands ? rig.standForwardSum / rig.standCount - rig.homeForwardSum / rig.homeCount : 0.0;
         rig.gait = n == 0
            ? LimbReachComponent.Gait.NONE
            : new LimbReachComponent.Gait(meanLift, meanSway, meanStride, fLift, fSway, fStride, rLift, rSway, rStride, supportRight, supportForward);
      }

      if (tickFrame && LimbWatch.watching(body.getId())) {
         LimbWatch.sample(body, rig);
      }

      if (rig.traceSink != null && tickFrame) {
         Consumer<String> sink = rig.traceSink;
         rig.traceSink = null;
         sink.accept(LimbReadout.snapshot(rig));
      }

      return rig.touched;
   }

   private static void placeFoot(
      FaunaBase body,
      LimbRig rig,
      Limb L,
      LimbTrace tr,
      Vec3 clipWorld,
      Vec3 bodyWorld,
      Vec3 drawn,
      Vec3 hip,
      Vec3 lag,
      double straight,
      boolean steps,
      boolean striding,
      double bodyCeiling,
      boolean armed
   ) {
      Footing foot = body.footing();
      double hipY = hip.y;
      LimbDef limb = L.def;
      LimbState st = L.st;
      GaitSpec gait = rig.spec.gait;
      int tick = body.tickCount;
      boolean stale = st.tick != Integer.MIN_VALUE && tick - st.tick > 2;
      st.tick = tick;
      st.footTargetO = st.footTarget;
      UnaryOperator<Vec3> groundRide = foot.ride();
      if (groundRide != null) {
         st.ride(groundRide);
      }

      Vec3 was = foot.groundOld();
      if (stale) {
         st.groundY = Double.NaN;
         st.footTargetO = null;
         st.step.release();
         st.releasePivot = null;
         st.liftedAbove = Double.NaN;
         st.homePrev = null;
         st.plant.clear();
         st.swingGround = Double.NaN;
      }

      double clipAbove = clipWorld.y - drawn.y;
      if (StepsDebug.forceDelta != 0.0F) {
         st.footTarget = bodyWorld.add(0.0, StepsDebug.forceDelta, 0.0);
         st.footTargetO = st.footTarget;
         st.correction = new Vec3(0.0, StepsDebug.forceDelta, 0.0);
         st.groundY = drawn.y + StepsDebug.forceDelta;
         st.plant.clear();
         st.planted = false;
      } else {
         double vx = foot.x() - was.x;
         double vz = foot.z() - was.z;
         double speed = Math.hypot(vx, vz);
         boolean still = Math.hypot(speed, foot.y() - was.y) < 0.01;
         boolean standing = still || striding;
         Vec3 home = bodyWorld;
         boolean off = rig.limbsOff.contains(limb.name());
         boolean reach = limb.reach() && !off && foot.reaches();
         FootPlant plant = st.plant;
         boolean plantable = reach && !standing && StepsDebug.plant != StepsDebug.Plant.OFF;
         Vec3 homeVel = st.homePrev == null ? Vec3.ZERO : new Vec3(home.x - st.homePrev.x, 0.0, home.z - st.homePrev.z);
         st.homePrev = home;
         boolean held = plant.begin(
            clipWorld, home, foot.x(), foot.z(), was.x, was.z, foot.yaw(), plantable, 0.5 * straight, homeVel, lag.x, lag.z
         );
         Vec3 at = held ? plant.pin : home;
         double tiltPitch = AttitudePass.terrainPitchRadians(body);
         double tiltRoll = AttitudePass.terrainRollRadians(body);
         double yawRad = Math.toRadians(foot.yaw());
         double dxHome = at.x - foot.x();
         double dzHome = at.z - foot.z();
         double forward = dxHome * -Math.sin(yawRad) + dzHome * Math.cos(yawRad);
         double right = dxHome * -Math.cos(yawRad) + dzHome * -Math.sin(yawRad);
         double tilt = forward * Math.tan(-tiltPitch) + right * Math.tan(tiltRoll);
         double fromY = Math.min(drawn.y + foot.height() * 0.5 + tilt, bodyCeiling - 0.01);
         if (reach && !Double.isNaN(st.groundY)) {
            double over = st.groundY + 1.1 + (held ? 0.0 : Math.hypot(homeVel.x, homeVel.z));
            fromY = Math.max(fromY, Math.min(over, drawn.y + limb.reachUp()));
            fromY = Math.min(fromY, bodyCeiling - 0.01);
         }

         double upMax = drawn.y + limb.reachUp() - fromY;
         double down = fromY - (Math.min(drawn.y, hipY) + limb.reachDown());
         tr.tilt = tilt;
         tr.tiltPitch = tiltPitch;
         tr.tiltRoll = tiltRoll;
         double groundHome = Double.NaN;
         tr.rayFrom = fromY;
         tr.rayDown = down;
         tr.upMax = upMax;
         tr.ceiling = bodyCeiling;
         tr.buried = armed && reach && foot.oracle().inside(body, at.x, fromY - 0.01, at.z);
         if (reach) {
            long clock = CLOCK_PROBE.start();
            groundHome = held
               ? foot.oracle().probe(body, at.x, at.z, fromY, upMax, down)
               : foot.oracle().probe(body, home.x, home.z, fromY, upMax, down);
            CLOCK_PROBE.end(clock);
         }

         double reachTarget = Double.isNaN(groundHome) ? home.y - clipAbove : groundHome;
         FootStep step = st.step;
         step.tickClocks();
         Vec3 bodyVel = new Vec3(foot.x() - was.x, foot.y() - was.y, foot.z() - was.z);
         double yawDeltaRad = Math.toRadians(Mth.wrapDegrees(foot.yaw() - foot.yawO()));
         step.carry(bodyVel, yawDeltaRad, foot.position());
         tr.groundHome = groundHome;
         tr.clipAbove = clipAbove;
         tr.steps = steps;
         tr.why = FootStep.Trigger.NONE;
         tr.pairFree = true;
         tr.supported = true;
         tr.triggerRadius = Double.NaN;
         tr.release = Double.NaN;
         boolean lifted = step.pin != null && clipAbove - st.pinAbove > 0.15;
         if (lifted) {
            st.liftedAbove = st.pinAbove;
         }

         boolean aloft = !Double.isNaN(st.liftedAbove) && clipAbove - st.liftedAbove > 0.075;
         if (!aloft || !steps || !standing) {
            st.liftedAbove = Double.NaN;
         }

         boolean pinned = steps && standing && !lifted && !aloft && !off;
         if (!pinned && step.pin != null) {
            if (still) {
               st.releasePivot = st.footTarget;
               st.releaseTick = tick;
            } else {
               st.releasePivot = null;
            }

            step.release();
         }

         Vec3 pivotTarget;
         if (pinned) {
            if (step.pin == null) {
               double g = Double.isNaN(st.groundY) ? reachTarget : st.groundY;
               double px = plant.x(home);
               double pz = plant.z(home);
               if (!plant.planted() && st.releasePivot != null && st.footTarget != null) {
                  px = st.footTarget.x;
                  pz = st.footTarget.z;
               }

               step.place(new Vec3(px, g, pz));
               plant.clear();
               st.pinAbove = clipAbove;
               st.releasePivot = null;
            }

            double zone = limb.zone();
            double drop = hipY - bodyWorld.y;
            double reachRadius = Math.max(zone, HingeLeg.reach(straight, drop));
            double leadReach = Math.max(reachRadius, HingeLeg.reach(straight, drop * 0.5));
            double travel = Math.hypot(homeVel.x, homeVel.z);
            double ux = travel < 1.0E-6 ? 0.0 : homeVel.x / travel;
            double uz = travel < 1.0E-6 ? 0.0 : homeVel.z / travel;
            double floor = gait.strideFloor() * reachRadius;
            double trailMax = Math.min(
               reachRadius, Math.max(floor, FootStep.along(home.x - hip.x, home.z - hip.z, -ux, -uz, reachRadius))
            );
            double leadMax = Math.min(leadReach, Math.max(floor, FootStep.along(home.x - hip.x, home.z - hip.z, ux, uz, leadReach)));
            double trailCap = gait.strideShare() * trailMax;
            double radius = striding ? Math.min(zone, trailCap / gait.leashShare()) : zone + speed * gait.triggerTicks();
            double shortfall = striding ? Math.max(0.0, Math.min(zone, gait.strideShare() * reachRadius) - radius) : 0.0;
            double half = Math.min(FootStep.half(gait, reachRadius, leadReach, speed) + shortfall, gait.strideShare() * leadMax);
            double leash = striding ? FootStep.strideLeash(gait, zone, radius, half, trailCap) : FootStep.leash(gait, zone);
            tr.reach = reachRadius;
            tr.half = half;
            tr.striding = striding;
            tr.aheadOf = striding ? half : Double.NaN;
            tr.trailMax = trailMax;
            tr.leadMax = leadMax;
            tr.straight = straight;
            Vec3 aim = striding ? FootStep.ahead(home, homeVel, half) : FootStep.led(gait, home, homeVel, FootStep.leadMax(gait, zone));
            double arc = gait.liftShare() * straight;
            if (step.mode == FootStep.Mode.PINNED) {
               boolean supported = true;
               if (reach) {
                  double g = foot.oracle().column(body, step.pin.x, step.pin.z, step.pin.y + 0.1, 0.2);
                  supported = !Double.isNaN(g) && Math.abs(g - step.pin.y) <= 0.1;
               }

               boolean adjacentDown = rig.plan.down(L.index, rig.steps);
               boolean adjacentRested = rig.plan.rested(L.index, rig.steps);
               FootStep.Trigger why = !striding
                  ? step.trigger(home, radius, leash, supported, adjacentDown, adjacentRested)
                  : step.stray(home, homeVel, radius, leash, supported, adjacentDown, adjacentRested && rig.plan.pairHalfway(L.index, rig.steps));
               Vec3 landing = why == FootStep.Trigger.NONE ? null : landing(body, reach, aim, clipAbove, fromY, upMax, down);
               if (landing != null) {
                  step.begin(landing, why);
               } else {
                  step.hold();
               }

               tr.why = why;
               tr.pairFree = adjacentDown && adjacentRested;
               tr.supported = supported;
               tr.triggerRadius = radius;
            } else {
               Vec3 landing = landing(body, reach, aim, clipAbove, fromY, upMax, down);
               if (landing != null) {
                  step.retarget(landing, Smoothing.on ? gait.landingRate() : 1.0);
               }

               boolean landed = step.advance(gait, arc, homeVel.length());
               if (landed) {
                  st.pinAbove = clipAbove;
               }
            }

            pivotTarget = new Vec3(step.sole.x, step.sole.y + st.pinAbove + step.lift, step.sole.z);
            st.groundY = step.sole.y;
            tr.pin = step.pin;
            tr.homeDist = FootStep.horizontal(step.pin, home);
            tr.mode = step.mode;
            tr.stepTarget = step.mode == FootStep.Mode.PACED ? step.target : null;
            tr.stepProgress = step.progress;
            rig.homeRightSum += right;
            rig.homeForwardSum += forward;
            rig.homeCount++;
            if (step.grounded() && step.pin != null) {
               double px = step.pin.x - foot.x();
               double pz = step.pin.z - foot.z();
               rig.standRightSum = rig.standRightSum + (px * -Math.cos(yawRad) + pz * -Math.sin(yawRad));
               rig.standForwardSum = rig.standForwardSum + (px * -Math.sin(yawRad) + pz * Math.cos(yawRad));
               rig.standCount++;
            }

            if (arc > 0.0 && half > 0.0) {
               double normLift = step.lift / arc;
               double signedSway = Math.signum(right) * normLift;
               double lead = (pivotTarget.x - home.x) * -Math.sin(yawRad) + (pivotTarget.z - home.z) * Math.cos(yawRad);
               double signedStride = -Math.signum(right) * Math.max(-1.0, Math.min(1.0, lead / half));
               rig.liftSum += normLift;
               rig.swaySum += signedSway;
               rig.strideSum += signedStride;
               if (rig.plan.isFront(L.index)) {
                  rig.frontLiftSum += normLift;
                  rig.frontSwaySum += signedSway;
                  rig.frontStrideSum += signedStride;
                  rig.frontCount++;
               } else {
                  rig.rearLiftSum += normLift;
                  rig.rearSwaySum += signedSway;
                  rig.rearStrideSum += signedStride;
                  rig.rearCount++;
               }
            }
         } else if (!reach) {
            st.groundY = Double.NaN;
            pivotTarget = bleed(st, bodyWorld, still, tick, gait, tr);
         } else {
            double lift = Double.isNaN(reachTarget) ? 0.0 : reachTarget - drawn.y;
            if (!still) {
               double band = 0.1;
               st.reaching = Math.abs(lift) > (st.reaching ? band * 0.5 : band);
            }

            double asked = !still && !st.reaching ? drawn.y : reachTarget;
            boolean swinging = !still && !plant.planted() && !Double.isNaN(plant.share) && plant.share < 0.0;
            if (swinging && reach) {
               double look = 3.0;
               double ahead = foot.oracle().probe(body, home.x + homeVel.x * look, home.z + homeVel.z * look, fromY, upMax, down);
               double goal = Double.isNaN(ahead) ? asked : Math.max(asked, ahead);
               double from = Double.isNaN(st.swingGround) ? (Double.isNaN(st.groundY) ? asked : st.groundY) : st.swingGround;
               double rate = goal > from ? 0.5 : 0.6;
               st.swingGround = Ease.step(from, goal, rate, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
               asked = st.swingGround;
            } else {
               st.swingGround = Double.NaN;
            }

            tr.lift = lift;
            tr.reaching = st.reaching;
            tr.asked = asked;
            st.groundY = asked;
            double px = plant.x(home);
            double pz = plant.z(home);
            double hipDrawnY = hip.y - lag.y;
            boolean within = Math.sqrt(
                  (px - hip.x) * (px - hip.x)
                     + (pz - hip.z) * (pz - hip.z)
                     + (reachTarget + clipAbove - hipDrawnY) * (reachTarget + clipAbove - hipDrawnY)
               )
               <= 0.985 * straight;
            plant.settle(home, plantable, st.reaching || StepsDebug.plant == StepsDebug.Plant.ALWAYS, groundHome, within);
            if (plant.planted()) {
               st.groundY = groundHome;
            }

            double ground = Double.isNaN(st.groundY) ? drawn.y : st.groundY;
            double hop = Math.min(gait.liftShare() * straight, 0.25 * plant.residual()) * plant.hop();
            pivotTarget = bleed(st, new Vec3(plant.x(home), ground + clipAbove + hop, plant.z(home)), still, tick, gait, tr);
            tr.pin = plant.pin;
            tr.homeDist = plant.planted() ? FootStep.horizontal(plant.pin, home) : Double.NaN;
            tr.mode = null;
            tr.stepTarget = null;
            tr.stepProgress = 0.0;
         }

         tr.home = home;
         tr.groundEased = st.groundY;
         tr.still = still;
         tr.pinned = pinned;
         st.footTarget = pivotTarget;
         if (st.footTargetO == null) {
            st.footTargetO = st.footTarget;
         }

         st.correction = st.footTarget.subtract(home);
         boolean clipRegime = !pinned && st.releasePivot == null;
         boolean planted = clipRegime && plant.planted();
         if (planted && !st.planted) {
            st.footTargetO = st.footTarget;
         }

         Vec3 offsetNow = clipRegime
            ? new Vec3(st.footTarget.x - bodyWorld.x, st.footTarget.y - clipWorld.y, st.footTarget.z - bodyWorld.z)
            : Vec3.ZERO;
         st.offsetO = !clipRegime || !st.clipRegime && !st.planted ? offsetNow : st.offset;
         if (clipRegime && st.planted && !planted) {
            st.offsetO = new Vec3(offsetNow.x, st.offset.y, offsetNow.z);
         }

         st.offset = offsetNow;
         st.clipRegime = clipRegime && !planted;
         st.planted = planted;
         tr.planted = planted;
         tr.plantShare = plant.share;
         tr.plantReleased = plant.released;
         st.clipTicks = !reach && !pinned && st.releasePivot == null ? st.clipTicks + 1 : 0;
         tr.offset = st.offset;
         tr.clipRegime = clipRegime;
      }

      if (tr.rendered != null && drawing()) {
         double hw = 0.25;
         AABB around = new AABB(
            tr.rendered.x - hw,
            tr.rendered.y - 0.1,
            tr.rendered.z - hw,
            tr.rendered.x + hw,
            tr.rendered.y + 0.1,
            tr.rendered.z + hw
         );
         tr.renderedInside = Penetration.overlaps(
            new Vec3(tr.rendered.x, tr.rendered.y, tr.rendered.z),
            hw - 1.0E-6,
            0.05,
            st.gathers.boxes(0, tick, around, region -> GatherCache.blocks(body, region))
         );
         tr.groundUnderRendered = LevelGroundOracle.INSTANCE.column(body, tr.rendered.x, tr.rendered.z, tr.rendered.y + 0.05, 8.0);
      }
   }

   private static Vec3 bleed(LimbState st, Vec3 placement, boolean still, int tick, GaitSpec gait, LimbTrace tr) {
      if (st.releasePivot == null) {
         return placement;
      }

      if (still && Smoothing.on) {
         double k = (double)(tick - st.releaseTick) / gait.releaseTicks();
         if (k >= 1.0) {
            st.releasePivot = null;
            return placement;
         } else {
            tr.release = k;
            return st.releasePivot.lerp(placement, k * k * (3.0 - 2.0 * k));
         }
      } else {
         st.releasePivot = null;
         return placement;
      }
   }

   private static void solveLimb(
      FaunaBase body,
      LimbRig rig,
      Limb L,
      AttitudePass.Applied attitude,
      Map<String, float[]> bodyWrites,
      boolean steps,
      boolean striding,
      double bodyCeiling,
      Vec3 drawn,
      Matrix4f toWorld,
      Matrix4f toModel,
      float partialTick,
      boolean log,
      boolean armed
   ) {
      Footing foot = body.footing();
      LimbDef limb = L.def;
      AzBone rootBone = L.root;
      AzBone midBone = L.mid;
      AzBone effBone = L.eff;
      if (rootBone != null && midBone != null && effBone != null) {
         LimbState st = L.st;
         LimbTrace tr = armed ? st.trace : UNARMED;
         tr.tick = body.tickCount;
         tr.partial = partialTick;
         tr.drawn = drawn;
         tr.entity = foot.position();
         tr.yaw = foot.yaw();
         tr.skipped = null;
         if (armed) {
            tr.originWorld = toWorld(toWorld, drawn, new Vector3f(0.0F, 0.0F, 0.0F));
            tr.upWorld = toWorld(toWorld, Vec3.ZERO, new Vector3f(0.0F, 1.0F, 0.0F))
               .subtract(toWorld(toWorld, Vec3.ZERO, new Vector3f(0.0F, 0.0F, 0.0F)));
            tr.rootBefore = new float[]{rootBone.getRotX(), rootBone.getRotY(), rootBone.getRotZ()};
            tr.midBefore = new float[]{midBone.getRotX(), midBone.getRotY(), midBone.getRotZ()};
         }

         tr.rendered = drawing() ? BonePalettes.worldPosition(body.getId(), effBone.getName(), drawn) : null;
         List<AzBone> chain = L.ancestors;
         Vector3f rHip = L.rHip;
         Vector3f mKnee = L.mKnee;
         Vector3f eFoot = L.eFoot;
         Matrix4f rootParentAccum = L.rootParent;
         Matrix4f midParentAccum = L.midParent;
         Matrix4f effParentAccum = L.effParent;
         boolean procedural = attitude != null || !bodyWrites.isEmpty();
         Matrix4f effParentClip = procedural ? L.effParentClip : effParentAccum;
         Vector3f footClip = procedural ? L.footClip : eFoot;
         Vector3f joint = rig.v;
         forwardKinematics(rig, L, attitude, bodyWrites);
         if (L.iRoot >= 0 && L.iMid >= 0 && L.iEff >= 0) {
            float l1 = rHip.distance(mKnee);
            float l2 = mKnee.distance(eFoot);
            boolean zleg = L.iKnee >= 0;
            toWorld.transformPosition(joint.set(L.hipHome));
            double hipX = joint.x() + drawn.x;
            double hipY = joint.y() + drawn.y;
            double hipZ = joint.z() + drawn.z;
            if (armed) {
               tr.hip = toWorld(toWorld, drawn, rHip);
               tr.kneeAnim = toWorld(toWorld, drawn, mKnee);
               tr.foot0 = toWorld(toWorld, drawn, eFoot);
               tr.l1 = l1;
               tr.l2 = l2;
            }

            if (!(l1 < 1.0E-4F) && !(l2 < 1.0E-4F)) {
               int tick = body.tickCount;
               boolean firstFrameOfTick = st.tick != tick;
               Vec3 clipWorld = null;
               Vec3 bodyWorld = null;
               if (firstFrameOfTick || armed || log || drawing()) {
                  clipWorld = toWorld(toWorld, drawn, footClip);
                  bodyWorld = footClip == eFoot ? clipWorld : toWorld(toWorld, drawn, L.footHome);
                  tr.clipFoot = clipWorld;
                  tr.bodyFoot = bodyWorld;
               }

               if (firstFrameOfTick) {
                  double straight = zleg ? ZLeg.longest(rHip, L.kneeJoint, mKnee, eFoot, L.hinge) : l1 + l2;
                  double tx = foot.x() - drawn.x;
                  double ty = foot.y() - drawn.y;
                  double tz = foot.z() - drawn.z;
                  placeFoot(
                     body,
                     rig,
                     L,
                     tr,
                     clipWorld.add(tx, ty, tz),
                     bodyWorld.add(tx, ty, tz),
                     foot.position(),
                     new Vec3(hipX + tx, hipY + ty, hipZ + tz),
                     new Vec3(tx, ty, tz),
                     straight,
                     steps,
                     striding,
                     bodyCeiling,
                     armed
                  );
               }

               Vec3 targetFrame = st.footTargetO.lerp(st.footTarget, partialTick);
               UnaryOperator<Vec3> between = foot.rideBetween(partialTick);
               UnaryOperator<Vec3> tickRide = between == null ? null : foot.ride();
               if (tickRide != null) {
                  Vec3 carried = tickRide.apply(st.footTargetO);
                  targetFrame = between.apply(st.footTargetO).add(st.footTarget.subtract(carried).scale(partialTick));
               }

               Vector3f target;
               if (st.planted) {
                  Vec3 offset = st.offsetO.lerp(st.offset, partialTick);
                  toWorld.transformPosition(joint.set(footClip));
                  targetFrame = new Vec3(targetFrame.x, joint.y() + drawn.y + offset.y, targetFrame.z);
                  target = toModelPoint(toModel, drawn, targetFrame, L.target);
               } else if (st.clipRegime) {
                  Vec3 offset = st.offsetO.lerp(st.offset, partialTick);
                  if (footClip == eFoot && offset.lengthSqr() == 0.0) {
                     target = L.target.set(eFoot);
                  } else {
                     toWorld.transformPosition(joint.set(footClip));
                     float clipY = joint.y();
                     toWorld.transformPosition(joint.set(L.footHome));
                     L.target.set((float)(joint.x() + offset.x), (float)(clipY + offset.y), (float)(joint.z() + offset.z));
                     target = toModel.transformPosition(L.target);
                  }

                  if (armed || log) {
                     targetFrame = toWorld(toWorld, drawn, target);
                  }
               } else {
                  target = toModelPoint(toModel, drawn, targetFrame, L.target);
               }

               tr.target0 = st.footTarget;
               tr.target = targetFrame;
               if (armed) {
                  tr.hipToTarget = tr.hip.distanceTo(targetFrame);
               }

               tr.kneeSolved = null;
               tr.endSolved = null;
               if (log) {
                  StepsDebug.say(
                     "[LegReach]   %s bodyFoot=(%.2f,%.2f,%.2f) above=%.3f corr=(%.3f,%.3f,%.3f) ground=%.2f eased=%.2f%n",
                     limb.foot(),
                     bodyWorld.x,
                     bodyWorld.y,
                     bodyWorld.z,
                     tr.clipAbove,
                     st.correction.x,
                     st.correction.y,
                     st.correction.z,
                     tr.groundHome,
                     st.groundY
                  );
               }

               if (drawing()) {
                  LimbReadout.drawFoot(body.getId(), bodyWorld, st);
               }

               toWorld.transformDirection(joint.set(target).sub(eFoot));
               double reachNow = joint.y();
               boolean up = reachNow > 0.0;
               float share;
               if (!Float.isNaN(StepsDebug.poseShare)) {
                  up = StepsDebug.poseShare > 0.0F;
                  share = Math.abs(StepsDebug.poseShare);
               } else {
                  float reach = up ? limb.reachUp() : -limb.reachDown();
                  share = Mth.clamp((float)Math.abs(reachNow) / Math.max(reach, 0.001F), 0.0F, 1.0F);
               }

               if (plantPose(L, up, share)) {
                  forwardKinematics(rig, L, attitude, bodyWrites);
                  l1 = rHip.distance(mKnee);
                  l2 = mKnee.distance(eFoot);
               }

               if (st.clipTicks >= 2) {
                  tr.skipped = "reach off: the animated foot, nothing written";
                  noteAfter(tr, armed, rootBone, midBone);
               } else if (target.distanceSquared(eFoot) < 1.0E-8F) {
                  tr.skipped = "target is the animated foot: nothing written";
                  noteAfter(tr, armed, rootBone, midBone);
               } else if (StepsDebug.noWrite) {
                  tr.skipped = "/rig freeze: nothing written";
                  noteAfter(tr, armed, rootBone, midBone);
               } else {
                  Quaternionf effAnimWorld = BoneFk.orthonormalRotation(effParentClip, L.effAnimWorld).mul(BoneFk.localQuat(effBone, rig.q));
                  boolean untilted = effParentClip != effParentAccum;
                  Quaternionf untilt = null;
                  if (untilted) {
                     untilt = BoneFk.orthonormalRotation(effParentClip, L.untilt).mul(BoneFk.orthonormalRotation(effParentAccum, rig.q).conjugate());
                  }

                  for (int k = 0; k < L.anchors.length; k++) {
                     L.anchorLocked[k] = false;
                     if (up) {
                        Matrix4f pa = anchorParent(rig, L, k, rootParentAccum, midParentAccum, effParentAccum);
                        if (pa != null) {
                           Quaternionf world = BoneFk.orthonormalRotation(pa, L.anchorWorld[k]).mul(BoneFk.localQuat(L.anchors[k], rig.q));
                           if (untilt != null) {
                              world.premul(untilt);
                           }

                           L.anchorLocked[k] = true;
                        }
                     }
                  }

                  aim(rig, L, target, effAnimWorld, tr);
                  if (armed) {
                     tr.endSolved = toWorld(toWorld, drawn, L.solEnd);
                     tr.kneeSolved = toWorld(toWorld, drawn, L.solMid);
                  }

                  noteAfter(tr, armed, rootBone, midBone);
                  if (drawing()) {
                     LimbReadout.collectDebugLines(body.getId(), body.position(), rootBone, midBone, effBone);
                  }
               }
            } else {
               if (log) {
                  StepsDebug.say("[LegReach]   %s LEN-TINY l1=%.5f l2=%.5f%n", limb.foot(), l1, l2);
               }
            }
         } else {
            if (log) {
               StringBuilder names = new StringBuilder();

               for (AzBone b : chain) {
                  names.append(b.getName()).append('>');
               }

               StepsDebug.say(
                  "[LegReach]   %s JOINTS-NULL chainLen=%d rHip=%b mKnee=%b eFoot=%b chain=%s%n",
                  limb.foot(),
                  chain.size(),
                  L.iRoot >= 0,
                  L.iMid >= 0,
                  L.iEff >= 0,
                  names
               );
            }
         }
      } else {
         if (log) {
            StepsDebug.say("[LegReach]   %s BONES-NULL root=%b mid=%b eff=%b%n", limb.foot(), rootBone != null, midBone != null, effBone != null);
         }
      }
   }

   static void aim(LimbRig rig, Limb L, Vector3f target, Quaternionfc effWorld, LimbTrace tr) {
      AzBone rootBone = L.root;
      AzBone midBone = L.mid;
      AzBone effBone = L.eff;
      List<AzBone> chain = L.ancestors;
      Vector3f rHip = L.rHip;
      Vector3f mKnee = L.mKnee;
      Vector3f eFoot = L.eFoot;
      Matrix4f rootParentAccum = L.rootParent;
      Matrix4f midParentAccum = L.midParent;
      float l1 = rHip.distance(mKnee);
      float l2 = mKnee.distance(eFoot);
      boolean zleg = L.iKnee >= 0;
      Vector3f solMid = L.solMid;
      Vector3f solEnd = L.solEnd;
      Vector3f footFolded = L.footFolded;
      if (zleg) {
         L.zSide = ZLeg.side(rHip, L.kneeJoint, mKnee, eFoot, L.hinge, L.zSide);
         float phi = ZLeg.fold(rHip, L.kneeJoint, mKnee, eFoot, L.hinge, rHip.distance(target), L.zSide);
         L.knee.setRotX(L.knee.getRotX() + phi);
         midBone.setRotX(midBone.getRotX() - phi);
         tr.foldDeg = Math.toDegrees(phi);
      } else {
         Quaternionf qMidParent = BoneFk.orthonormalRotation(midParentAccum, rig.q);
         L.hingeKnown = HingeLeg.observe(rHip, mKnee, eFoot, qMidParent, L.hingeLocal, L.hingeKnown);
         if (!L.hingeKnown) {
            BoneFk.localQuat(midBone, HINGE_Q).transform(L.hingeLocal.set(1.0F, 0.0F, 0.0F));
         }

         qMidParent.transform(L.hinge.set(L.hingeLocal));
         float phi = HingeLeg.fold(rHip, mKnee, eFoot, L.hinge, l1, l2, rHip.distance(target));
         HingeLeg.footAt(mKnee, eFoot, L.hinge, phi, footFolded);
         BoneFk.aimBone(midBone, midParentAccum, mKnee.x, mKnee.y, mKnee.z, eFoot.x, eFoot.y, eFoot.z, footFolded.x, footFolded.y, footFolded.z);
         tr.foldDeg = Math.toDegrees(phi);
         tr.hingeKnown = L.hingeKnown;
      }

      Matrix4f a0 = rig.walk.set(rootParentAccum);
      int a = L.iRoot;

      for (int n = chain.size(); a < n; a++) {
         AzBone b = chain.get(a);
         if (a == L.iEff) {
            a0.transformPosition(BoneFk.offsetPoint(b, footFolded));
            break;
         }

         BoneFk.mulLocal(a0, b);
      }

      BoneFk.aimBone(rootBone, rootParentAccum, rHip.x, rHip.y, rHip.z, footFolded.x, footFolded.y, footFolded.z, target.x, target.y, target.z);
      Vector3f newKnee = L.newKnee;
      Vector3f newFoot = L.newFoot;
      Matrix4f newMidParent = L.newMidParent;
      Matrix4f a1 = rig.walk.set(rootParentAccum);
      int ax = L.iRoot;

      for (int n = chain.size(); ax < n; ax++) {
         AzBone b = chain.get(ax);
         if (ax == L.iMid) {
            a1.transformPosition(BoneFk.offsetPoint(b, newKnee));
            newMidParent.set(a1);
         } else if (ax == L.iEff) {
            a1.transformPosition(BoneFk.offsetPoint(b, newFoot));
            break;
         }

         BoneFk.mulLocal(a1, b);
      }

      solMid.set(newKnee);
      solEnd.set(newFoot);
      Matrix4f effParentNew = L.effParentNew.set(newMidParent);

      for (int axx = L.iMid; axx < L.iEff; axx++) {
         BoneFk.mulLocal(effParentNew, chain.get(axx));
      }

      BoneFk.lockWorldOrientation(effBone, effParentNew, effWorld);

      for (int k = 0; k < L.anchors.length; k++) {
         if (L.anchorLocked[k]) {
            Matrix4f pa = anchorParent(rig, L, k, rootParentAccum, newMidParent, effParentNew);
            if (pa != null) {
               BoneFk.lockWorldOrientation(L.anchors[k], pa, L.anchorWorld[k]);
            }
         }
      }
   }

   private static boolean plantPose(Limb L, boolean up, float share) {
      AzBone[] poseBones = up ? L.highBones : L.lowBones;
      float[][] poseDeg = up ? L.highDeg : L.lowDeg;
      if (poseBones.length != 0 && !(share <= 0.001F)) {
         for (int i = 0; i < poseBones.length; i++) {
            AzBone b = poseBones[i];
            float[] o = poseDeg[i];
            b.setRotX(b.getRotX() + o[0] * (float) (Math.PI / 180.0) * share);
            b.setRotY(b.getRotY() + o[1] * (float) (Math.PI / 180.0) * share);
            b.setRotZ(b.getRotZ() + o[2] * (float) (Math.PI / 180.0) * share);
         }

         return true;
      } else {
         return false;
      }
   }

   private static void forwardKinematics(LimbRig rig, Limb L, AttitudePass.Applied attitude, Map<String, float[]> bodyWrites) {
      List<AzBone> chain = L.ancestors;
      Matrix4f accum = rig.accum.identity();
      Matrix4f accumClip = attitude == null && bodyWrites.isEmpty() ? null : rig.accumClip.identity();
      Matrix4f accumHome = null;
      Vector3f joint = rig.v;
      int a = 0;

      for (int n = chain.size(); a < n; a++) {
         AzBone b = chain.get(a);
         float[] add = bodyWrites.isEmpty() ? null : bodyWrites.get(b.getName());
         boolean sprung = add != null && (add[0] != 0.0F || add[1] != 0.0F || add[2] != 0.0F || add[3] != 0.0F || add[4] != 0.0F || add[5] != 0.0F);
         boolean forked = accumHome != null;
         if (forked) {
            if (a == L.iRoot) {
               accumHome.transformPosition(BoneFk.offsetPoint(b, L.hipHome));
            } else if (a == L.iEff) {
               accumHome.transformPosition(BoneFk.offsetPoint(b, L.footHome));
            }
         }

         if (accumHome == null && sprung) {
            accumHome = rig.accumHome.set(accum);
         }

         if (accumHome != null) {
            if (sprung) {
               BoneFk.mulLocal(
                  accumHome,
                  b,
                  b.getRotX() - add[0],
                  b.getRotY() - add[1],
                  b.getRotZ() - add[2],
                  b.getPosX() - add[3],
                  b.getPosY() - add[4],
                  b.getPosZ() - add[5],
                  accumHome
               );
            } else {
               BoneFk.mulLocal(accumHome, b);
            }
         }

         if (a == L.iRoot) {
            accum.transformPosition(BoneFk.offsetPoint(b, joint));
            L.rHip.set(joint);
            L.rootParent.set(accum);
            if (!forked) {
               L.hipHome.set(joint);
            }
         } else if (a == L.iKnee) {
            accum.transformPosition(BoneFk.offsetPoint(b, joint));
            L.kneeJoint.set(joint);
         } else if (a == L.iMid) {
            accum.transformPosition(BoneFk.offsetPoint(b, joint));
            L.mKnee.set(joint);
            L.midParent.set(accum);
         } else if (a == L.iEff) {
            accum.transformPosition(BoneFk.offsetPoint(b, joint));
            L.eFoot.set(joint);
            L.effParent.set(accum);
            if (!forked) {
               L.footHome.set(joint);
            }

            if (accumClip != null) {
               L.effParentClip.set(accumClip);
               accumClip.transformPosition(BoneFk.offsetPoint(b, L.footClip));
            }
         }

         BoneFk.mulLocal(accum, b);
         if (a == L.iKnee) {
            accum.getColumn(0, L.hinge).normalize();
         }

         if (accumClip != null) {
            float[] d = bodyWrites.get(b.getName());
            if (attitude != null && a == L.iAttitude) {
               BoneFk.mulLocal(
                  accumClip,
                  b,
                  attitude.restRotX(),
                  attitude.restRotY(),
                  attitude.restRotZ(),
                  attitude.restPosX(),
                  attitude.restPosY(),
                  attitude.restPosZ(),
                  accumClip
               );
            } else if (d != null) {
               BoneFk.mulLocal(
                  accumClip,
                  b,
                  b.getRotX() - d[0],
                  b.getRotY() - d[1],
                  b.getRotZ() - d[2],
                  b.getPosX() - d[3],
                  b.getPosY() - d[4],
                  b.getPosZ() - d[5],
                  accumClip
               );
            } else {
               BoneFk.mulLocal(accumClip, b);
            }
         }
      }
   }

   private static void noteAfter(LimbTrace tr, boolean armed, AzBone rootBone, AzBone midBone) {
      if (armed) {
         tr.rootAfter = new float[]{rootBone.getRotX(), rootBone.getRotY(), rootBone.getRotZ()};
         tr.midAfter = new float[]{midBone.getRotX(), midBone.getRotY(), midBone.getRotZ()};
      }
   }

   private static Matrix4f anchorParent(LimbRig rig, Limb L, int k, Matrix4f rootP, Matrix4f midP, Matrix4f effP) {
      List<AzBone> chain = L.anchorAncestors[k];
      if (chain == null) {
         return null;
      }

      int from = L.anchorFrom[k];
      Matrix4f accum = rig.walk;
      int start;
      if (from < 0) {
         accum.identity();
         start = 0;
      } else {
         accum.set(switch (L.anchorHangs[k]) {
            case 0 -> rootP;
            case 1 -> midP;
            default -> effP;
         });
         start = from;
      }

      int a = start;

      for (int n = chain.size() - 1; a < n; a++) {
         BoneFk.mulLocal(accum, chain.get(a));
      }

      return accum;
   }

   static Vec3 toWorld(Matrix4f toWorld, Vec3 drawn, Vector3f model) {
      Vector3f v = toWorld.transformPosition(new Vector3f(model));
      return new Vec3(v.x() + drawn.x, v.y() + drawn.y, v.z() + drawn.z);
   }

   static Vector3f toModelPoint(Matrix4f toModel, Vec3 drawn, Vec3 world, Vector3f dest) {
      return toModel.transformPosition(
         dest.set((float)(world.x - drawn.x), (float)(world.y - drawn.y), (float)(world.z - drawn.z))
      );
   }

   private static Vec3 landing(FaunaBase body, boolean reach, Vec3 at, double clipAbove, double fromY, double upMax, double down) {
      Footing foot = body.footing();
      if (!reach) {
         return new Vec3(at.x, at.y - clipAbove, at.z);
      }

      double ground = foot.oracle().probe(body, at.x, at.z, fromY, upMax, down);
      return Double.isNaN(ground) ? null : new Vec3(at.x, ground, at.z);
   }
}

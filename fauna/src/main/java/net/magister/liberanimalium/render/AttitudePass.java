package net.magister.liberanimalium.render;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.model.AzBoneSnapshot;
import net.magister.liberanimalium.anim.api.SignalView;
import net.magister.liberanimalium.anim.molang.AnimQueries;
import net.magister.liberanimalium.anim.molang.SpringChannelSpec;
import net.magister.liberanimalium.anim.rig.Attitude;
import net.magister.liberanimalium.anim.rig.BoneAxis;
import net.magister.liberanimalium.anim.rig.BoneFk;
import net.magister.liberanimalium.anim.rig.Ease;
import net.magister.liberanimalium.anim.rig.Footing;
import net.magister.liberanimalium.anim.rig.GaitQueries;
import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.anim.rig.RiggedBody;
import net.magister.liberanimalium.body.anim.binding.AnimSignals;
import net.magister.liberanimalium.body.anim.binding.StateSnapshot;
import net.magister.liberanimalium.body.state.TransportMode;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.LookFrame;
import net.magister.liberanimalium.chassis.component.AttitudeComponent;
import net.magister.liberanimalium.chassis.component.LimbReachComponent;
import net.magister.liberanimalium.debug.client.FaunaLayers;
import net.magister.liberanimalium.debug.client.LegReachDebugRenderer;
import net.magister.liberanimalium.render.limbs.StepsDebug;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class AttitudePass implements RigPass {
   private static final String[] PROBE_NAMES = new String[]{"front", "back", "left", "right", "centre"};
   private static final double JITTER_DECAY = 0.9;
   private static final double CARRY_MAX_STEP = 2.0;
   private static final PerfClock.Section CLOCK_PROBE = PerfClock.client("rig.attitude.probe");
   private static volatile double forcePitch = Double.NaN;
   private static volatile double forceRoll = Double.NaN;
   private static volatile double forceLean = Double.NaN;
   private static final double FALL_SHARE_OUT = 0.125;
   private static final double FALL_RAY_HEADROOM = 0.5;

   public static void force(double pitchDeg, double rollDeg, double leanDeg) {
      forcePitch = pitchDeg;
      forceRoll = rollDeg;
      forceLean = leanDeg;
   }

   public static void release() {
      forcePitch = Double.NaN;
      forceRoll = Double.NaN;
      forceLean = Double.NaN;
   }

   public static boolean forced() {
      return !Double.isNaN(forcePitch);
   }

   public static AttitudePass.Applied applied(FaunaBase body) {
      AttitudeComponent st = AttitudeComponent.of(body);
      return st != null && st.wrote ? st.applied : null;
   }

   public static double rollRadians(FaunaBase body) {
      AttitudeComponent st = AttitudeComponent.of(body);
      return st != null && st.wrote ? st.roll + st.lean : 0.0;
   }

   public static float pitchDegrees(FaunaBase body) {
      AttitudeComponent st = AttitudeComponent.of(body);
      return st != null && st.wrote ? (float)Math.toDegrees(st.pitch) : 0.0F;
   }

   public static double terrainPitchRadians(FaunaBase body) {
      AttitudeComponent st = AttitudeComponent.of(body);
      return st != null && st.wrote ? st.pitch : 0.0;
   }

   public static double terrainRollRadians(FaunaBase body) {
      AttitudeComponent st = AttitudeComponent.of(body);
      return st != null && st.wrote ? st.roll : 0.0;
   }

   public static boolean dropping(FaunaBase body) {
      AttitudeComponent st = AttitudeComponent.of(body);
      return st != null && st.dropping;
   }

   public static double shiftAt(FaunaBase body, float partialTick) {
      AttitudeComponent st = AttitudeComponent.of(body);
      if (st != null && st.wrote && st.shifting) {
         double from = st.tick == body.tickCount ? st.heightO : st.height;
         Footing foot = body.footing();
         return Attitude.shift(from, st.height, foot.yo(), foot.y(), partialTick);
      } else {
         return 0.0;
      }
   }

   @Override
   public String name() {
      return "attitude";
   }

   @Override
   public void solve(RigFrame frame) {
      JsonObject cfg = frame.steps.attitude;
      if (cfg != null && flag(cfg, "enabled", true)) {
         List<String> root = frame.spec.chain("root");
         if (!root.isEmpty()) {
            String boneName = root.get(0);
            AzBone bone = frame.model.getBoneOrNull(boneName);
            if (bone != null && frame.write.has(boneName)) {
               FaunaBase body = frame.body instanceof FaunaBase db ? db : null;
               Entity entity = frame.body.animEntity();
               AttitudeComponent st = AttitudeComponent.of(frame.body);
               if (st != null) {
                  if (st.applied == null || !st.applied.bone().equals(boneName)) {
                     AzBoneSnapshot rest = bone.getInitialAzSnapshot();
                     st.applied = rest != null
                        ? new AttitudePass.Applied(
                           boneName, rest.getRotX(), rest.getRotY(), rest.getRotZ(), rest.getOffsetX(), rest.getOffsetY(), rest.getOffsetZ()
                        )
                        : new AttitudePass.Applied(boneName, bone.getRotX(), bone.getRotY(), bone.getRotZ(), bone.getPosX(), bone.getPosY(), bone.getPosZ());
                  }

                  int curTick = entity != null ? entity.tickCount : frame.body.animTick();
                  if (st.tick != curTick) {
                     st.tick = curTick;
                     if (body != null) {
                        tick(body, cfg, st);
                     } else {
                        tickGeneric(entity, frame.body, cfg, st);
                     }

                     stepBody(frame.body, frame.steps.body, st);
                  }

                  float pt = Attitude.sampleTick(body != null && PoseInterpolator.blended(body), frame.partialTick);
                  double pitch = Mth.lerp(pt, st.pitchO, st.pitch);
                  double roll = Mth.lerp(pt, st.rollO, st.roll);
                  double lean = Mth.lerp(pt, st.leanO, st.lean);
                  boolean translate = StepsDebug.shift && flag(cfg, "translation", false);
                  double shift = 0.0;
                  st.shifting = translate && !Double.isNaN(st.height);
                  if (st.shifting) {
                     Footing foot = frame.footing();
                     double yO = foot != null ? foot.yo() : frame.drawnPosition.y;
                     double y = foot != null ? foot.y() : frame.drawnPosition.y;
                     shift = Attitude.shift(st.heightO, st.height, yO, y, pt);
                  }

                  boolean idle = Math.abs(pitch) < 0.001 && Math.abs(roll) < 0.001 && Math.abs(lean) < 0.001 && Math.abs(shift) < 0.001;
                  AttitudePass.Applied a = st.applied;
                  if (idle) {
                     if (st.wrote) {
                        frame.write.set(boneName, BoneAxis.ROT_X, a.restRotX());
                        frame.write.set(boneName, BoneAxis.ROT_Y, a.restRotY());
                        frame.write.set(boneName, BoneAxis.ROT_Z, a.restRotZ());
                        frame.write.set(boneName, BoneAxis.POS_X, a.restPosX());
                        frame.write.set(boneName, BoneAxis.POS_Y, a.restPosY());
                        frame.write.set(boneName, BoneAxis.POS_Z, a.restPosZ());
                        st.wrote = false;
                        st.write = Attitude.Write.NONE;
                     }
                  } else {
                     Footing stand = frame.footing();
                     float yaw = stand != null ? Mth.rotLerp(frame.partialTick, stand.yawO(), stand.yaw()) : 0.0F;
                     double holdX = 0.0;
                     double holdY = 0.0;
                     double holdZ = 0.0;
                     double fallShare = Mth.lerp(pt, st.fallShareO, st.fallShare);
                     if (translate || fallShare > 0.0) {
                        Vector3f pivotWorld = frame.modelToStand()
                           .transformPosition(new Vector3f(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F));
                        if (translate) {
                           holdX = -pivotWorld.x;
                           holdY = -pivotWorld.y;
                           holdZ = -pivotWorld.z;
                        }

                        if (fallShare > 0.0 && entity != null) {
                           holdX += (-pivotWorld.x - holdX) * fallShare;
                           holdY += (entity.getBbHeight() * 0.5 - pivotWorld.y - holdY) * fallShare;
                           holdZ += (-pivotWorld.z - holdZ) * fallShare;
                        }
                     }

                     Attitude.Write w = Attitude.write(frame.modelToStand(), yaw, pitch, roll, lean, holdX, holdY, holdZ, shift);
                     frame.write.set(boneName, BoneAxis.ROT_X, (float)(a.restRotX() + w.rotX()));
                     frame.write.set(boneName, BoneAxis.ROT_Y, (float)(a.restRotY() + w.rotY()));
                     frame.write.set(boneName, BoneAxis.ROT_Z, (float)(a.restRotZ() + w.rotZ()));
                     frame.write.set(boneName, BoneAxis.POS_X, (float)(a.restPosX() + w.posX()));
                     frame.write.set(boneName, BoneAxis.POS_Y, (float)(a.restPosY() + w.posY()));
                     frame.write.set(boneName, BoneAxis.POS_Z, (float)(a.restPosZ() + w.posZ()));
                     st.wrote = true;
                     st.write = w;
                     if (body != null && FaunaLayers.drawing(FaunaLayers.RIG)) {
                        draw(body, st, pitch, roll);
                     }
                  }
               }
            }
         }
      }
   }

   private static void tick(FaunaBase body, JsonObject cfg, AttitudeComponent st) {
      st.pitchO = st.pitch;
      st.rollO = st.roll;
      st.leanO = st.lean;
      st.heightO = st.height;
      st.fallShareO = st.fallShare;
      boolean gate = !body.isFlying()
         && !body.isPassenger()
         && (body.getTransportMode() == TransportMode.GROUNDED || body.isClimbing())
         && RigLod.of(body).legsActive();
      st.gate = gate;
      st.springs = gate;
      Footing foot = body.footing();
      if (st.footing != foot) {
         st.footing = foot;
         st.height = st.heightO = Double.NaN;
         st.castX = Double.NaN;
         st.measured = false;
         Arrays.fill(st.grounds, Double.NaN);
      }

      if (!gate && !forced()) {
         if (st.falling && st.fallShare > 0.0 && body.isFlying() && !body.isControlledByLocalInstance()) {
            float nose = (float)Math.toDegrees(st.pitch);
            body.setXRot(nose);
            body.xRotO = nose;
         }

         st.fallen = 0.0;
         st.fallShare = st.fallShareO = 0.0;
         st.pitch = st.pitchO = 0.0;
         st.roll = st.rollO = 0.0;
         st.lean = st.leanO = 0.0;
         st.height = st.heightO = Double.NaN;
         st.leanRaw = 0.0;
         st.measured = false;
         st.falling = false;
         st.dropping = false;
         st.castX = Double.NaN;
         st.cross = Attitude.Cross.NONE;
         Arrays.fill(st.probes, null);
         Arrays.fill(st.grounds, Double.NaN);
         Arrays.fill(st.voids, false);
         if (body.getVehicle() instanceof Player && RigLod.of(body).legsActive()) {
            gaitVars(body, foot, st, 0.0, 0.0);
            shoulderLoad(st);
            st.springs = true;
         }
      } else {
         double pitch = 0.0;
         double roll = 0.0;
         double lean = 0.0;
         double height = foot.y();
         double centreX = foot.x();
         double centreZ = foot.z();
         double floorY = foot.y();
         double dx = centreX - st.centreX;
         double dz = centreZ - st.centreZ;
         boolean carry = st.measured && !Double.isNaN(st.height) && Math.abs(dx) < 2.0 && Math.abs(dz) < 2.0;
         double carried = carry ? st.cross.gradX() * dx + st.cross.gradZ() * dz : 0.0;
         st.centreX = centreX;
         st.centreY = floorY;
         st.centreZ = centreZ;
         st.measured = false;
         st.cross = Attitude.Cross.NONE;
         JsonObject probeCfg = cfg.has("probes") && cfg.get("probes").isJsonObject() ? cfg.getAsJsonObject("probes") : null;
         double box = body.getBbWidth() * 0.5;
         st.forwardArm = num(probeCfg, "forward", box) + Math.hypot(foot.x() - foot.groundOld().x, foot.z() - foot.groundOld().z) * 4.0;
         st.backArm = num(probeCfg, "back", box);
         st.sideArm = num(probeCfg, "side", box);
         st.maxSlope = Math.toRadians(num(cfg, "max_slope", 45.0));
         st.reach = Attitude.boxReach(box, foot.yaw());
         double dy = foot.y() - foot.yo();
         boolean wasAloft = st.falling;
         double groundSpeed = Math.hypot(foot.x() - foot.groundOld().x, foot.z() - foot.groundOld().z);
         boolean held = foot.onGround() || wasAloft || foot.held(1.1 + groundSpeed * 3.0);
         st.falling = Attitude.aloft(wasAloft, foot.onGround(), dy, held, body.hopping());
         st.dropping = st.falling && (wasAloft ? st.dropping : !held && dy <= 0.0 && !body.hopping());
         if (st.falling && !forced()) {
            st.castX = Double.NaN;
            Arrays.fill(st.grounds, Double.NaN);
            Arrays.fill(st.voids, false);
            st.speed = Math.hypot(dx, dz);
            pitch = Attitude.fallPitch(dy, st.speed, st.maxSlope);
            pitch *= share(cfg, "pitch", true);
            st.leanRaw = 0.0;
            double turn = Math.toRadians(4.0);
            if (!wasAloft) {
               st.fallen = 0.0;
               st.fallBase = st.pitch;
            }

            st.fallBase = Ease.step(st.fallBase, pitch, 0.35, turn);
            if (dy < 0.0) {
               st.fallen -= dy;
            }

            double left = 6.0 - st.fallen;
            boolean caught = body.canFly()
               && !body.isInWater()
               && (left <= 0.0 || Double.isNaN(foot.oracle().column(body, centreX, centreZ, floorY + 0.5, left + 0.5)));
            st.fallShare = Math.max(caught ? Attitude.fallShare(st.fallen, 6.0) : 0.0, st.fallShare - 0.125);
            st.pitch = st.fallBase + (Attitude.pathPitch(dy, st.speed) - st.fallBase) * st.fallShare;
            st.roll = Ease.step(st.roll, 0.0, 0.35, turn);
            st.lean = Ease.step(st.lean, 0.0, 0.35, turn);
            if (!Double.isNaN(st.height)) {
               st.height = Ease.step(st.height + dy, floorY, 1.0, 0.2, 1.0);
            }

            st.jitterPitch = Math.max(st.jitterPitch * 0.9, Math.abs(st.pitch - st.pitchO));
         } else {
            st.fallen = 0.0;
            st.fallShare = Math.max(0.0, st.fallShare - 0.125);
            if (forced()) {
               pitch = Math.toRadians(forcePitch);
               roll = Math.toRadians(forceRoll);
               lean = Math.toRadians(Double.isNaN(forceLean) ? 0.0 : forceLean);
            } else if (gate) {
               float yaw = foot.yaw();
               double longestArm = Math.max(Math.max(st.forwardArm, st.backArm), st.sideArm);
               double swept = longestArm * Math.abs(Math.toRadians(Mth.wrapDegrees(yaw - st.castYaw)));
               boolean still = !Double.isNaN(st.castX)
                  && Math.abs(centreX - st.castX) < 0.01
                  && Math.abs(centreZ - st.castZ) < 0.01
                  && Math.abs(floorY - st.castY) < 0.01
                  && swept < 0.01
                  && foot.ride() == null
                  && body.tickCount - st.castTick < 10;
               if (still) {
                  st.reused++;
               } else {
                  st.reused = 0;
                  st.castX = centreX;
                  st.castZ = centreZ;
                  st.castY = floorY;
                  st.castYaw = yaw;
                  st.castTick = body.tickCount;
                  long clock = CLOCK_PROBE.start();
                  double[] forward = new double[]{st.forwardArm, -st.backArm, 0.0, 0.0, 0.0};
                  double[] side = new double[]{0.0, 0.0, -st.sideArm, st.sideArm, 0.0};
                  double[] arm = new double[]{st.forwardArm, st.backArm, st.sideArm, st.sideArm, 0.0};
                  double[] down = new double[]{st.forwardArm, st.backArm, st.sideArm, st.sideArm, st.reach};

                  for (int i = 0; i < 5; i++) {
                     Attitude.Probe p = Attitude.at(centreX, centreZ, yaw, forward[i], side[i]);
                     double band = num(cfg, "band", num(probeCfg, "band", Attitude.band(down[i], st.maxSlope)));
                     st.probes[i] = p;
                     st.bands[i] = band;
                     double start = foot.height() * 0.5;
                     double top = Attitude.band(arm[i], st.maxSlope);
                     double lift = Mth.clamp(Attitude.startLift(forward[i], side[i], st.pitch, st.roll), -start - band + 0.1, top - start);
                     st.starts[i] = start + lift;
                     if (!Double.isNaN(st.grounds[i]) && arm[i] > 0.0) {
                        double over = st.grounds[i] - floorY + 1.1 + Math.hypot(dx, dz) + swept;
                        st.starts[i] = Math.max(st.starts[i], Math.min(over, top));
                     }

                     st.ups[i] = top - st.starts[i];
                     st.grounds[i] = foot.oracle().probe(body, p.x(), p.z(), floorY + st.starts[i], st.ups[i], st.starts[i] + band);
                     st.voids[i] = Double.isNaN(st.grounds[i]) && !foot.oracle().missedHigh(body, p.x(), p.z(), floorY + st.starts[i]);
                  }

                  CLOCK_PROBE.end(clock);
               }

               Attitude.Edges edges = (along, from, surface) -> {
                  Attitude.Probe at = along ? Attitude.at(centreX, centreZ, yaw, from, 0.0) : Attitude.at(centreX, centreZ, yaw, 0.0, from);
                  double share = foot.oracle().edge(body, at.x(), at.z(), centreX, centreZ, surface);
                  return Double.isNaN(share) ? Double.NaN : from * (1.0 - share);
               };
               double[] found = new double[5];

               for (int i = 0; i < 5; i++) {
                  found[i] = st.voids[i] ? Double.NEGATIVE_INFINITY : st.grounds[i];
               }

               Attitude.Cross cross = Attitude.cross(
                  found[0],
                  found[1],
                  found[2],
                  found[3],
                  found[4],
                  foot.onGround() ? floorY : Double.NaN,
                  st.forwardArm,
                  st.backArm,
                  st.sideArm,
                  yaw,
                  st.maxSlope,
                  st.reach,
                  edges
               );
               st.cross = cross;
               st.measured = true;
               pitch = cross.pitch();
               roll = cross.roll();
               double pitchShare = share(cfg, "pitch", true);
               double rollShare = share(cfg, "roll", false);
               pitch = pitchShare == 0.0 ? 0.0 : pitch * pitchShare;
               roll = rollShare == 0.0 ? 0.0 : roll * rollShare;
               if (!Double.isNaN(cross.height())) {
                  height = cross.height();
               }

               st.speed = Math.hypot(foot.x() - foot.groundOld().x, foot.z() - foot.groundOld().z);
               st.yawRate = Math.toRadians(Mth.wrapDegrees(foot.yaw() - foot.yawO()));
               lean = Attitude.lean(st.speed, st.yawRate, 0.08) * num(cfg, "lean", 1.0);
            }

            gaitVars(body, foot, st, dx, dz);
            st.leanRaw = lean;
            st.writes = (share(cfg, "pitch", true) > 0.0 ? "pitchx" + share(cfg, "pitch", true) + " " : "")
               + (share(cfg, "roll", false) > 0.0 ? "rollx" + share(cfg, "roll", false) + " " : "")
               + (num(cfg, "lean", 1.0) != 0.0 ? "leanx" + num(cfg, "lean", 1.0) + " " : "")
               + (flag(cfg, "translation", false) ? "hold+shift" : "");
            if (st.writes.isBlank()) {
               st.writes = "nothing";
            }

            if (forced()) {
               st.pitch = pitch;
               st.roll = roll;
               st.lean = lean;
               st.height = Double.NaN;
            } else {
               st.height = Double.isNaN(st.height) ? floorY : st.height + carried;
               double turn = Math.toRadians(4.0);
               st.pitch = Ease.step(st.pitch, pitch, 0.35, turn);
               st.roll = Ease.step(st.roll, roll, 0.35, turn);
               st.lean = Ease.step(st.lean, lean, 0.35, turn);
               double heightRate = height < st.height ? 1.0 : 0.35;
               st.height = Ease.step(st.height, height, heightRate, 0.2, 1.0);
               st.jitterPitch = Math.max(st.jitterPitch * 0.9, Math.abs(st.pitch - st.pitchO));
            }
         }
      }
   }

   private static void gaitVars(Entity entity, Footing foot, AttitudeComponent st, double dx, double dz) {
      float bodyYaw = foot != null ? foot.yaw() : 0.0F;
      float bodyYawO = foot != null ? foot.yawO() : 0.0F;
      double yaw = Math.toRadians(bodyYaw);
      double fx = -Math.sin(yaw);
      double fz = Math.cos(yaw);
      double rx = -Math.cos(yaw);
      double rz = -Math.sin(yaw);
      LimbReachComponent limbs = entity instanceof FaunaBase body ? LimbReachComponent.of(body) : null;
      LimbReachComponent.Gait gait = limbs == null ? LimbReachComponent.Gait.NONE : limbs.gait();
      int tick = entity != null ? entity.tickCount : st.motionTick + 1;
      if (tick != st.motionTick + 1) {
         st.motion.reset();
      }

      st.motionTick = tick;
      if (foot != null) {
         st.motion.step(foot.x(), foot.y(), foot.z(), 1.0, 1.5);
      }

      double[] v = st.gaitVars;
      v[GaitQueries.Var.FRONT_SWAY.ordinal()] = gait.frontSway();
      v[GaitQueries.Var.REAR_SWAY.ordinal()] = gait.rearSway();
      v[GaitQueries.Var.FRONT_LIFT.ordinal()] = gait.frontLift();
      v[GaitQueries.Var.REAR_LIFT.ordinal()] = gait.rearLift();
      v[GaitQueries.Var.FRONT_STRIDE.ordinal()] = gait.frontStride();
      v[GaitQueries.Var.REAR_STRIDE.ordinal()] = gait.rearStride();
      v[GaitQueries.Var.SWAY.ordinal()] = gait.sway();
      v[GaitQueries.Var.LIFT.ordinal()] = gait.lift();
      v[GaitQueries.Var.STRIDE.ordinal()] = gait.stride();
      v[GaitQueries.Var.SPEED.ordinal()] = Math.hypot(dx, dz);
      v[GaitQueries.Var.FORWARD_SPEED.ordinal()] = dx * fx + dz * fz;
      v[GaitQueries.Var.LATERAL_SPEED.ordinal()] = dx * rx + dz * rz;
      v[GaitQueries.Var.YAW_RATE.ordinal()] = Mth.wrapDegrees(bodyYaw - bodyYawO);
      v[GaitQueries.Var.ACCEL_FORWARD.ordinal()] = st.motion.accelAlong(fx, 0.0, fz) / 0.08;
      v[GaitQueries.Var.ACCEL_RIGHT.ordinal()] = st.motion.accelAlong(rx, 0.0, rz) / 0.08;
      v[GaitQueries.Var.SUPPORT_RIGHT.ordinal()] = gait.supportRight();
      v[GaitQueries.Var.SUPPORT_FORWARD.ordinal()] = gait.supportForward();
   }

   private static void shoulderLoad(AttitudeComponent st) {
      double[] v = st.gaitVars;
      double push = -st.motion.accelAlong(0.0, 1.0, 0.0) / 0.08;
      v[GaitQueries.Var.SWAY.ordinal()] = v[GaitQueries.Var.FRONT_SWAY.ordinal()] = v[GaitQueries.Var.REAR_SWAY.ordinal()] = 0.0;
      v[GaitQueries.Var.STRIDE.ordinal()] = v[GaitQueries.Var.FRONT_STRIDE.ordinal()] = v[GaitQueries.Var.REAR_STRIDE.ordinal()] = 0.0;
      v[GaitQueries.Var.LIFT.ordinal()] = v[GaitQueries.Var.FRONT_LIFT.ordinal()] = push;
      v[GaitQueries.Var.REAR_LIFT.ordinal()] = push * 0.5;
   }

   private static void stepBody(RiggedBody body, SpringChannelSpec spec, AttitudeComponent st) {
      if (!spec.isEmpty()) {
         if (!st.springs && !forced()) {
            st.body.reset();
         } else {
            st.snapshot = body instanceof FaunaBase fauna ? StateSnapshot.from(fauna) : null;
            boolean live = st.springs && body.animStepsAllowed();
            SignalView previous = AnimQueries.bound();
            AnimQueries.install(AnimSignals.scalarKeys());
            AnimQueries.bind(st.snapshot);
            GaitQueries.NS.bind(st.gaitVars);

            try {
               st.body.step(spec, 0.05, live);
            } finally {
               GaitQueries.NS.unbind();
               AnimQueries.bind(previous);
            }
         }
      }
   }

   public static void applyBody(RigFrame frame) {
      AttitudeComponent st = AttitudeComponent.of(frame.body);
      if (st != null) {
         SpringChannelSpec spec = frame.steps.body;
         boolean live = !spec.isEmpty() && st.springs;

         for (float[] d : st.bodyWrites.values()) {
            Arrays.fill(d, 0.0F);
         }

         if (live) {
            SignalView previous = AnimQueries.bound();
            AnimQueries.bind(st.snapshot);
            GaitQueries.NS.bind(st.gaitVars);

            try {
               st.body.evaluate(spec, frame.partialTick, (ch, val) -> {
                  BoneAxis axisx = BoneAxis.byKey(ch.dof);
                  if (axisx != null && axisx.ordinal() < 6 && frame.write.has(ch.bone)) {
                     boolean rotation = axisx.ordinal() < 3;
                     st.bodyWrites.computeIfAbsent(ch.bone, b -> new float[6])[axisx.ordinal()] += rotation ? (float)Math.toRadians(val) : (float)val;
                  }
               });
            } finally {
               GaitQueries.NS.unbind();
               AnimQueries.bind(previous);
            }
         }

         List<String> order = new ArrayList<>(st.bodyWrites.keySet());
         order.sort(Comparator.comparingInt(b -> depth(frame, b)));

         for (String bone : order) {
            float[] delta = st.bodyWrites.get(bone);
            float[] last = st.bodyLast.computeIfAbsent(bone, b -> new float[]{Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN});
            float[] cur = new float[6];
            float[] base = new float[6];
            float[] value = new float[6];

            for (int i = 0; i < 6; i++) {
               cur[i] = frame.write.read(bone, BoneAxis.values()[i]);
               boolean untouched = !Float.isNaN(last[i]) && Math.abs(cur[i] - last[i]) < 1.0E-6F;
               base[i] = untouched ? cur[i] - st.lastDelta(bone, i) : cur[i];
               value[i] = base[i] + delta[i];
            }

            if (delta[1] != 0.0F) {
               vertical(frame, bone, base, value, delta[1]);

               for (int i = 0; i < 3; i++) {
                  delta[i] = value[i] - base[i];
               }
            }

            for (int i = 0; i < 6; i++) {
               float added = value[i] - base[i];
               if (added != 0.0F || !Float.isNaN(last[i])) {
                  BoneAxis axis = BoneAxis.values()[i];
                  if (Math.abs(value[i] - cur[i]) > 1.0E-7F) {
                     frame.write.add(bone, axis, value[i] - cur[i]);
                  }

                  last[i] = added == 0.0F ? Float.NaN : value[i];
                  st.setLastDelta(bone, i, added);
               }
            }
         }
      }
   }

   private static void vertical(RigFrame frame, String bone, float[] base, float[] value, float yaw) {
      AzBone az = frame.model.getBoneOrNull(bone);
      if (az != null) {
         List<AzBone> chain = BoneFk.ancestorsTopDown(az, frame.model);
         double[] parents = LookFrame.IDENTITY;

         for (int i = 0; i < chain.size() - 1; i++) {
            AzBone p = chain.get(i);
            parents = LookFrame.mul(parents, LookFrame.of(p.getRotX(), p.getRotY(), p.getRotZ()));
         }

         float[] e = LookFrame.turn(parents, value[0], base[1], value[2], LookFrame.rotY(yaw));

         for (int i = 0; i < 3; i++) {
            value[i] = base[i] + (float)Math.IEEEremainder(e[i] - base[i], Math.PI * 2);
         }
      }
   }

   private static int depth(RigFrame frame, String bone) {
      AzBone az = frame.model.getBoneOrNull(bone);
      return az == null ? 0 : BoneFk.ancestorsTopDown(az, frame.model).size();
   }

   public static Map<String, float[]> bodyWrites(FaunaBase body) {
      AttitudeComponent st = AttitudeComponent.of(body);
      return st == null ? Map.of() : st.bodyWrites;
   }

   private static void tickGeneric(Entity entity, RiggedBody body, JsonObject cfg, AttitudeComponent st) {
      st.pitchO = st.pitch;
      st.rollO = st.roll;
      st.leanO = st.lean;
      st.heightO = st.height;
      boolean gate = body.animGrounded() && (entity == null || !entity.isPassenger() && RigLod.of(entity).legsActive());
      st.gate = gate;
      st.springs = gate;
      if (!gate && !forced()) {
         st.pitch = st.pitchO = 0.0;
         st.roll = st.rollO = 0.0;
         st.lean = st.leanO = 0.0;
         st.height = st.heightO = Double.NaN;
         st.leanRaw = 0.0;
         st.measured = false;
         st.falling = false;
         st.castX = Double.NaN;
         st.cross = Attitude.Cross.NONE;
         Arrays.fill(st.probes, null);
         Arrays.fill(st.grounds, Double.NaN);
      } else {
         double pitch = 0.0;
         double roll = 0.0;
         double lean = 0.0;
         double height = entity != null ? entity.getY() : 0.0;
         double centreX = entity != null ? entity.getX() : 0.0;
         double centreZ = entity != null ? entity.getZ() : 0.0;
         double floorY = entity != null ? entity.getY() : 0.0;
         double dx = centreX - st.centreX;
         double dz = centreZ - st.centreZ;
         boolean carry = st.measured && !Double.isNaN(st.height) && Math.abs(dx) < 2.0 && Math.abs(dz) < 2.0;
         double carried = carry ? st.cross.gradX() * dx + st.cross.gradZ() * dz : 0.0;
         st.centreX = centreX;
         st.centreY = floorY;
         st.centreZ = centreZ;
         st.measured = false;
         st.cross = Attitude.Cross.NONE;
         Footing foot = entity == null ? null : body.animFooting();
         gaitVars(entity, foot, st, dx, dz);
         if (forced()) {
            pitch = Math.toRadians(forcePitch);
            roll = Math.toRadians(forceRoll);
            lean = Double.isNaN(forceLean) ? 0.0 : Math.toRadians(forceLean);
         }

         double turn = Math.toRadians(4.0);
         st.pitch = Ease.step(st.pitch, pitch, 0.35, turn);
         st.roll = Ease.step(st.roll, roll, 0.35, turn);
         st.lean = Ease.step(st.lean, lean, 0.35, turn);
         if (Double.isNaN(st.height)) {
            st.height = height;
         } else {
            st.height = height < st.height ? Ease.step(st.height + carried, height, 1.0, 1.0) : Ease.step(st.height + carried, height, 0.35, 0.2);
         }
      }
   }

   private static void draw(FaunaBase body, AttitudeComponent st, double pitch, double roll) {
      int id = body.getId();
      Footing foot = body.footing();

      for (int i = 0; i < st.probes.length; i++) {
         Attitude.Probe p = st.probes[i];
         if (p != null) {
            boolean found = !Double.isNaN(st.grounds[i]);
            double y = found ? st.grounds[i] : st.centreY;
            cross(foot, id, p.x(), y, p.z(), 0.25F, found ? 0.2F : 1.0F, found ? 1.0F : (st.voids[i] ? 0.85F : 0.2F), 0.2F);
            line(foot, id, p.x(), st.centreY - st.bands[i], p.z(), p.x(), st.centreY + st.starts[i], p.z(), 0.3F, 0.3F, 0.6F);
         }
      }

      double py = Double.isNaN(st.cross.height()) ? st.centreY : st.cross.height();
      float yaw = foot.yaw();
      double sin = Math.sin(Math.toRadians(yaw));
      double cos = Math.cos(Math.toRadians(yaw));
      double halfF = Math.max(st.forwardArm, st.backArm);
      double halfR = st.sideArm;
      double[][] corners = new double[][]{{halfR, halfF}, {-halfR, halfF}, {-halfR, -halfF}, {halfR, -halfF}};
      double[] wx = new double[4];
      double[] wy = new double[4];
      double[] wz = new double[4];

      for (int i = 0; i < 4; i++) {
         double r = corners[i][0];
         double f = corners[i][1];
         wx[i] = st.centreX + f * -sin + r * -cos;
         wz[i] = st.centreZ + f * cos + r * -sin;
         wy[i] = py - f * Math.tan(pitch) + r * Math.tan(roll);
      }

      for (int i = 0; i < 4; i++) {
         int j = (i + 1) % 4;
         line(foot, id, wx[i], wy[i], wz[i], wx[j], wy[j], wz[j], 1.0F, 0.0F, 1.0F);
      }

      cross(foot, id, st.centreX, py, st.centreZ, 0.2F, 1.0F, 0.0F, 1.0F);
   }

   private static void cross(Footing foot, int id, double x, double y, double z, float size, float r, float g, float b) {
      Vec3 at = foot.toWorld(new Vec3(x, y, z));
      LegReachDebugRenderer.cross(id, at.x, at.y, at.z, size, r, g, b);
   }

   private static void line(Footing foot, int id, double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b) {
      Vec3 from = foot.toWorld(new Vec3(x0, y0, z0));
      Vec3 to = foot.toWorld(new Vec3(x1, y1, z1));
      LegReachDebugRenderer.line(id, from.x, from.y, from.z, to.x, to.y, to.z, r, g, b);
   }

   public static String snapshot(FaunaBase body) {
      AttitudeComponent st = AttitudeComponent.of(body);
      if (st == null) {
         return "no attitude has run for this entity (no steps.attitude block, or no root chain)";
      }

      StringBuilder sb = new StringBuilder();
      sb.append(
         String.format(
            Locale.ROOT,
            "gate=%s forced=%s%s | writes: %s | probes fwd=%.2f back=%.2f side=%.2f box reach=%.2f max slope=%.0f° | %s",
            st.gate,
            forced(),
            st.falling
               ? (
                  st.dropping
                     ? " ALOFT, walked off (no rays, height carried with the entity, feet let go)"
                     : " ALOFT (no rays, height carried with the entity)"
               )
               : "",
            st.writes == null ? "?" : st.writes,
            st.forwardArm,
            st.backArm,
            st.sideArm,
            st.reach,
            Math.toDegrees(st.maxSlope),
            st.reused > 0 ? "still: answers reused " + st.reused + " ticks" : "cast this tick"
         )
      );
      sb.append(
         String.format(
            Locale.ROOT,
            "\n  cross pitch=%s (%s) roll=%s (%s) height=%s | gradient (%.3f, %.3f) per block",
            deg(st.cross.pitch()),
            st.cross.along(),
            deg(st.cross.roll()),
            st.cross.across(),
            Double.isNaN(st.cross.height()) ? " - " : String.format(Locale.ROOT, "%.2f", st.cross.height()),
            st.cross.gradX(),
            st.cross.gradZ()
         )
      );
      sb.append(
         String.format(
            Locale.ROOT,
            "\n  lean v=%.3f ω=%s asked=%s | body (eased) pitch=%s roll=%s lean=%s height=%s | jitter pitch %s/tick | hold %s",
            st.speed,
            deg(st.yawRate),
            deg(st.leanRaw),
            deg(st.pitch),
            deg(st.roll),
            deg(st.lean),
            Double.isNaN(st.height) ? " - " : String.format(Locale.ROOT, "%.2f", st.height),
            deg(st.jitterPitch),
            StepsDebug.shift ? "on" : "off"
         )
      );
      sb.append(
         String.format(
            Locale.ROOT,
            "\n  wrote=%s rot=(%s, %s, %s) pos=(%.2f, %.2f, %.2f) px",
            st.wrote,
            deg(st.write.rotX()),
            deg(st.write.rotY()),
            deg(st.write.rotZ()),
            st.write.posX(),
            st.write.posY(),
            st.write.posZ()
         )
      );
      sb.append(String.format(Locale.ROOT, "\n  centre (%.2f, %.2f, %.2f):", st.centreX, st.centreY, st.centreZ));

      for (int i = 0; i < st.probes.length; i++) {
         Attitude.Probe p = st.probes[i];
         if (p == null) {
            sb.append(' ').append(PROBE_NAMES[i]).append("[not asked]");
         } else {
            sb.append(
               String.format(
                  Locale.ROOT,
                  " %s[x%.2f z%.2f %s dig ≤%.2f down %.2f]",
                  PROBE_NAMES[i],
                  p.x(),
                  p.z(),
                  Double.isNaN(st.grounds[i])
                     ? (st.voids[i] ? "VOID" : "WALL, abstains")
                     : String.format(Locale.ROOT, "ground=%.2f (%+.2f)", st.grounds[i], st.grounds[i] - st.centreY),
                  st.ups[i],
                  st.bands[i]
               )
            );
         }
      }

      return sb.toString();
   }

   private static String deg(double rad) {
      return Double.isNaN(rad) ? " - " : String.format(Locale.ROOT, "%+.1f°", Math.toDegrees(rad));
   }

   private static double num(JsonObject cfg, String key, double fallback) {
      return cfg != null && cfg.has(key) && cfg.get(key).isJsonPrimitive() ? cfg.get(key).getAsDouble() : fallback;
   }

   private static double share(JsonObject cfg, String key, boolean fallback) {
      return Attitude.share(cfg == null ? null : cfg.get(key), fallback);
   }

   private static boolean flag(JsonObject cfg, String key, boolean fallback) {
      return cfg != null && cfg.has(key) && cfg.get(key).isJsonPrimitive() ? cfg.get(key).getAsBoolean() : fallback;
   }

   public record Applied(String bone, float restRotX, float restRotY, float restRotZ, float restPosX, float restPosY, float restPosZ) {
   }
}

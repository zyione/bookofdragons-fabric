package net.magister.liberanimalium.render.limbs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.anim.rig.FootStep;
import net.magister.liberanimalium.anim.rig.GaitSpec;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.LimbReachComponent;
import net.magister.liberanimalium.render.AttitudePass;
import net.magister.liberanimalium.render.WatchFiles;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LimbWatch {
   private static final Logger LOGGER = LoggerFactory.getLogger("LiberAnimalium/LimbWatch");
   private static int watched = Integer.MIN_VALUE;
   private static String watchedName = "";
   private static BufferedWriter csv;
   private static Path csvPath;
   private static int csvRows;
   private static int lastConsoleTick = Integer.MIN_VALUE;
   private static final Map<String, LimbWatch.Second> LIMBS = new LinkedHashMap<>();
   static final String HEADER = "tick,limb,regime,why,striding,still,steps_allowed,steps_moving,x,y,z,speed,vy,forward_speed,lateral_speed,yaw,yaw_rate,terrain_pitch_deg,terrain_roll_deg,gait_lift,gait_sway,gait_stride,trail,side,lead,radius,leash,trail_max,lead_max,half,reach,straight,hip_to_target,out,miss,fold_deg,step_lift,step_progress,supported,pair_free,ground_home,ground_eased,ground_lift,reaching,asked,offset_y,corr_x,corr_y,corr_z,pin_x,pin_y,pin_z,home_x,home_y,home_z,target_x,target_y,target_z,target_jump,plant_share,plant_released";
   static final int COLUMNS = "tick,limb,regime,why,striding,still,steps_allowed,steps_moving,x,y,z,speed,vy,forward_speed,lateral_speed,yaw,yaw_rate,terrain_pitch_deg,terrain_roll_deg,gait_lift,gait_sway,gait_stride,trail,side,lead,radius,leash,trail_max,lead_max,half,reach,straight,hip_to_target,out,miss,fold_deg,step_lift,step_progress,supported,pair_free,ground_home,ground_eased,ground_lift,reaching,asked,offset_y,corr_x,corr_y,corr_z,pin_x,pin_y,pin_z,home_x,home_y,home_z,target_x,target_y,target_z,target_jump,plant_share,plant_released"
      .split(",").length;
   private static final LimbWatch.Second BODY = new LimbWatch.Second();

   private LimbWatch() {
   }

   public static Path start(int entityId, String name) {
      stop();
      watched = entityId;
      watchedName = name;
      LIMBS.clear();
      BODY.reset();
      lastConsoleTick = Integer.MIN_VALUE;
      WatchFiles.Opened opened = WatchFiles.open(
         "limbwatch",
         name,
         entityId,
         "tick,limb,regime,why,striding,still,steps_allowed,steps_moving,x,y,z,speed,vy,forward_speed,lateral_speed,yaw,yaw_rate,terrain_pitch_deg,terrain_roll_deg,gait_lift,gait_sway,gait_stride,trail,side,lead,radius,leash,trail_max,lead_max,half,reach,straight,hip_to_target,out,miss,fold_deg,step_lift,step_progress,supported,pair_free,ground_home,ground_eased,ground_lift,reaching,asked,offset_y,corr_x,corr_y,corr_z,pin_x,pin_y,pin_z,home_x,home_y,home_z,target_x,target_y,target_z,target_jump,plant_share,plant_released",
         LOGGER,
         "[LimbWatch]"
      );
      csv = opened == null ? null : opened.writer();
      csvPath = opened == null ? null : opened.path();
      if (opened != null) {
         csvRows = 0;
      }

      return csvPath;
   }

   public static void stop() {
      watched = Integer.MIN_VALUE;
      LIMBS.clear();
      WatchFiles.close(csv);
      csv = null;
   }

   public static boolean watching(int entityId) {
      return watched == entityId;
   }

   public static boolean active() {
      return watched != Integer.MIN_VALUE;
   }

   public static Path file() {
      return csvPath;
   }

   public static int rows() {
      return csvRows;
   }

   static void sample(FaunaBase body, LimbRig rig) {
      int tick = body.tickCount;
      double vx = body.getX() - body.xo;
      double vy = body.getY() - body.yo;
      double vz = body.getZ() - body.zo;
      double speed = Math.hypot(vx, vz);
      double yawRad = Math.toRadians(body.yBodyRot);
      double fx = -Math.sin(yawRad);
      double fz = Math.cos(yawRad);
      double rx = -Math.cos(yawRad);
      double rz = -Math.sin(yawRad);
      double forward = vx * fx + vz * fz;
      double lateral = vx * rx + vz * rz;
      double yawRate = Mth.wrapDegrees(body.yBodyRot - body.yBodyRotO);
      double pitch = Math.toDegrees(AttitudePass.terrainPitchRadians(body));
      double roll = Math.toDegrees(AttitudePass.terrainRollRadians(body));
      boolean allowed = body.animStepsAllowed();
      boolean moving = body.animStepsMoving();
      LimbReachComponent.Gait g = rig.gait;
      BODY.ticks++;
      BODY.speedSum += speed;
      BODY.vyMin = Math.min(BODY.vyMin, vy);
      BODY.vyMax = Math.max(BODY.vyMax, vy);
      BODY.pitchMin = Math.min(BODY.pitchMin, pitch);
      BODY.pitchMax = Math.max(BODY.pitchMax, pitch);
      Object[] bodyRow = new Object[COLUMNS];
      Arrays.fill(bodyRow, "");
      int k = 0;
      bodyRow[k++] = tick;
      bodyRow[k++] = "body";
      k += 4;
      bodyRow[k++] = allowed ? 1 : 0;
      bodyRow[k++] = moving ? 1 : 0;
      bodyRow[k++] = body.getX();
      bodyRow[k++] = body.getY();
      bodyRow[k++] = body.getZ();
      bodyRow[k++] = speed;
      bodyRow[k++] = vy;
      bodyRow[k++] = forward;
      bodyRow[k++] = lateral;
      bodyRow[k++] = (double)body.yBodyRot;
      bodyRow[k++] = yawRate;
      bodyRow[k++] = pitch;
      bodyRow[k++] = roll;
      bodyRow[k++] = g.lift();
      bodyRow[k++] = g.sway();
      bodyRow[k++] = g.stride();
      write(bodyRow);

      for (Limb L : rig.limbs) {
         LimbState st = L.st;
         LimbTrace t = st.trace;
         if (st.footTarget != null && t.tick == tick) {
            FootStep step = st.step;
            String regime = t.skipped != null && t.skipped.startsWith("reach off")
               ? "OFF"
               : (t.pinned ? (step.mode == FootStep.Mode.PACED ? "PACED" : "PINNED") : (!Double.isNaN(t.release) ? "RELEASE" : (t.planted ? "PLANT" : "CLIP")));
            double trail = Double.NaN;
            double side = Double.NaN;
            double lead = Double.NaN;
            if (t.pin != null && t.home != null) {
               double dx = t.pin.x - t.home.x;
               double dz = t.pin.z - t.home.z;
               if (speed > 1.0E-6) {
                  lead = (dx * vx + dz * vz) / speed;
                  trail = -lead;
                  side = Math.abs(dx * vz - dz * vx) / speed;
               } else {
                  trail = Math.hypot(dx, dz);
                  side = 0.0;
                  lead = -trail;
               }
            }

            boolean out = !Double.isNaN(t.straight) && t.hipToTarget > t.straight;
            double miss = t.endSolved != null && t.target != null ? t.endSolved.distanceTo(t.target) : Double.NaN;
            double jump = st.footTargetO != null && st.footTarget != null ? st.footTarget.distanceTo(st.footTargetO) : Double.NaN;
            LimbWatch.Second s = LIMBS.computeIfAbsent(L.def.name(), name -> new LimbWatch.Second());
            s.ticks++;
            if (regime.equals("PINNED")) {
               s.pinned++;
            } else if (regime.equals("PACED")) {
               s.paced++;
            } else if (regime.equals("PLANT")) {
               s.planted++;
            } else {
               s.clip++;
            }

            if (t.why != null && t.why != FootStep.Trigger.NONE) {
               s.lifts++;
            }

            if (out) {
               s.out++;
            }

            if (!Double.isNaN(trail)) {
               s.trailMax = Math.max(s.trailMax, trail);
            }

            if (!Double.isNaN(lead) && regime.equals("PINNED")) {
               s.leadSum += lead;
            }

            if (!Double.isNaN(miss)) {
               s.missMax = Math.max(s.missMax, miss);
            }

            if (!Double.isNaN(t.foldDeg)) {
               s.foldMin = Math.min(s.foldMin, t.foldDeg);
               s.foldMax = Math.max(s.foldMax, t.foldDeg);
            }

            if (!Double.isNaN(jump)) {
               s.targetJumpMax = Math.max(s.targetJumpMax, jump);
            }

            Object[] row = new Object[COLUMNS];
            Arrays.fill(row, "");
            int i = 0;
            row[i++] = tick;
            row[i++] = L.def.name();
            row[i++] = regime;
            row[i++] = t.why == null ? "" : t.why.name();
            row[i++] = t.striding ? 1 : 0;
            row[i++] = t.still ? 1 : 0;
            row[i++] = allowed ? 1 : 0;
            row[i++] = moving ? 1 : 0;
            i = 22;
            row[i++] = trail;
            row[i++] = side;
            row[i++] = lead;
            row[i++] = t.triggerRadius;
            row[i++] = t.pinned ? leash(rig, L, t) : Double.NaN;
            row[i++] = t.trailMax;
            row[i++] = t.leadMax;
            row[i++] = t.half;
            row[i++] = t.reach;
            row[i++] = t.straight;
            row[i++] = t.hipToTarget;
            row[i++] = out ? 1 : 0;
            row[i++] = miss;
            row[i++] = t.foldDeg;
            row[i++] = step.lift;
            row[i++] = step.progress;
            row[i++] = t.supported ? 1 : 0;
            row[i++] = t.pairFree ? 1 : 0;
            row[i++] = t.groundHome;
            row[i++] = t.groundEased;
            row[i++] = t.lift;
            row[i++] = t.reaching ? 1 : 0;
            row[i++] = t.asked;
            row[i++] = t.offset == null ? Double.NaN : t.offset.y;
            row[i++] = st.correction.x;
            row[i++] = st.correction.y;
            row[i++] = st.correction.z;
            row[i++] = c(t.pin, 0);
            row[i++] = c(t.pin, 1);
            row[i++] = c(t.pin, 2);
            row[i++] = c(t.home, 0);
            row[i++] = c(t.home, 1);
            row[i++] = c(t.home, 2);
            row[i++] = c(st.footTarget, 0);
            row[i++] = c(st.footTarget, 1);
            row[i++] = c(st.footTarget, 2);
            row[i++] = jump;
            row[i++] = t.plantShare;
            row[i++] = t.plantReleased.name();
            write(row);
         }
      }

      if (tick - lastConsoleTick >= 20) {
         lastConsoleTick = tick;
         LOGGER.debug(
            "[LimbWatch] {} t={} body last {} ticks: speed {} vy {}..{} terrain pitch {}..{} steps allowed={} moving={}",
            new Object[]{
               watchedName,
               tick,
               BODY.ticks,
               f(BODY.ticks == 0 ? 0.0 : BODY.speedSum / BODY.ticks),
               f(BODY.vyMin),
               f(BODY.vyMax),
               f(BODY.pitchMin),
               f(BODY.pitchMax),
               allowed,
               moving
            }
         );

         for (Entry<String, LimbWatch.Second> e : LIMBS.entrySet()) {
            LimbWatch.Second s = e.getValue();
            LOGGER.debug(
               "[LimbWatch] {} {}: pinned {} paced {} planted {} clip {} of {} ticks, {} lifts, trail max {}, lead mean {}, out of reach {} ticks, miss max {}, fold {}..{} deg, target jump max {}",
               new Object[]{
                  watchedName,
                  e.getKey(),
                  s.pinned,
                  s.paced,
                  s.planted,
                  s.clip,
                  s.ticks,
                  s.lifts,
                  f(s.trailMax),
                  f(s.pinned == 0 ? 0.0 : s.leadSum / s.pinned),
                  s.out,
                  f(s.missMax),
                  s.foldMin == Double.POSITIVE_INFINITY ? " - " : f(s.foldMin),
                  s.foldMax == Double.NEGATIVE_INFINITY ? " - " : f(s.foldMax),
                  f(s.targetJumpMax)
               }
            );
            s.reset();
         }

         BODY.reset();
      }
   }

   private static double leash(LimbRig rig, Limb L, LimbTrace t) {
      GaitSpec g = rig.spec.gait;
      return t.striding && !Double.isNaN(t.trailMax)
         ? FootStep.strideLeash(g, L.def.zone(), t.triggerRadius, t.half, g.strideShare() * t.trailMax)
         : FootStep.leash(g, L.def.zone());
   }

   private static void write(Object[] cells) {
      if (csv != null) {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
               sb.append(',');
            }

            Object c = cells[i];
            if (c instanceof Double d) {
               sb.append(n(d));
            } else if (c instanceof Float f) {
               sb.append(n(f.floatValue()));
            } else {
               sb.append(c);
            }
         }

         sb.append('\n');

         try {
            csv.write(sb.toString());
            csvRows++;
         } catch (IOException e) {
            LOGGER.warn("[LimbWatch] write failed; the watch stopped", e);
            stop();
         }
      }
   }

   private static String n(double v) {
      return Double.isNaN(v) ? "" : String.format(Locale.ROOT, "%.3f", v);
   }

   private static String c(Vec3 v, int i) {
      return v == null ? "" : String.format(Locale.ROOT, "%.3f", i == 0 ? v.x : (i == 1 ? v.y : v.z));
   }

   private static String f(double v) {
      return String.format(Locale.ROOT, "%.3f", v);
   }

   private static final class Second {
      int ticks;
      int pinned;
      int paced;
      int clip;
      int planted;
      int lifts;
      int out;
      double trailMax;
      double leadSum;
      double missMax;
      double foldMin = Double.POSITIVE_INFINITY;
      double foldMax = Double.NEGATIVE_INFINITY;
      double speedSum;
      double vyMin = Double.POSITIVE_INFINITY;
      double vyMax = Double.NEGATIVE_INFINITY;
      double pitchMin = Double.POSITIVE_INFINITY;
      double pitchMax = Double.NEGATIVE_INFINITY;
      double targetJumpMax;

      void reset() {
         this.ticks = this.pinned = this.paced = this.clip = this.planted = this.lifts = this.out = 0;
         this.trailMax = this.leadSum = this.missMax = this.speedSum = this.targetJumpMax = 0.0;
         this.foldMin = this.vyMin = this.pitchMin = Double.POSITIVE_INFINITY;
         this.foldMax = this.vyMax = this.pitchMax = Double.NEGATIVE_INFINITY;
      }
   }
}

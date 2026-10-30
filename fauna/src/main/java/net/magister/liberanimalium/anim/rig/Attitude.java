package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Attitude {
   public static final double VOID = Double.NEGATIVE_INFINITY;
   public static final double FALL_FREE = 1.25;

   private Attitude() {
   }

   public static double share(JsonElement authored, boolean fallback) {
      if (authored != null && authored.isJsonPrimitive()) {
         JsonPrimitive value = authored.getAsJsonPrimitive();
         if (value.isBoolean()) {
            return value.getAsBoolean() ? 1.0 : 0.0;
         }

         if (value.isNumber()) {
            return Math.max(0.0, Math.min(1.0, value.getAsDouble()));
         }
      }

      return fallback ? 1.0 : 0.0;
   }

   public static Attitude.Probe at(double centreX, double centreZ, double yawDeg, double forward, double side) {
      double y = Math.toRadians(yawDeg);
      double fx = -Math.sin(y);
      double fz = Math.cos(y);
      double rx = -Math.cos(y);
      double rz = -Math.sin(y);
      return new Attitude.Probe(centreX + forward * fx + side * rx, centreZ + forward * fz + side * rz);
   }

   public static Attitude.Cross cross(
      double front, double back, double left, double right, double forwardArm, double backArm, double sideArm, double yawDeg, double maxSlopeRad
   ) {
      return cross(front, back, left, right, Double.NaN, Double.NaN, forwardArm, backArm, sideArm, yawDeg, maxSlopeRad, 0.0, null);
   }

   public static Attitude.Cross cross(
      double front,
      double back,
      double left,
      double right,
      double centre,
      double floor,
      double forwardArm,
      double backArm,
      double sideArm,
      double yawDeg,
      double maxSlopeRad,
      double reach,
      Attitude.Edges edges
   ) {
      boolean hangs = !Double.isNaN(floor)
         && !Double.isNaN(centre)
         && (centre == Double.NEGATIVE_INFINITY || floor - centre > reach * Math.tan(maxSlopeRad) + 1.1);
      Attitude.Axis along = axis(front, back, centre, forwardArm, backArm, floor, hangs, maxSlopeRad, reach, edges, true);
      Attitude.Axis across = axis(right, left, centre, sideArm, sideArm, floor, hangs, maxSlopeRad, reach, edges, false);
      boolean alongOk = !Double.isNaN(along.grad());
      boolean acrossOk = !Double.isNaN(across.grad());
      double alongGrad = alongOk ? along.grad() : 0.0;
      double acrossGrad = acrossOk ? across.grad() : 0.0;
      double pitch = alongOk ? clamp(Math.atan(-alongGrad), maxSlopeRad) : Double.NaN;
      double roll = acrossOk ? clamp(Math.atan(acrossGrad), maxSlopeRad) : Double.NaN;
      double alongHeight = along.height();
      double acrossHeight = across.height();
      boolean alongHung = along.rule() == Attitude.Rule.SPAN || along.rule() == Attitude.Rule.TIP;
      boolean acrossHung = across.rule() == Attitude.Rule.SPAN || across.rule() == Attitude.Rule.TIP;
      if (alongHung && !acrossHung) {
         acrossHeight = Double.NaN;
      }

      if (acrossHung && !alongHung) {
         alongHeight = Double.NaN;
      }

      double height = !Double.isNaN(alongHeight) && !Double.isNaN(acrossHeight)
         ? 0.5 * (alongHeight + acrossHeight)
         : (!Double.isNaN(alongHeight) ? alongHeight : acrossHeight);
      double y = Math.toRadians(yawDeg);
      double fx = -Math.sin(y);
      double fz = Math.cos(y);
      double rx = -Math.cos(y);
      double rz = -Math.sin(y);
      return new Attitude.Cross(pitch, roll, height, alongGrad * fx + acrossGrad * rx, alongGrad * fz + acrossGrad * rz, along.rule(), across.rule());
   }

   private static Attitude.Axis axis(
      double plus,
      double minus,
      double centre,
      double plusArm,
      double minusArm,
      double floor,
      boolean hangs,
      double maxSlopeRad,
      double reach,
      Attitude.Edges edges,
      boolean along
   ) {
      if (!Double.isNaN(plus) && !Double.isNaN(minus) && !(plusArm + minusArm <= 1.0E-6)) {
         boolean plusGround = plus != Double.NEGATIVE_INFINITY;
         boolean minusGround = minus != Double.NEGATIVE_INFINITY;
         boolean hasCentre = !Double.isNaN(centre);
         boolean centreGround = hasCentre && centre != Double.NEGATIVE_INFINITY;
         if (plusGround && minusGround) {
            double grad = (plus - minus) / (plusArm + minusArm);
            double line = plus - grad * plusArm;
            if (!hasCentre) {
               return new Attitude.Axis(grad, line, Attitude.Rule.LINE);
            }

            if (centreGround && centre - line > 1.1) {
               double toPlus = (plus - centre) / plusArm;
               double toMinus = (centre - minus) / minusArm;
               return new Attitude.Axis(Math.abs(toPlus) <= Math.abs(toMinus) ? toPlus : toMinus, centre, Attitude.Rule.CREST);
            }

            if (hangs) {
               boolean plusHigh = plus >= minus;
               Attitude.Axis hung = hang(
                  plusHigh ? plusArm : -minusArm, plusHigh ? -minusArm : plusArm, plusHigh ? minus : plus, floor, maxSlopeRad, reach, edges, along
               );
               if (hung != null) {
                  return hung;
               }

               if (edges != null) {
                  return new Attitude.Axis(grad, Double.NaN, Attitude.Rule.LINE);
               }
            }

            return new Attitude.Axis(grad, line, Attitude.Rule.LINE);
         } else {
            if (!plusGround && !minusGround) {
               return centreGround && !hangs ? new Attitude.Axis(Double.NaN, centre, Attitude.Rule.CREST) : Attitude.Axis.NONE;
            }

            double at = plusGround ? plusArm : -minusArm;
            double ground = plusGround ? plus : minus;
            if (centreGround && !hangs) {
               return new Attitude.Axis((ground - centre) / at, centre, Attitude.Rule.CREST);
            }

            if (hangs) {
               Attitude.Axis hung = hang(at, plusGround ? -minusArm : plusArm, Double.NEGATIVE_INFINITY, floor, maxSlopeRad, reach, edges, along);
               if (hung != null) {
                  return hung;
               }
            }

            return Attitude.Axis.NONE;
         }
      } else {
         return Attitude.Axis.NONE;
      }
   }

   private static Attitude.Axis hang(
      double holdAt, double farAt, double far, double floor, double maxSlopeRad, double reach, Attitude.Edges edges, boolean along
   ) {
      if (edges == null) {
         return null;
      }

      double edge = edges.find(along, holdAt, floor);
      if (!Double.isNaN(edge) && !(Math.abs(farAt - edge) < 1.0E-6)) {
         double grad;
         Attitude.Rule rule;
         if (far != Double.NEGATIVE_INFINITY) {
            grad = clamp((far - floor) / (farAt - edge), Math.tan(maxSlopeRad));
            rule = Attitude.Rule.SPAN;
         } else {
            double share = reach > 1.0E-6 ? Math.min(1.0, Math.abs(edge) / reach) : 1.0;
            grad = -Math.signum(farAt) * Math.tan(maxSlopeRad * share);
            rule = Attitude.Rule.TIP;
         }

         return new Attitude.Axis(grad, floor - grad * edge, rule);
      } else {
         return null;
      }
   }

   public static double boxReach(double halfWidth, double yawDeg) {
      double y = Math.toRadians(yawDeg);
      return halfWidth / Math.max(Math.abs(Math.sin(y)), Math.abs(Math.cos(y)));
   }

   public static boolean aloft(boolean wasAloft, boolean onGround, double dy, boolean held, boolean hopping) {
      return !onGround && (wasAloft || hopping || dy > 0.0 || !held);
   }

   public static double fallPitch(double dy, double horiz, double maxSlopeRad) {
      return dy >= 0.0 ? 0.0 : clamp(Math.atan2(-dy, horiz), maxSlopeRad);
   }

   public static double fallShare(double fallen, double whole) {
      if (whole <= 1.25) {
         return fallen >= whole ? 1.0 : 0.0;
      }

      double t = Math.max(0.0, Math.min(1.0, (fallen - 1.25) / (whole - 1.25)));
      return t * t * (3.0 - 2.0 * t);
   }

   public static double pathPitch(double dy, double horiz) {
      return dy >= 0.0 ? 0.0 : Math.atan2(-dy, horiz);
   }

   private static double clamp(double v, double limit) {
      return v > limit ? limit : Math.max(v, -limit);
   }

   public static double startLift(double forward, double side, double pitchRad, double rollRad) {
      return -forward * Math.tan(pitchRad) + side * Math.tan(rollRad);
   }

   public static double band(double arm, double maxSlopeRad) {
      return Math.abs(arm) * Math.tan(maxSlopeRad) + 1.0;
   }

   public static Quaternionf modelRotation(Matrix4f modelToWorld, double yawDeg, double pitch, double roll) {
      Matrix3f m = modelToWorld.get3x3(new Matrix3f());
      Matrix3f local = new Matrix3f(m).invert().mul(worldRotation(yawDeg, pitch, roll)).mul(m);
      return local.getNormalizedRotation(new Quaternionf());
   }

   public static double lean(double speed, double yawRate, double gravity) {
      return -Math.atan(speed * yawRate / gravity);
   }

   public static Matrix3f worldRotation(double yawDeg, double pitch, double roll) {
      double y = Math.toRadians(yawDeg);
      Vector3f fwd = new Vector3f((float)(-Math.sin(y)), 0.0F, (float)Math.cos(y));
      Vector3f right = new Vector3f((float)(-Math.cos(y)), 0.0F, (float)(-Math.sin(y)));
      Matrix3f r = new Matrix3f().rotation((float)(-pitch), right);
      r.rotate((float)(-roll), fwd);
      return r;
   }

   public static float sampleTick(boolean blended, float partialTick) {
      return blended ? 1.0F : partialTick;
   }

   public static double shift(double heightO, double height, double yO, double y, float pt) {
      double h = Double.isNaN(heightO) ? height : heightO + pt * (height - heightO);
      return h - (yO + pt * (y - yO));
   }

   public static Attitude.Write write(
      Matrix4f modelToWorld, double yawDeg, double pitch, double roll, double lean, double holdX, double holdY, double holdZ, double shift
   ) {
      Matrix3f m = modelToWorld.get3x3(new Matrix3f());
      Matrix3f mInv = new Matrix3f(m).invert();
      Matrix3f q = worldRotation(yawDeg, pitch, roll + lean);
      Matrix3f local = new Matrix3f(mInv).mul(q).mul(m);
      Quaternionf quat = local.getNormalizedRotation(new Quaternionf());
      float[] euler = BoneFk.eulerZYX(quat);
      Vector3f d = new Vector3f((float)holdX, (float)holdY, (float)holdZ);
      Vector3f t = new Vector3f(d).sub(q.transform(new Vector3f(d)));
      t.y += (float)shift;
      Vector3f model = mInv.transform(t);
      return new Attitude.Write(euler[0], euler[1], euler[2], -model.x * 16.0, model.y * 16.0, model.z * 16.0);
   }

   private record Axis(double grad, double height, Attitude.Rule rule) {
      static final Attitude.Axis NONE = new Attitude.Axis(Double.NaN, Double.NaN, Attitude.Rule.NONE);
   }

   public record Cross(double pitch, double roll, double height, double gradX, double gradZ, Attitude.Rule along, Attitude.Rule across) {
      public static final Attitude.Cross NONE = new Attitude.Cross(Double.NaN, Double.NaN, Double.NaN, 0.0, 0.0, Attitude.Rule.NONE, Attitude.Rule.NONE);
   }

   @FunctionalInterface
   public interface Edges {
      double find(boolean var1, double var2, double var4);
   }

   public record Probe(double x, double z) {
   }

   public enum Rule {
      NONE,
      LINE,
      CREST,
      SPAN,
      TIP;
   }

   public record Write(double rotX, double rotY, double rotZ, double posX, double posY, double posZ) {
      public static final Attitude.Write NONE = new Attitude.Write(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   }
}

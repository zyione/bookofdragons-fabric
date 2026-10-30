package net.magister.liberanimalium.render.limbs;

import java.util.Locale;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.FootStep;
import net.magister.liberanimalium.anim.rig.GaitSpec;
import net.magister.liberanimalium.anim.rig.LimbDef;
import net.magister.liberanimalium.chassis.component.LimbReachComponent;
import net.magister.liberanimalium.debug.client.LegReachDebugRenderer;
import net.magister.liberanimalium.render.BonePalettes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class LimbReadout {
   private LimbReadout() {
   }

   static String snapshot(LimbRig rig) {
      StringBuilder sb = new StringBuilder();
      boolean frame = false;

      for (Limb L : rig.limbs) {
         LimbDef limb = L.def;
         LimbState st = L.st;
         if (st.footTarget != null) {
            LimbTrace t = st.trace;
            if (!frame && t.drawn != null) {
               frame = true;
               sb.append(
                  String.format(
                     Locale.ROOT,
                     "frame: tick=%d pt=%.2f drawn=%s entity=%s yaw=%.1f | model origin in world=%s (floor at drawn.y) | model up in world=%s",
                     t.tick,
                     t.partial,
                     fmt(t.drawn),
                     fmt(t.entity),
                     t.yaw,
                     fmt(t.originWorld),
                     fmt(t.upWorld)
                  )
               );
            }

            sb.append(
               String.format(
                  Locale.ROOT,
                  "\n%s (%s)%s reach %s zone=%.2f%s",
                  limb.name(),
                  limb.foot(),
                  limb.splay() ? " [splay]" : " [follows body]",
                  limb.reach() ? String.format(Locale.ROOT, "up=%.1f down=%.1f", limb.reachUp(), limb.reachDown()) : "OFF (pins and steps only)",
                  limb.zone(),
                  t.skipped == null ? "" : " - " + t.skipped
               )
            );
            sb.append(
               String.format(
                  Locale.ROOT, "\n  clip foot %s  above floor=%+.3f  | body foot %s | home %s", fmt(t.clipFoot), t.clipAbove, fmt(t.bodyFoot), fmt(t.home)
               )
            );
            sb.append(
               String.format(
                  Locale.ROOT,
                  "\n  ground: under home=%s %s  eased=%s",
                  h(t.groundHome),
                  Double.isNaN(t.groundHome) ? "(none in the band: the clip's foot)" : "(reachable)",
                  h(t.groundEased)
               )
            );
            sb.append(
               String.format(
                  Locale.ROOT,
                  "\n  probe: start y=%.3f (tilted %+.3f by pitch %+.1f° roll %+.1f°) %s | dig up to y=%.3f (max %.1f over the floor, roof over the body %s) | drop to y=%.3f",
                  t.rayFrom,
                  t.tilt,
                  Math.toDegrees(t.tiltPitch),
                  Math.toDegrees(t.tiltRoll),
                  t.buried ? "BURIED - dug up" : "in air - dropped",
                  t.rayFrom + t.upMax,
                  limb.reachUp(),
                  Double.isInfinite(t.ceiling) ? "none" : String.format(Locale.ROOT, "at y=%.3f", t.ceiling),
                  t.rayFrom - t.rayDown
               )
            );
            Locale var10001 = Locale.ROOT;
            Object[] var10003 = new Object[2];
            String var10006;
            if (t.mode == null && t.planted) {
               var10006 = String.format(
                  Locale.ROOT, "PLANTED at %s |pin-clip|=%.3f, the clip carries it back at %.2f of the body's speed", fmt(t.pin), t.homeDist, t.plantShare
               );
            } else if (t.mode == null) {
               var10006 = String.format(
                     Locale.ROOT,
                     "CLIP (its clip height over the eased ground; back through the body at %s of its speed, last plant let go by %s)",
                     Double.isNaN(t.plantShare) ? " - " : String.format(Locale.ROOT, "%.2f", t.plantShare),
                     t.plantReleased
                  )
                  + (Double.isNaN(t.release) ? "" : String.format(Locale.ROOT, " releasing a pin k=%.2f", t.release));
            } else {
               switch (t.mode) {
                  case PINNED:
                     var10006 = String.format(
                        Locale.ROOT,
                        "PINNED at %s |pin-home|=%.3f trigger=%s adjacent free=%b supported=%b why=%s",
                        fmt(t.pin),
                        t.homeDist,
                        Double.isNaN(t.triggerRadius) ? " - " : String.format(Locale.ROOT, "%.2f", t.triggerRadius),
                        t.pairFree,
                        t.supported,
                        t.why
                     );
                     break;
                  case PACED:
                     var10006 = String.format(Locale.ROOT, "PACED STEP s=%.2f -> %s", t.stepProgress, fmt(t.stepTarget));
                     break;
                  default:
                     throw new IncompatibleClassChangeError();
               }
            }

            var10003[0] = var10006;
            var10003[1] = t.steps ? "allowed by the held state" : "not allowed (no held state declares steps)";
            sb.append(String.format(var10001, "\n  foot: %s | steps %s", var10003));
            sb.append(
               String.format(Locale.ROOT, "\n  correction (tick) %s -> tick target %s -> frame target %s", fmt(st.correction), fmt(t.target0), fmt(t.target))
            );
            if (!Double.isNaN(t.half)) {
               GaitSpec g = rig.spec.gait;
               sb.append(
                  String.format(
                     Locale.ROOT,
                     "\n  stride: %s | reach circle %.2f about the hip (leg straight %.2f) | from the home the circle allows %.2f of trail and %.2f of lead | lead %s of half %.2f (floor %.2f .. cap %.2f of the reach; the speed over %d ticks between) | repositions past %.2f behind (the zone, capped by the trail), leash %.2f",
                     t.striding ? "STRIDING" : "standing steps",
                     t.reach,
                     t.straight,
                     t.trailMax,
                     t.leadMax,
                     Double.isNaN(t.aheadOf) ? " - " : String.format(Locale.ROOT, "%.2f", t.aheadOf),
                     t.half,
                     g.strideFloor() * t.reach,
                     g.strideShare() * t.reach,
                     g.strideTicks(),
                     t.triggerRadius,
                     FootStep.leash(g, limb.zone())
                  )
               );
            }

            sb.append(
               String.format(
                  Locale.ROOT,
                  "\n  leg: hip %s knee(anim) %s foot(anim) %s l1=%.3f l2=%.3f reach=%.3f |hip->target|=%.3f%s",
                  fmt(t.hip),
                  fmt(t.kneeAnim),
                  fmt(t.foot0),
                  t.l1,
                  t.l2,
                  t.l1 + t.l2,
                  t.hipToTarget,
                  t.hipToTarget > (Double.isNaN(t.straight) ? t.l1 + t.l2 : t.straight) ? " OUT OF REACH (clamped)" : ""
               )
            );
            if (t.endSolved != null) {
               sb.append(String.format(Locale.ROOT, "\n  solve: knee %s end %s", fmt(t.kneeSolved), fmt(t.endSolved)));
            }

            if (!Double.isNaN(t.foldDeg) && limb.knee() == null) {
               sb.append(
                  String.format(
                     Locale.ROOT,
                     "%n  hinge: %s folded %+.1f deg (+ closes) about %s",
                     limb.mid(),
                     t.foldDeg,
                     t.hingeKnown ? "the clip's own bend, remembered" : "its X axis (no clip has bent this knee yet)"
                  )
               );
            } else if (!Double.isNaN(t.foldDeg)) {
               sb.append(String.format(Locale.ROOT, "%n  z-leg: knee %s folded %+.1f deg, ankle %s the other way", limb.knee(), t.foldDeg, limb.mid()));
            }

            sb.append(
               String.format(
                  Locale.ROOT,
                  "\n  wrote: %s rot %s->%s | %s rot %s->%s | %s sole locked to the clip",
                  limb.root(),
                  deg(t.rootBefore),
                  deg(t.rootAfter),
                  limb.mid(),
                  deg(t.midBefore),
                  deg(t.midAfter),
                  limb.foot()
               )
            );
            if (t.rendered != null) {
               sb.append(
                  String.format(
                     Locale.ROOT,
                     "\n  drawn foot (last frame) %s  %s  ground under it=%s",
                     fmt(t.rendered),
                     t.renderedInside ? "INSIDE A BLOCK" : "free",
                     Double.isNaN(t.groundUnderRendered) ? "none within 8" : String.format(Locale.ROOT, "%.3f", t.groundUnderRendered)
                  )
               );
            }
         }
      }

      LimbReachComponent.Gait g = rig.gait;
      sb.append(
         String.format(
            Locale.ROOT,
            "\ngait: lift=%.2f sway=%+.2f stride=%+.2f | front lift=%.2f sway=%+.2f stride=%+.2f | rear lift=%.2f sway=%+.2f stride=%+.2f",
            g.lift(),
            g.sway(),
            g.stride(),
            g.frontLift(),
            g.frontSway(),
            g.frontStride(),
            g.rearLift(),
            g.rearSway(),
            g.rearStride()
         )
      );
      return sb.toString();
   }

   private static String h(double y) {
      return Double.isNaN(y) ? " - " : String.format(Locale.ROOT, "%.3f", y);
   }

   private static String deg(float[] r) {
      return r == null ? " - " : String.format(Locale.ROOT, "(%.1f,%.1f,%.1f)°", Math.toDegrees(r[0]), Math.toDegrees(r[1]), Math.toDegrees(r[2]));
   }

   static void drawFoot(int id, Vec3 home, LimbState st) {
      if (st.footTarget != null) {
         LegReachDebugRenderer.cross(id, home.x, home.y, home.z, 0.1F, 0.5F, 0.5F, 0.5F);
         Vec3 pivot = st.footTarget;
         LegReachDebugRenderer.cross(id, pivot.x, pivot.y, pivot.z, 0.15F, 0.2F, 1.0F, 0.2F);
         if (!Double.isNaN(st.groundY)) {
            LegReachDebugRenderer.cross(id, pivot.x, st.groundY, pivot.z, 0.1F, 0.3F, 0.3F, 1.0F);
         }
      }
   }

   static void collectDebugLines(int id, Vec3 body, AzBone root, AzBone mid, AzBone eff) {
      Vec3 h = BonePalettes.worldPosition(id, root.getName(), body);
      Vec3 k = BonePalettes.worldPosition(id, mid.getName(), body);
      Vec3 f = BonePalettes.worldPosition(id, eff.getName(), body);
      if (h != null && k != null && f != null) {
         LegReachDebugRenderer.line(id, h.x, h.y, h.z, k.x, k.y, k.z, 1.0F, 1.0F, 0.0F);
         LegReachDebugRenderer.line(id, k.x, k.y, k.z, f.x, f.y, f.z, 1.0F, 0.5F, 0.0F);
         addDescendants(id, body, mid);
         Matrix4f w = BonePalettes.worldMatrix(id, eff.getName());
         if (w != null) {
            axis(id, f, w, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F);
            axis(id, f, w, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F);
            axis(id, f, w, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);
         }
      }
   }

   private static void addDescendants(int id, Vec3 body, AzBone bone) {
      Vec3 p = BonePalettes.worldPosition(id, bone.getName(), body);
      if (p != null) {
         for (AzBone c : bone.getChildBones()) {
            Vec3 cp = BonePalettes.worldPosition(id, c.getName(), body);
            if (cp != null) {
               boolean membrane = c.getName().toLowerCase(Locale.ROOT).contains("membrane");
               if (membrane) {
                  LegReachDebugRenderer.line(id, p.x, p.y, p.z, cp.x, cp.y, cp.z, 1.0F, 0.0F, 1.0F);
               } else {
                  LegReachDebugRenderer.line(id, p.x, p.y, p.z, cp.x, cp.y, cp.z, 0.0F, 1.0F, 1.0F);
                  addDescendants(id, body, c);
               }
            }
         }
      }
   }

   private static void axis(int id, Vec3 p, Matrix4f w, float ax, float ay, float az, float r, float g, float b) {
      Vector3f dir = w.transformDirection(new Vector3f(ax, ay, az));
      if (!(dir.lengthSquared() < 1.0E-9F)) {
         dir.normalize().mul(0.35F);
         LegReachDebugRenderer.line(id, p.x, p.y, p.z, p.x + dir.x(), p.y + dir.y(), p.z + dir.z(), r, g, b);
      }
   }

   private static String fmt(Vec3 v) {
      return v == null ? " - " : String.format(Locale.ROOT, "(%.3f, %.3f, %.3f)", v.x, v.y, v.z);
   }
}

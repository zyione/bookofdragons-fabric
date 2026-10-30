package net.magister.liberanimalium.render.limbs;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.BoneFk;
import net.magister.liberanimalium.anim.rig.ZLeg;
import net.magister.liberanimalium.body.grip.GripGeometry;
import net.magister.liberanimalium.body.grip.GripRules;
import net.magister.liberanimalium.body.grip.GripSpec;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.GripComponent;
import net.magister.liberanimalium.debug.client.GripWatch;
import net.magister.liberanimalium.render.AttitudePass;
import net.magister.liberanimalium.render.CubeMesh;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

public final class GripSolver {
   private static final Logger LOG = LogUtils.getLogger();
   private static final float[] ALONG = new float[]{0.4F, 0.7F, 1.0F};

   private GripSolver() {
   }

   public static List<String> solve(
      FaunaBase body,
      LimbRig rig,
      AzBakedModel model,
      GripSpec spec,
      Supplier<GripGeometry.Box> boxOf,
      float weight,
      boolean close,
      Vec3 drawn,
      Matrix4f toWorld,
      Matrix4f toModel,
      float partial
   ) {
      Set<String> touched = new LinkedHashSet<>();
      AttitudePass.Applied attitude = AttitudePass.applied(body);
      Map<String, float[]> bodyWrites = AttitudePass.bodyWrites(body);
      double size = GripRules.size(body);
      PhysicsSheet.Grip physics = PhysicsSheet.of(body).grip();
      double bite = physics.bite() / 16.0 * size;
      Vector3f point = new Vector3f();
      Vector3f inward = new Vector3f();
      Quaternionf drawnTurn = toWorld.getNormalizedRotation(new Quaternionf());
      List<GripSpec.Gripper> found = new ArrayList<>(spec.grippers().size());
      Vec3 sum = Vec3.ZERO;
      Quaternionf torso = null;
      Iterator grip = spec.grippers().iterator();

      while (true) {
         GripSpec.Gripper gripper;
         while (true) {
            if (!grip.hasNext()) {
               if (found.isEmpty()) {
                  return List.of();
               }

               GripComponent gripx = body.grip();
               Vec3 bodyFrame = GripGeometry.bodyPoint(drawn, body.getBbHeight() * 0.5, drawnTurn, sum.scale(1.0 / found.size()));
               gripx.noteDrawnHips(body.tickCount + partial, bodyFrame.subtract(spec.middle().scale(size)), drawn, drawnTurn, torso);
               GripGeometry.Box box = boxOf.get();
               if (box == null) {
                  return List.of();
               }

               boolean watch = GripWatch.watching && firstThisTick(body, rig);
               if (watch) {
                  watchBody(body, drawnTurn, box, weight, close, drawn);
               }

               Quaternionf boxTurn = toModel.getNormalizedRotation(new Quaternionf()).mul(box.rotation());
               Quaternionf stood = gripx.stoodTorso(partial);
               Quaternionf calm = stood == null ? new Quaternionf() : stood.mul(new Quaternionf(torso).conjugate());

               for (GripSpec.Gripper gripperx : found) {
                  if (gripperx.closes()) {
                     Limb L = rig.named(gripperx.name());
                     Vec3 hip = FaunaLegReachSolver.toWorld(toWorld, drawn, L.rHip);
                     float side = gripperx.at().x > 0.001 ? 1.0F : (gripperx.at().x < -0.001 ? -1.0F : 0.0F);
                     double forward = (gripperx.at().z - spec.middle().z) * size;
                     box.grip(side, forward, gripx.bandMid(), bite, point, inward);
                     Vector3f full = FaunaLegReachSolver.toModelPoint(toModel, drawn, new Vec3(point.x, point.y, point.z), new Vector3f());
                     Vector3f target = L.target.set(L.eFoot).lerp(full, weight);
                     Quaternionf effWorld = BoneFk.orthonormalRotation(L.effParent, new Quaternionf()).mul(BoneFk.localQuat(L.eff, new Quaternionf()));
                     Quaternionf effCalm = new Quaternionf(calm).mul(effWorld);
                     Vector3f face = effCalm.transform(new Vector3f(0.0F, -1.0F, 0.0F));
                     Vector3f normal = side != 0.0F ? new Vector3f(-side, 0.0F, 0.0F) : new Vector3f(0.0F, -1.0F, 0.0F);
                     Quaternionf laid = new Quaternionf(boxTurn).mul(new Quaternionf().rotationTo(face, normal)).mul(effCalm);
                     Quaternionf effTarget = new Quaternionf(effWorld).slerp(laid, weight);
                     Vector3f into = toModel.transformDirection(new Vector3f(inward)).normalize();
                     FaunaLegReachSolver.aim(rig, L, target, effTarget, FaunaLegReachSolver.UNARMED);
                     touched.add(L.root.getName());
                     touched.add(L.mid.getName());
                     touched.add(L.eff.getName());
                     if (L.knee != null) {
                        touched.add(L.knee.getName());
                     }

                     for (AzBone anchor : L.anchors) {
                        touched.add(anchor.getName());
                     }

                     if (close) {
                        closeDigits(L, box, into, physics.curlMax(), weight, drawn, toWorld, touched);
                     }

                     if (watch) {
                        watchLimb(gripperx.name(), L, hip, point, drawnTurn, drawn, toWorld);
                     }
                  }
               }

               return List.copyOf(touched);
            }

            gripper = (GripSpec.Gripper)grip.next();
            if (gripper.closes()) {
               Limb L = rig.named(gripper.name());
               if (L != null && L.root != null && L.mid != null && L.eff != null) {
                  FaunaLegReachSolver.walk(rig, L, attitude, bodyWrites);
                  if (L.iRoot >= 0 && L.iMid >= 0 && L.iEff >= 0) {
                     sum = sum.add(FaunaLegReachSolver.toWorld(toWorld, drawn, L.rHip));
                     if (torso == null) {
                        torso = BoneFk.orthonormalRotation(L.rootParent, new Quaternionf());
                     }
                     break;
                  }
               }
            } else {
               AzBone bone = model.getBoneOrNull(gripper.bone());
               if (bone != null) {
                  Matrix4f accum = new Matrix4f();

                  for (AzBone b : BoneFk.ancestorsTopDown(bone, model)) {
                     BoneFk.mulLocal(accum, b);
                  }

                  Vector3f end = CubeMesh.farEnd(bone);
                  if (end == null) {
                     end = new Vector3f(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
                  }

                  sum = sum.add(FaunaLegReachSolver.toWorld(toWorld, drawn, accum.transformPosition(end)));
                  if (torso == null) {
                     torso = BoneFk.orthonormalRotation(accum, new Quaternionf());
                  }
                  break;
               }
            }
         }

         found.add(gripper);
      }
   }

   private static boolean firstThisTick(FaunaBase body, LimbRig rig) {
      if (rig.gripWatchTick == body.tickCount) {
         return false;
      }

      rig.gripWatchTick = body.tickCount;
      return true;
   }

   private static void watchBody(FaunaBase body, Quaternionf turn, GripGeometry.Box box, float weight, boolean close, Vec3 drawn) {
      Vector3f nose = turn.transform(new Vector3f(0.0F, 0.0F, -1.0F));
      Vector3f back = turn.transform(new Vector3f(0.0F, 1.0F, 0.0F));
      Vector3f boxUp = box.rotation().transform(new Vector3f(0.0F, 1.0F, 0.0F));
      LOG.debug(
         String.format(
            Locale.ROOT,
            "[GripWatch] t=%d at=(%.2f,%.2f,%.2f) yaw=%.1f nose=(%.2f,%.2f,%.2f) back=(%.2f,%.2f,%.2f) box=(%.2f,%.2f,%.2f) boxUp=(%.2f,%.2f,%.2f) weight=%.2f close=%b",
            body.tickCount,
            drawn.x,
            drawn.y,
            drawn.z,
            body.yBodyRot,
            nose.x,
            nose.y,
            nose.z,
            back.x,
            back.y,
            back.z,
            box.centre().x - drawn.x,
            box.centre().y - drawn.y,
            box.centre().z - drawn.z,
            boxUp.x,
            boxUp.y,
            boxUp.z,
            weight,
            close
         )
      );
   }

   private static void watchLimb(String name, Limb L, Vec3 hip, Vector3f target, Quaternionf turn, Vec3 drawn, Matrix4f toWorld) {
      Quaternionf inverse = new Quaternionf(turn).conjugate();
      Vector3f rel = inverse.transform(new Vector3f((float)(target.x - hip.x), (float)(target.y - hip.y), (float)(target.z - hip.z)));
      Vector3f hipBody = inverse.transform(
         new Vector3f((float)(hip.x - drawn.x), (float)(hip.y - drawn.y), (float)(hip.z - drawn.z))
      );
      Vec3 foot = FaunaLegReachSolver.toWorld(toWorld, drawn, L.solEnd);
      double straight = L.iKnee >= 0 ? ZLeg.longest(L.rHip, L.kneeJoint, L.mKnee, L.eFoot, L.hinge) : L.rHip.distance(L.mKnee) + L.mKnee.distance(L.eFoot);
      double scale = toWorld.getScale(new Vector3f()).x;
      Vec3 clip = FaunaLegReachSolver.toWorld(toWorld, drawn, L.eFoot);
      Vector3f clipBody = inverse.transform(
         new Vector3f((float)(clip.x - drawn.x), (float)(clip.y - drawn.y), (float)(clip.z - drawn.z))
      );
      LOG.debug(
         String.format(
            Locale.ROOT,
            "[GripWatch]   %s hip(body)=(%.2f,%.2f,%.2f) clipfoot(body)=(%.2f,%.2f,%.2f) target-hip(body)=(%.2f,%.2f,%.2f) dist=%.2f leg=%.2f miss=%.2f",
            name,
            hipBody.x,
            hipBody.y,
            -hipBody.z,
            clipBody.x,
            clipBody.y,
            -clipBody.z,
            rel.x,
            rel.y,
            -rel.z,
            rel.length(),
            straight * scale,
            foot.distanceTo(new Vec3(target.x, target.y, target.z))
         )
      );
   }

   private static void closeDigits(Limb L, GripGeometry.Box box, Vector3f into, double curlMax, float weight, Vec3 drawn, Matrix4f toWorld, Set<String> touched) {
      Matrix4f effAccum = BoneFk.mulLocal(L.effParentNew, L.eff, new Matrix4f());
      Quaternionf effRotation = BoneFk.orthonormalRotation(effAccum, new Quaternionf());
      double most = curlMax * (float) (Math.PI / 180.0);

      for (AzBone digit : L.eff.getChildBones()) {
         Vector3f tipRest = CubeMesh.farEnd(digit);
         if (tipRest != null) {
            Matrix4f accum = BoneFk.mulLocal(effAccum, digit, new Matrix4f());
            Vector3f pivot = accum.transformPosition(new Vector3f(digit.getPivotX() / 16.0F, digit.getPivotY() / 16.0F, digit.getPivotZ() / 16.0F));
            Vector3f reach = accum.transformPosition(new Vector3f(tipRest)).sub(pivot);
            Vector3f hinge = new Vector3f(reach).cross(into);
            if (!(hinge.lengthSquared() < 1.0E-10F)) {
               hinge.normalize();
               double turn = closure(box, pivot, reach, hinge, most, drawn, toWorld);
               turn *= weight;
               if (!(turn < 1.0E-4)) {
                  Quaternionf world = new Quaternionf()
                     .rotationAxis((float)turn, hinge)
                     .mul(effRotation.mul(BoneFk.localQuat(digit, new Quaternionf()), new Quaternionf()));
                  BoneFk.lockWorldOrientation(digit, effAccum, world);
                  touched.add(digit.getName());
               }
            }
         }
      }
   }

   private static double closure(GripGeometry.Box box, Vector3f pivot, Vector3f reach, Vector3f hinge, double most, Vec3 drawn, Matrix4f toWorld) {
      boolean[] read = new boolean[ALONG.length];
      boolean any = false;

      for (int i = 0; i < ALONG.length; i++) {
         read[i] = depth(box, pivot, reach, hinge, 0.0, ALONG[i], drawn, toWorld) > 0.0;
         any |= read[i];
      }

      if (!any) {
         return 0.0;
      }

      double nearestTurn = 0.0;
      double nearest = clearance(box, pivot, reach, hinge, 0.0, read, drawn, toWorld);
      double last = 0.0;

      for (int k = 1; k <= 8; k++) {
         double turn = most * k / 8.0;
         double gap = clearance(box, pivot, reach, hinge, turn, read, drawn, toWorld);
         if (gap <= 0.0) {
            double lo = last;
            double hi = turn;

            for (int i = 0; i < 8; i++) {
               double mid = (lo + hi) * 0.5;
               if (clearance(box, pivot, reach, hinge, mid, read, drawn, toWorld) <= 0.0) {
                  hi = mid;
               } else {
                  lo = mid;
               }
            }

            return lo;
         }

         if (gap < nearest) {
            nearest = gap;
            nearestTurn = turn;
         }

         last = turn;
      }

      return nearestTurn;
   }

   private static double clearance(
      GripGeometry.Box box, Vector3f pivot, Vector3f reach, Vector3f hinge, double angle, boolean[] read, Vec3 drawn, Matrix4f toWorld
   ) {
      double least = Double.MAX_VALUE;

      for (int i = 0; i < ALONG.length; i++) {
         if (read[i]) {
            least = Math.min(least, depth(box, pivot, reach, hinge, angle, ALONG[i], drawn, toWorld));
         }
      }

      return least;
   }

   private static double depth(GripGeometry.Box box, Vector3f pivot, Vector3f reach, Vector3f hinge, double angle, float along, Vec3 drawn, Matrix4f toWorld) {
      Vector3f point = new Quaternionf().rotationAxis((float)angle, hinge).transform(new Vector3f(reach).mul(along)).add(pivot);
      return box.depth(FaunaLegReachSolver.toWorld(toWorld, drawn, point));
   }
}

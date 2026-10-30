package net.magister.liberanimalium.render.limbs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.BoneFk;
import net.magister.liberanimalium.anim.rig.FootStep;
import net.magister.liberanimalium.anim.rig.GaitPlan;
import net.magister.liberanimalium.anim.rig.LimbDef;
import net.magister.liberanimalium.anim.rig.StepsSpec;
import net.magister.liberanimalium.chassis.component.LimbReachComponent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class LimbRig {
   public AzBakedModel model;
   public StepsSpec spec = StepsSpec.EMPTY;
   public Limb[] limbs = new Limb[0];
   public GaitPlan plan = GaitPlan.of(new double[0], new double[0], new double[0]);
   public FootStep[] steps = new FootStep[0];
   public List<String> touched = List.of();
   public int ceilingTick = Integer.MIN_VALUE;
   public int gripWatchTick = Integer.MIN_VALUE;
   public double ceiling = Double.POSITIVE_INFINITY;
   public String attitudeBone;
   public Consumer<String> traceSink;
   public Set<String> limbsOff = Set.of();
   public double liftSum;
   public double swaySum;
   public double strideSum;
   public double frontLiftSum;
   public double frontSwaySum;
   public double frontStrideSum;
   public double rearLiftSum;
   public double rearSwaySum;
   public double rearStrideSum;
   public int frontCount;
   public int rearCount;
   public double homeRightSum;
   public double homeForwardSum;
   public double standRightSum;
   public double standForwardSum;
   public int homeCount;
   public int standCount;
   public int gaitTick = Integer.MIN_VALUE;
   public int footfallTick = Integer.MIN_VALUE;
   public LimbReachComponent.Gait gait = LimbReachComponent.Gait.NONE;
   public int epoch;
   public final Matrix4f toModel = new Matrix4f();
   public final Matrix4f accum = new Matrix4f();
   public final Matrix4f accumClip = new Matrix4f();
   public final Matrix4f accumHome = new Matrix4f();
   public final Matrix4f walk = new Matrix4f();
   public final Vector3f v = new Vector3f();
   public final Quaternionf q = new Quaternionf();

   public Limb named(String name) {
      for (Limb limb : this.limbs) {
         if (limb.def.name().equals(name)) {
            return limb;
         }
      }

      return null;
   }

   public void resolve(AzBakedModel model, StepsSpec spec) {
      Map<String, LimbState> kept = new HashMap<>();

      for (Limb old : this.limbs) {
         kept.put(old.def.foot(), old.st);
      }

      List<Limb> out = new ArrayList<>();
      LinkedHashSet<String> touched = new LinkedHashSet<>();

      for (LimbDef def : spec.limbs) {
         touched.add(def.root());
         touched.add(def.mid());
         touched.add(def.foot());
         if (def.knee() != null) {
            touched.add(def.knee());
         }

         touched.addAll(def.anchors());
         touched.addAll(def.plantHigh().keySet());
         touched.addAll(def.plantLow().keySet());
         Limb limb = new Limb(def, kept.getOrDefault(def.foot(), new LimbState()));
         limb.root = model.getBoneOrNull(def.root());
         limb.mid = model.getBoneOrNull(def.mid());
         limb.eff = model.getBoneOrNull(def.foot());
         if (limb.root != null && limb.mid != null && limb.eff != null) {
            limb.ancestors = BoneFk.ancestorsTopDown(limb.eff, model);
            limb.iRoot = indexOf(limb.ancestors, limb.root);
            limb.iMid = indexOf(limb.ancestors, limb.mid);
            limb.iEff = indexOf(limb.ancestors, limb.eff);
            if (def.knee() != null) {
               limb.knee = model.getBoneOrNull(def.knee());
               limb.iKnee = limb.knee == null ? -1 : indexOf(limb.ancestors, limb.knee);
               if (limb.knee == null) {
                  StepsDebug.LOGGER.warn("[LegReach] {}: knee bone {} is not in the model; a two-bone leg", def.name(), def.knee());
               } else if (limb.iKnee <= limb.iRoot || limb.iKnee >= limb.iMid) {
                  StepsDebug.LOGGER
                     .warn(
                        "[LegReach] {}: knee {} is not strictly between {} and {} on {}'s chain; a two-bone leg",
                        new Object[]{def.name(), def.knee(), def.root(), def.mid(), def.foot()}
                     );
                  limb.knee = null;
                  limb.iKnee = -1;
               }
            }

            List<AzBone> anchors = new ArrayList<>();

            for (String an : def.anchors()) {
               AzBone ab = model.getBoneOrNull(an);
               if (ab != null) {
                  anchors.add(ab);
               }
            }

            int n = anchors.size();
            limb.anchors = anchors.toArray(new AzBone[0]);
            List<AzBone>[] lists = new List[n];
            limb.anchorAncestors = lists;
            limb.anchorFrom = new int[n];
            limb.anchorHangs = new int[n];
            limb.anchorWorld = new Quaternionf[n];
            limb.anchorLocked = new boolean[n];

            for (int k = 0; k < n; k++) {
               List<AzBone> chain = BoneFk.ancestorsTopDown(limb.anchors[k], model);
               lists[k] = chain;
               limb.anchorWorld[k] = new Quaternionf();
               int from = -1;
               int hangs = -1;

               for (int a = 0; a < chain.size(); a++) {
                  AzBone b = chain.get(a);
                  if (b == limb.root) {
                     from = a;
                     hangs = 0;
                  } else if (b == limb.mid) {
                     from = a;
                     hangs = 1;
                  } else if (b == limb.eff) {
                     from = a;
                     hangs = 2;
                  }
               }

               limb.anchorFrom[k] = from;
               limb.anchorHangs[k] = hangs;
            }

            limb.highBones = poseBones(model, def.plantHigh());
            limb.highDeg = poseDegrees(model, def.plantHigh());
            limb.lowBones = poseBones(model, def.plantLow());
            limb.lowDeg = poseDegrees(model, def.plantLow());
         }

         out.add(limb);
      }

      this.limbs = out.toArray(new Limb[0]);
      this.touched = List.copyOf(touched);
      int n = this.limbs.length;
      double[] px = new double[n];
      double[] py = new double[n];
      double[] pz = new double[n];
      this.steps = new FootStep[n];

      for (int i = 0; i < n; i++) {
         Limb l = this.limbs[i];
         l.index = i;
         this.steps[i] = l.st.step;
         px[i] = l.eff == null ? Double.NaN : l.eff.getPivotX();
         py[i] = l.eff == null ? Double.NaN : l.eff.getPivotY();
         pz[i] = l.eff == null ? Double.NaN : l.eff.getPivotZ();
      }

      this.plan = GaitPlan.of(px, py, pz);

      for (Limb limb : this.limbs) {
         limb.iAttitude = -1;
      }

      this.attitudeBone = null;
      this.model = model;
      this.spec = spec;
   }

   private static int indexOf(List<AzBone> chain, AzBone bone) {
      for (int i = 0; i < chain.size(); i++) {
         if (chain.get(i) == bone) {
            return i;
         }
      }

      return -1;
   }

   private static AzBone[] poseBones(AzBakedModel model, Map<String, float[]> pose) {
      List<AzBone> out = new ArrayList<>();

      for (Entry<String, float[]> e : pose.entrySet()) {
         AzBone b = model.getBoneOrNull(e.getKey());
         if (b != null) {
            out.add(b);
         }
      }

      return out.toArray(new AzBone[0]);
   }

   private static float[][] poseDegrees(AzBakedModel model, Map<String, float[]> pose) {
      List<float[]> out = new ArrayList<>();

      for (Entry<String, float[]> e : pose.entrySet()) {
         if (model.getBoneOrNull(e.getKey()) != null) {
            out.add(e.getValue());
         }
      }

      return out.toArray(new float[0][]);
   }
}

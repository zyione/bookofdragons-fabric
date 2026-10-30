package net.magister.liberanimalium.render;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.BoneAxis;
import net.magister.liberanimalium.anim.rig.BoneFk;
import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.chassis.FaunaBase;

final class SizePass implements RigPass {
   private static final Map<FaunaBase, Map<String, float[]>> WRITTEN = Collections.synchronizedMap(new WeakHashMap<>());

   @Override
   public String name() {
      return "size";
   }

   @Override
   public void solve(RigFrame frame) {
      applySizeScaling(frame);
   }

   private static void applySizeScaling(RigFrame frame) {
      FaunaBase body = (FaunaBase)frame.body;
      float scale = frame.spec.size().scaleFor(body.getGrowthStage());
      String root = FaunaProceduralAnimator.first(frame, "root");
      if (root != null) {
         frame.write.set(root, BoneAxis.SCALE_X, scale);
         frame.write.set(root, BoneAxis.SCALE_Y, scale);
         frame.write.set(root, BoneAxis.SCALE_Z, scale);
         scaleMovesAbove(frame, body, root, scale);
      }
   }

   private static void scaleMovesAbove(RigFrame frame, FaunaBase body, String root, float scale) {
      AzBone bone = frame.model.getBoneOrNull(root);
      if (bone != null) {
         List<AzBone> chain = BoneFk.ancestorsTopDown(bone, frame.model);
         Map<String, float[]> written = WRITTEN.computeIfAbsent(body, b -> new HashMap<>());

         for (int i = 0; i < chain.size() - 1; i++) {
            String name = chain.get(i).getName();
            if (frame.write.has(name)) {
               float[] last = written.computeIfAbsent(name, n -> new float[]{Float.NaN, Float.NaN, Float.NaN});

               for (int a = 0; a < 3; a++) {
                  BoneAxis axis = BoneAxis.values()[BoneAxis.POS_X.ordinal() + a];
                  float now = frame.write.read(name, axis);
                  if (now != 0.0F && now != last[a]) {
                     last[a] = now * scale;
                     frame.write.set(name, axis, last[a]);
                  }
               }
            }
         }
      }
   }
}

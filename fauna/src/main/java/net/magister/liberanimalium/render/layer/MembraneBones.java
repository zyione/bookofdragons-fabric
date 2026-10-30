package net.magister.liberanimalium.render.layer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.model.AzBoneSnapshot;
import net.magister.liberanimalium.anim.rig.BoneMatrices;
import net.magister.liberanimalium.render.BonePalettes;
import net.magister.liberanimalium.render.membrane.MembraneDefinitions;
import net.magister.liberanimalium.render.membrane.MembranePanel;
import net.magister.liberanimalium.render.membrane.MembraneSeams;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class MembraneBones {
   private final Map<BoneMatrices, MembraneBones.Resolved> resolved = Collections.synchronizedMap(new WeakHashMap<>());

   MembraneBones.Resolved fill(int entityId, MembraneDefinitions.Species def, Matrix4f root) {
      BonePalettes.Frame frame = BonePalettes.frame(entityId);
      if (frame == null) {
         return null;
      }

      MembraneBones.Resolved r = this.resolve(frame.layout(), def);
      Matrix4f[] pose = frame.pose();

      for (int k = 0; k < r.index.length; k++) {
         Matrix4f mat = r.mat[k];
         mat.set(root).mul(pose[r.index[k]]);
         mat.transformPosition(r.pos[k].set(r.pivot[k * 3], r.pivot[k * 3 + 1], r.pivot[k * 3 + 2]));
      }

      return r;
   }

   MembraneBones.Resolved resolve(BoneMatrices layout, MembraneDefinitions.Species def) {
      MembraneBones.Resolved cached = this.resolved.get(layout);
      if (cached == null || cached.def != def && !def.equals(cached.def)) {
         List<Integer> idx = new ArrayList<>();
         List<String> names = new ArrayList<>();

         for (String name : def.capturedBones()) {
            int i = layout.indexOf(name);
            if (i >= 0) {
               idx.add(i);
               names.add(name);
            }
         }

         MembraneBones.Resolved r = new MembraneBones.Resolved(idx.size());

         for (int k = 0; k < idx.size(); k++) {
            r.index[k] = idx.get(k);
            AzBone bone = layout.bone(r.index[k]);
            r.pivot[k * 3] = bone.getPivotX() / 16.0F;
            r.pivot[k * 3 + 1] = bone.getPivotY() / 16.0F;
            r.pivot[k * 3 + 2] = bone.getPivotZ() / 16.0F;
            r.positions.put(names.get(k), r.pos[k]);
            r.mats.put(names.get(k), r.mat[k]);
         }

         Map<String, Vector3f> rest = new HashMap<>();

         for (int k = 0; k < idx.size(); k++) {
            rest.put(names.get(k), new Vector3f(r.pivot[k * 3], r.pivot[k * 3 + 1], r.pivot[k * 3 + 2]));
         }

         r.pins = MembraneSeams.of(def.panels(), rest::get);
         r.upSide = new float[def.panels().size()];

         for (int i = 0; i < r.upSide.length; i++) {
            r.upSide[i] = upSide(layout, def.panels().get(i));
         }

         r.def = def;
         this.resolved.put(layout, r);
         return r;
      } else {
         return cached;
      }
   }

   static float upSide(BoneMatrices layout, MembranePanel panel) {
      int anchor = layout.indexOf(panel.dorsalAnchor());
      float[] d = panel.localDorsal();
      if (anchor >= 0 && d != null) {
         Quaternionf turn = new Quaternionf();

         for (int b = anchor; b >= 0; b = layout.parentOf(b)) {
            AzBoneSnapshot rest = layout.bone(b).getInitialAzSnapshot();
            if (rest != null) {
               turn.premul(new Quaternionf().rotateZ(rest.getRotZ()).rotateY(rest.getRotY()).rotateX(rest.getRotX()));
            }
         }

         return turn.transform(new Vector3f(d[0], d[1], d[2])).y() < 0.0F ? -1.0F : 1.0F;
      } else {
         return 1.0F;
      }
   }

   static final class Resolved {
      final int[] index;
      final float[] pivot;
      final Vector3f[] pos;
      final Matrix4f[] mat;
      final Map<String, Vector3f> positions = new HashMap<>();
      final Map<String, Matrix4f> mats = new HashMap<>();
      MembraneSeams.Pins pins;
      float[] upSide;
      MembraneDefinitions.Species def;

      Resolved(int n) {
         this.index = new int[n];
         this.pivot = new float[n * 3];
         this.pos = new Vector3f[n];
         this.mat = new Matrix4f[n];

         for (int k = 0; k < n; k++) {
            this.pos[k] = new Vector3f();
            this.mat[k] = new Matrix4f();
         }
      }
   }
}

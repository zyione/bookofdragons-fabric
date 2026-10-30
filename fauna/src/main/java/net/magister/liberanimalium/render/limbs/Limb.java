package net.magister.liberanimalium.render.limbs;

import java.util.List;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.LimbDef;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Limb {
   public final LimbDef def;
   public final LimbState st;
   public AzBone root;
   public AzBone mid;
   public AzBone eff;
   public List<AzBone> ancestors;
   public int iRoot;
   public int iMid;
   public int iEff;
   public int iAttitude = -1;
   public int index;
   public AzBone[] anchors = new AzBone[0];
   public List<AzBone>[] anchorAncestors;
   public int[] anchorFrom;
   public int[] anchorHangs;
   public Quaternionf[] anchorWorld = new Quaternionf[0];
   public boolean[] anchorLocked = new boolean[0];
   public AzBone knee;
   public int iKnee = -1;
   public final Vector3f kneeJoint = new Vector3f();
   public final Vector3f hinge = new Vector3f();
   public final Vector3f footFolded = new Vector3f();
   public int zSide;
   public final Vector3f hingeLocal = new Vector3f(1.0F, 0.0F, 0.0F);
   public boolean hingeKnown;
   public AzBone[] highBones = new AzBone[0];
   public AzBone[] lowBones = new AzBone[0];
   public float[][] highDeg = new float[0][];
   public float[][] lowDeg = new float[0][];
   public final Matrix4f rootParent = new Matrix4f();
   public final Matrix4f midParent = new Matrix4f();
   public final Matrix4f effParent = new Matrix4f();
   public final Matrix4f effParentClip = new Matrix4f();
   public final Matrix4f newMidParent = new Matrix4f();
   public final Matrix4f effParentNew = new Matrix4f();
   public final Vector3f rHip = new Vector3f();
   public final Vector3f mKnee = new Vector3f();
   public final Vector3f eFoot = new Vector3f();
   public final Vector3f footClip = new Vector3f();
   public final Vector3f newKnee = new Vector3f();
   public final Vector3f newFoot = new Vector3f();
   public final Vector3f target = new Vector3f();
   public final Vector3f solMid = new Vector3f();
   public final Vector3f solEnd = new Vector3f();
   public final Vector3f hipHome = new Vector3f();
   public final Vector3f footHome = new Vector3f();
   public final Quaternionf effAnimWorld = new Quaternionf();
   public final Quaternionf untilt = new Quaternionf();

   public Limb(LimbDef def, LimbState st) {
      this.def = def;
      this.st = st;
   }
}

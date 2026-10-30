package net.magister.liberanimalium.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.BoneMatrices;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BonePalettes {
   public static final PerfClock.Section CLOCK = PerfClock.client("rig.fk");
   private static final Map<Integer, BonePalettes.State> STATES = new ConcurrentHashMap<>();

   private BonePalettes() {
   }

   public static BonePalettes.Frame frame(int entityId) {
      BonePalettes.State state = STATES.get(entityId);
      if (state != null && state.layout != null && state.frame == PerfClock.frameId()) {
         if (state.normalsFrame != state.frame) {
            if (state.normals.length != state.palette.length) {
               state.normals = state.layout.newNormals();
            }

            state.layout.computeNormals(state.normals);
            state.normalsFrame = state.frame;
         }

         return new BonePalettes.Frame(state.layout, state.palette, state.normals);
      } else {
         return null;
      }
   }

   public static void update(int entityId, AzBakedModel model, Matrix4f modelToWorld) {
      if (model != null) {
         int frame = PerfClock.frameId();
         BonePalettes.State state = STATES.computeIfAbsent(entityId, id -> new BonePalettes.State());
         if (state.forModel != model) {
            state.forModel = model;
            state.layout = BoneMatrices.of(model);
            state.palette = state.layout.newPalette();
            state.frame = Integer.MIN_VALUE;
         }

         state.toWorld.set(modelToWorld);
         if (state.frame != frame) {
            long t0 = CLOCK.start();

            try {
               state.layout.compute(state.palette);
               state.frame = frame;
            } finally {
               CLOCK.end(t0);
            }
         }
      }
   }

   public static Vec3 worldPosition(int entityId, String boneName, Vec3 body) {
      Vec3 offset = localOffset(entityId, boneName);
      return offset == null ? null : offset.add(body);
   }

   public static Vec3 localOffset(int entityId, String boneName) {
      return localOffset(entityId, boneName, null);
   }

   public static Vec3 localOffset(int entityId, String boneName, @Nullable Vector3f pivot) {
      BonePalettes.State state = STATES.get(entityId);
      if (state != null && state.layout != null && state.frame != Integer.MIN_VALUE) {
         int i = state.layout.indexOf(boneName);
         if (i < 0) {
            return null;
         }

         AzBone bone = state.layout.bone(i);
         Vector3f local = pivot != null
            ? new Vector3f(pivot.x() / 16.0F, pivot.y() / 16.0F, pivot.z() / 16.0F)
            : new Vector3f(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
         Vector3f model = state.palette[i].transformPosition(local);
         Vector3f world = state.toWorld.transformPosition(model);
         return new Vec3(world.x(), world.y(), world.z());
      } else {
         return null;
      }
   }

   public static Matrix4f worldMatrix(int entityId, String boneName) {
      BonePalettes.State state = STATES.get(entityId);
      if (state != null && state.layout != null && state.frame != Integer.MIN_VALUE) {
         int i = state.layout.indexOf(boneName);
         return i < 0 ? null : new Matrix4f(state.toWorld).mul(state.palette[i]);
      } else {
         return null;
      }
   }

   @Nullable
   public static Matrix4f modelToWorld(int entityId) {
      BonePalettes.State state = STATES.get(entityId);
      return state != null && state.layout != null && state.frame != Integer.MIN_VALUE ? new Matrix4f(state.toWorld) : null;
   }

   public static boolean ready(int entityId) {
      BonePalettes.State state = STATES.get(entityId);
      return state != null && state.frame != Integer.MIN_VALUE;
   }

   public static void forget(int entityId) {
      STATES.remove(entityId);
   }

   public static void clear() {
      STATES.clear();
   }

   public static int tracked() {
      return STATES.size();
   }

   public record Frame(BoneMatrices layout, Matrix4f[] pose, Matrix3f[] normals) {
   }

   private static final class State {
      AzBakedModel forModel;
      BoneMatrices layout;
      Matrix4f[] palette = new Matrix4f[0];
      Matrix3f[] normals = new Matrix3f[0];
      int frame = Integer.MIN_VALUE;
      int normalsFrame = Integer.MIN_VALUE;
      Matrix4f toWorld = new Matrix4f();
   }
}

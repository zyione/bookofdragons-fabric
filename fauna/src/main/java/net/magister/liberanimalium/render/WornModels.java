package net.magister.liberanimalium.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.function.Consumer;
import mod.azure.azurelib.model.AzBakedModel;
import net.magister.liberanimalium.compat.Verts;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class WornModels {
   private WornModels() {
   }

   public static boolean drawCpu(
      int entityId, AzBakedModel worn, Pose root, VertexConsumer out, int light, int overlay, float red, float green, float blue, float alpha
   ) {
      BonePalettes.Frame frame = BonePalettes.frame(entityId);
      if (frame != null && worn != null) {
         ModelMesh mesh = ModelMesh.worn(worn, frame.layout());
         int quads = mesh.quads();
         if (quads == 0) {
            return true;
         }

         float[] posed = new float[quads * 15];
         mesh.transform(frame.pose(), posed);
         float[] uv = mesh.uv();
         Matrix4f m = root.pose();
         Matrix3f n = root.normal();
         int q = 0;

         for (int o = 0; q < quads; o += 15) {
            for (int i = 0; i < 4; i++) {
               int p = o + 3 + i * 3;
               int t = (q * 4 + i) * 2;
               Verts.entity(
                  out,
                  m,
                  n,
                  posed[p],
                  posed[p + 1],
                  posed[p + 2],
                  red,
                  green,
                  blue,
                  alpha,
                  uv[t],
                  uv[t + 1],
                  overlay,
                  light,
                  posed[o],
                  posed[o + 1],
                  posed[o + 2]
               );
            }

            q++;
         }

         return true;
      } else {
         return false;
      }
   }

   public static void upwardPoints(
      int entityId, AzBakedModel wearer, AzBakedModel worn, float minUp, float minArea, float chance, Vec3 body, RandomSource random, Consumer<Vec3> out
   ) {
      BonePalettes.Frame frame = BonePalettes.frame(entityId);
      Matrix4f toWorld = BonePalettes.modelToWorld(entityId);
      if (frame != null && toWorld != null && worn != null && wearer != null) {
         boolean[] dressed = new boolean[frame.layout().size()];

         for (int b : ModelMesh.worn(worn, frame.layout()).bones()) {
            dressed[b] = true;
         }

         ModelMesh mesh = ModelMesh.of(wearer);
         int quads = mesh.quads();
         int[] quadBone = mesh.bones();
         float[] posed = new float[quads * 15];
         mesh.transform(frame.pose(), posed);
         Matrix3f turn = toWorld.get3x3(new Matrix3f());
         Vector3f normal = new Vector3f();
         float minBlocks = minArea / 256.0F;
         int q = 0;

         for (int o = 0; q < quads; o += 15) {
            if (quadBone[q] < dressed.length && dressed[quadBone[q]]) {
               turn.transform(normal.set(posed[o], posed[o + 1], posed[o + 2])).normalize();
               if (!(normal.y() < minUp) && !(quadArea(posed, o + 3) < minBlocks) && !(random.nextFloat() >= chance)) {
                  Vector3f a = corner(posed, o + 3, 0);
                  Vector3f b = corner(posed, o + 3, 1);
                  Vector3f d = corner(posed, o + 3, 3);
                  Vector3f point = new Vector3f(a).add(new Vector3f(b).sub(a).mul(random.nextFloat())).add(new Vector3f(d).sub(a).mul(random.nextFloat()));
                  toWorld.transformPosition(point);
                  out.accept(new Vec3(body.x + point.x(), body.y + point.y(), body.z + point.z()));
               }
            }

            q++;
         }
      }
   }

   private static Vector3f corner(float[] posed, int first, int i) {
      int p = first + i * 3;
      return new Vector3f(posed[p], posed[p + 1], posed[p + 2]);
   }

   private static float quadArea(float[] posed, int first) {
      Vector3f a = corner(posed, first, 0);
      Vector3f b = corner(posed, first, 1);
      Vector3f d = corner(posed, first, 3);
      return new Vector3f(b).sub(a).cross(new Vector3f(d).sub(a)).length();
   }
}

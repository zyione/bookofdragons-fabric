package net.magister.liberanimalium.render.membrane;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Locale;
import java.util.Map;
import net.magister.liberanimalium.compat.Verts;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class MembraneMesher {
   private static final int GU = 3;
   private static final int GV = 2;
   private static final boolean CURVED_CREASE = false;
   private static final float CREASE_BOW = 0.18F;
   private final Vector3f[][] gp = new Vector3f[4][3];
   private final float[][][] guv = new float[4][3][2];
   private final Vector3f dorsalScratch = new Vector3f();
   private final Vector3f dorsalTmp = new Vector3f();
   private final Vector3f nrmScratch = new Vector3f();

   public MembraneMesher() {
      for (int i = 0; i <= 3; i++) {
         for (int j = 0; j <= 2; j++) {
            this.gp[i][j] = new Vector3f();
         }
      }
   }

   public void draw(
      MembranePanel panel,
      Map<String, Matrix4f> mats,
      Vector3f p1,
      Vector3f p2,
      Vector3f p3,
      Vector3f p4,
      float creaseH,
      boolean pinU0,
      boolean pinU1,
      VertexConsumer consumer,
      int light,
      int overlay
   ) {
      Vector3f p1Loc = tailEdge(panel, p1, p2);
      Vector3f p4Loc = tailEdge(panel, p4, p3);
      Vector3f dorsal = this.computeDorsal(panel, mats, p1Loc, p2, p4Loc);
      float cx = (p1Loc.x() + p2.x() + p3.x() + p4Loc.x()) * 0.25F;
      float cy = (p1Loc.y() + p2.y() + p3.y() + p4Loc.y()) * 0.25F;
      float cz = (p1Loc.z() + p2.z() + p3.z() + p4Loc.z()) * 0.25F;
      boolean top = dorsal.x() * cx + dorsal.y() * cy + dorsal.z() * cz < 0.0F;
      this.grid(panel, p1Loc, p2, p3, p4Loc, dorsal, creaseH, pinU0, pinU1, 0.0F, top ? panel.uvTop() : panel.uvBottom());
      this.emitGrid(top, top ? this.nrmScratch.set(dorsal) : this.nrmScratch.set(dorsal).negate(), consumer, light, overlay);
   }

   public void drawOver(
      MembranePanel panel,
      Map<String, Matrix4f> mats,
      Vector3f p1,
      Vector3f p2,
      Vector3f p3,
      Vector3f p4,
      float creaseH,
      boolean pinU0,
      boolean pinU1,
      float side,
      float lift,
      VertexConsumer consumer,
      int light,
      int overlay
   ) {
      Vector3f p1Loc = tailEdge(panel, p1, p2);
      Vector3f p4Loc = tailEdge(panel, p4, p3);
      Vector3f dorsal = this.computeDorsal(panel, mats, p1Loc, p2, p4Loc);
      this.grid(panel, p1Loc, p2, p3, p4Loc, dorsal, creaseH, pinU0, pinU1, side * lift, panel.uvTop());
      this.emitGrid(side > 0.0F, this.nrmScratch.set(dorsal).mul(side), consumer, light, overlay);
   }

   public Vector3f pointOver(
      MembranePanel panel,
      Map<String, Matrix4f> mats,
      Vector3f p1,
      Vector3f p2,
      Vector3f p3,
      Vector3f p4,
      float creaseH,
      boolean pinU0,
      boolean pinU1,
      float side,
      float lift,
      float u,
      float v,
      Vector3f out
   ) {
      Vector3f p1Loc = tailEdge(panel, p1, p2);
      Vector3f p4Loc = tailEdge(panel, p4, p3);
      Vector3f dorsal = this.computeDorsal(panel, mats, p1Loc, p2, p4Loc);
      vertexAt(panel, p1Loc, p2, p3, p4Loc, dorsal, creaseH, pinU0, pinU1, side * lift, u, v, out);
      this.nrmScratch.set(dorsal).mul(side);
      return out;
   }

   public Vector3f normal() {
      return this.nrmScratch;
   }

   private static Vector3f tailEdge(MembranePanel panel, Vector3f corner, Vector3f neighbour) {
      if (!panel.corner()[0].toLowerCase(Locale.ROOT).startsWith("tail")) {
         return corner;
      }

      Vector3f dir = new Vector3f(corner).sub(neighbour);
      return dir.lengthSquared() <= 1.0E-9F ? corner : dir.normalize().mul(0.09375F).add(corner);
   }

   private void grid(
      MembranePanel panel,
      Vector3f p1,
      Vector3f p2,
      Vector3f p3,
      Vector3f p4,
      Vector3f dorsal,
      float creaseH,
      boolean pinU0,
      boolean pinU1,
      float lift,
      float[][] uv
   ) {
      for (int i = 0; i <= 3; i++) {
         for (int j = 0; j <= 2; j++) {
            float u = i / 3.0F;
            float v = j / 2.0F;
            float w00 = (1.0F - u) * (1.0F - v);
            float w10 = u * (1.0F - v);
            float w11 = u * v;
            float w01 = (1.0F - u) * v;
            vertexAt(panel, p1, p2, p3, p4, dorsal, creaseH, pinU0, pinU1, lift, u, v, this.gp[i][j]);
            this.guv[i][j][0] = uv[0][0] * w00 + uv[1][0] * w10 + uv[2][0] * w11 + uv[3][0] * w01;
            this.guv[i][j][1] = uv[0][1] * w00 + uv[1][1] * w10 + uv[2][1] * w11 + uv[3][1] * w01;
         }
      }
   }

   private static Vector3f vertexAt(
      MembranePanel panel,
      Vector3f p1,
      Vector3f p2,
      Vector3f p3,
      Vector3f p4,
      Vector3f dorsal,
      float creaseH,
      boolean pinU0,
      boolean pinU1,
      float lift,
      float u,
      float v,
      Vector3f out
   ) {
      float w00 = (1.0F - u) * (1.0F - v);
      float w10 = u * (1.0F - v);
      float w11 = u * v;
      float w01 = (1.0F - u) * v;
      float px = p1.x() * w00 + p2.x() * w10 + p3.x() * w11 + p4.x() * w01;
      float py = p1.y() * w00 + p2.y() * w10 + p3.y() * w11 + p4.y() * w01;
      float pz = p1.z() * w00 + p2.z() * w10 + p3.z() * w11 + p4.z() * w01;
      float uSpan = panel.uStart() + (panel.uEnd() - panel.uStart()) * u;
      float taper = (panel.creasePeak() == 1 ? uSpan : 1.0F - uSpan) * MembraneSeams.pinned(pinU0, pinU1, u);
      float prof = (1.0F - Math.abs(2.0F * v - 1.0F)) * taper;
      float d = lift - creaseH * prof;
      return out.set(px + dorsal.x() * d, py + dorsal.y() * d, pz + dorsal.z() * d);
   }

   private void emitGrid(boolean top, Vector3f nrm, VertexConsumer consumer, int light, int overlay) {
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 2; j++) {
            if (top) {
               emit(consumer, this.gp[i][j], nrm, this.guv[i][j], light, overlay);
               emit(consumer, this.gp[i + 1][j], nrm, this.guv[i + 1][j], light, overlay);
               emit(consumer, this.gp[i + 1][j + 1], nrm, this.guv[i + 1][j + 1], light, overlay);
               emit(consumer, this.gp[i][j + 1], nrm, this.guv[i][j + 1], light, overlay);
            } else {
               emit(consumer, this.gp[i][j + 1], nrm, this.guv[i][j + 1], light, overlay);
               emit(consumer, this.gp[i + 1][j + 1], nrm, this.guv[i + 1][j + 1], light, overlay);
               emit(consumer, this.gp[i + 1][j], nrm, this.guv[i + 1][j], light, overlay);
               emit(consumer, this.gp[i][j], nrm, this.guv[i][j], light, overlay);
            }
         }
      }
   }

   private Vector3f computeDorsal(MembranePanel panel, Map<String, Matrix4f> mats, Vector3f p1, Vector3f p2, Vector3f p4) {
      Matrix4f am = mats.get(panel.dorsalAnchor());
      if (am != null && panel.localDorsal() != null) {
         float[] ld = panel.localDorsal();
         am.transformDirection(this.dorsalScratch.set(ld[0], ld[1], ld[2]));
         if (this.dorsalScratch.lengthSquared() > 1.0E-9F) {
            return this.dorsalScratch.normalize();
         }
      }

      this.dorsalScratch.set(p2).sub(p1).cross(this.dorsalTmp.set(p4).sub(p1));
      return this.dorsalScratch.lengthSquared() > 1.0E-9F ? this.dorsalScratch.normalize() : this.dorsalScratch.set(0.0F, 1.0F, 0.0F);
   }

   private static void emit(VertexConsumer consumer, Vector3f p, Vector3f normal, float[] uv, int light, int overlay) {
      Verts.entity(consumer, p.x(), p.y(), p.z(), 1.0F, 1.0F, 1.0F, 1.0F, uv[0], uv[1], overlay, light, normal.x(), normal.y(), normal.z());
   }
}

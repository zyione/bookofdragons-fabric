package net.magister.bookofdragons.client.gui.book;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.magister.liberanimalium.compat.Verts;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

public final class RadarGraph {
   private static final int RINGS = 3;
   private static final float GRID_WIDTH = 0.5F;
   private static final float LINE_WIDTH = 1.0F;
   private static final float LABEL_GAP = 3.0F;
   private static final float FLOOR = 0.04F;

   private RadarGraph() {
   }

   public static void draw(
      GuiGraphics graphics,
      Font font,
      List<Component> labels,
      float[] values,
      int x,
      int y,
      int w,
      int h,
      float labelScale,
      int ink,
      int grid,
      int fill,
      int line
   ) {
      int n = values.length;
      if (n >= 3) {
         RadarGraph.Shape shape = RadarGraph.Shape.of(font, labels, n, x, y, w, h, labelScale);
         float cx = shape.cx;
         float cy = shape.cy;
         float r = shape.r;
         float[] sx = shape.sx;
         float[] sy = shape.sy;
         Matrix4f m = graphics.pose().last().pose();
         VertexConsumer vc = graphics.bufferSource().getBuffer(RenderType.gui());

         for (int ring = 1; ring <= 3; ring++) {
            float rr = r * ring / 3.0F;

            for (int i = 0; i < n; i++) {
               int j = (i + 1) % n;
               line(vc, m, cx + sx[i] * rr, cy + sy[i] * rr, cx + sx[j] * rr, cy + sy[j] * rr, 0.5F, grid);
            }
         }

         for (int i = 0; i < n; i++) {
            line(vc, m, cx, cy, cx + sx[i] * r, cy + sy[i] * r, 0.5F, grid);
         }

         float[] px = new float[n];
         float[] py = new float[n];

         for (int i = 0; i < n; i++) {
            float v = r * Math.max(0.04F, Math.min(1.0F, values[i]));
            px[i] = cx + sx[i] * v;
            py[i] = cy + sy[i] * v;
         }

         for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            quad(vc, m, cx, cy, px[i], py[i], px[j], py[j], px[j], py[j], fill);
         }

         for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            line(vc, m, px[i], py[i], px[j], py[j], 1.0F, line);
         }

         graphics.flush();

         for (int i = 0; i < n && i < labels.size(); i++) {
            float[] box = shape.label(font, labels.get(i), i);
            graphics.pose().pushPose();
            graphics.pose().translate(box[0], box[1], 0.0F);
            graphics.pose().scale(labelScale, labelScale, 1.0F);
            graphics.drawString(font, BookText.of(labels.get(i)), 0, 0, ink, false);
            graphics.pose().popPose();
         }
      }
   }

   public static int labelAt(Font font, List<Component> labels, int n, int x, int y, int w, int h, float labelScale, double mouseX, double mouseY) {
      if (n < 3) {
         return -1;
      }

      RadarGraph.Shape shape = RadarGraph.Shape.of(font, labels, n, x, y, w, h, labelScale);

      for (int i = 0; i < n && i < labels.size(); i++) {
         float[] box = shape.label(font, labels.get(i), i);
         if (mouseX >= box[0] - 1.0F && mouseX < box[0] + box[2] + 1.0F && mouseY >= box[1] - 1.0F && mouseY < box[1] + box[3] + 1.0F) {
            return i;
         }
      }

      return -1;
   }

   private static double angle(int i, int n) {
      return (-Math.PI / 2) + i * 2 * Math.PI / n;
   }

   private static void line(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float width, int argb) {
      float dx = x1 - x0;
      float dy = y1 - y0;
      float len = (float)Math.sqrt(dx * dx + dy * dy);
      if (!(len < 1.0E-4F)) {
         float nx = -dy / len * width / 2.0F;
         float ny = dx / len * width / 2.0F;
         quad(vc, m, x0 + nx, y0 + ny, x0 - nx, y0 - ny, x1 - nx, y1 - ny, x1 + nx, y1 + ny, argb);
      }
   }

   private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
      float a = (argb >>> 24 & 0xFF) / 255.0F;
      float r = (argb >> 16 & 0xFF) / 255.0F;
      float g = (argb >> 8 & 0xFF) / 255.0F;
      float b = (argb & 0xFF) / 255.0F;
      float turn = x0 * y1 - x1 * y0 + (x1 * y2 - x2 * y1) + (x2 * y3 - x3 * y2) + (x3 * y0 - x0 * y3);
      if (turn > 0.0F) {
         float tx = x1;
         float ty = y1;
         x1 = x3;
         y1 = y3;
         x3 = tx;
         y3 = ty;
      }

      Verts.posColor(vc, m, x0, y0, 0.0F, r, g, b, a);
      Verts.posColor(vc, m, x1, y1, 0.0F, r, g, b, a);
      Verts.posColor(vc, m, x2, y2, 0.0F, r, g, b, a);
      Verts.posColor(vc, m, x3, y3, 0.0F, r, g, b, a);
   }

   private record Shape(float cx, float cy, float r, float[] sx, float[] sy, float labelH, float labelScale) {
      static RadarGraph.Shape of(Font font, List<Component> labels, int n, int x, int y, int w, int h, float labelScale) {
         float labelW = 0.0F;

         for (Component label : labels) {
            labelW = Math.max(labelW, font.width(BookText.of(label)) * labelScale);
         }

         float labelH = BookText.line(font) * labelScale;
         float r = Math.max(6.0F, Math.min(w / 2.0F - labelW - 3.0F, h / 2.0F - labelH - 1.0F));
         float[] sx = new float[n];
         float[] sy = new float[n];

         for (int i = 0; i < n; i++) {
            double a = RadarGraph.angle(i, n);
            sx[i] = (float)Math.cos(a);
            sy[i] = (float)Math.sin(a);
         }

         return new RadarGraph.Shape(x + w / 2.0F, y + h / 2.0F, r, sx, sy, labelH, labelScale);
      }

      float[] label(Font font, Component label, int i) {
         float tw = font.width(BookText.of(label)) * this.labelScale;
         float lx = this.cx + this.sx[i] * (this.r + 3.0F);
         float ly = this.cy + this.sy[i] * (this.r + 3.0F);
         float tx = this.sx[i] > 0.3F ? lx : (this.sx[i] < -0.3F ? lx - tw : lx - tw / 2.0F);
         float ty = this.sy[i] < -0.3F ? ly - this.labelH : (this.sy[i] > 0.3F ? ly : ly - this.labelH / 2.0F);
         return new float[]{tx, ty, tw, this.labelH};
      }
   }
}

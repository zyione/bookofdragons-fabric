package net.magister.liberanimalium.debug.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.magister.liberanimalium.compat.Meshes;
import net.magister.liberanimalium.compat.Verts;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.event.RenderStage;
import net.magister.liberanimalium.platform.event.StageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class LegReachDebugRenderer {
   private static final Map<Integer, List<LegReachDebugRenderer.Line>> LINES = new ConcurrentHashMap<>();

   public static void clientInit() {
      ClientEvents.RENDER_STAGE.register(LegReachDebugRenderer::onRenderLevel);
   }

   private LegReachDebugRenderer() {
   }

   public static void line(int entityId, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b) {
      LINES.computeIfAbsent(entityId, k -> new ArrayList<>()).add(new LegReachDebugRenderer.Line(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), r, g, b));
   }

   public static void cross(int entityId, double x, double y, double z, float s, float r, float g, float b) {
      line(entityId, x - s, y, z, x + s, y, z, r, g, b);
      line(entityId, x, y - s, z, x, y + s, z, r, g, b);
      line(entityId, x, y, z - s, x, y, z + s, r, g, b);
   }

   public static void sphere(int entityId, double x, double y, double z, double radius, float r, float g, float b) {
      int segments = 12;

      for (int i = 0; i < 12; i++) {
         double a0 = (Math.PI * 2) * i / 12.0;
         double a1 = (Math.PI * 2) * (i + 1) / 12.0;
         double c0 = Math.cos(a0) * radius;
         double s0 = Math.sin(a0) * radius;
         double c1 = Math.cos(a1) * radius;
         double s1 = Math.sin(a1) * radius;
         line(entityId, x + c0, y, z + s0, x + c1, y, z + s1, r, g, b);
         line(entityId, x + c0, y + s0, z, x + c1, y + s1, z, r, g, b);
         line(entityId, x, y + c0, z + s0, x, y + c1, z + s1, r, g, b);
      }
   }

   public static void onRenderLevel(StageContext event) {
      if (event.getStage() == RenderStage.AFTER_TRANSLUCENT_BLOCKS && !LINES.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level == null) {
            LINES.clear();
         } else {
            Vec3 cam = event.getCamera().getPosition();
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            poseStack.translate(-cam.x, -cam.y, -cam.z);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.lineWidth(2.0F);
            Matrix4f matrix = poseStack.last().pose();
            BufferBuilder buffer = Meshes.immediate(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

            for (List<LegReachDebugRenderer.Line> lines : LINES.values()) {
               for (LegReachDebugRenderer.Line l : lines) {
                  Verts.posColor(buffer, matrix, (float)l.a().x, (float)l.a().y, (float)l.a().z, l.r(), l.g(), l.bl(), 1.0F);
                  Verts.posColor(buffer, matrix, (float)l.b().x, (float)l.b().y, (float)l.b().z, l.r(), l.g(), l.bl(), 1.0F);
               }
            }

            Meshes.draw(buffer);
            poseStack.popPose();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            LINES.clear();
         }
      }
   }

   public record Line(Vec3 a, Vec3 b, float r, float g, float bl) {
   }
}

package net.magister.liberanimalium.render.gpu;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.anim.rig.BoneMatrices;
import net.magister.liberanimalium.compat.ClientFrame;
import net.magister.liberanimalium.render.BonePalettes;
import net.magister.liberanimalium.render.FaunaRenderTypes;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;

public final class InstancedBatches {
   public static final PerfClock.Section CLOCK = PerfClock.client("gpu.instanced");
   private static final int HEADER_ROWS = 2;
   private static final Map<RenderType, InstancedBatches.Batch> BATCHES = new HashMap<>();
   private static int drawnLastFrame;
   private static int instancesLastFrame;
   private static int drawnThisFrame;
   private static int instancesThisFrame;
   private static int maxRows = -1;
   private static final Matrix4f OVERLAY_MODEL_VIEW = new Matrix4f();

   private InstancedBatches() {
   }

   public static int enqueue(
      RenderType type,
      String species,
      SkinnedMesh mesh,
      BoneMatrices layout,
      BonePalettes.Frame frame,
      boolean[] draw,
      Matrix4f modelView,
      Matrix3f rootNormal,
      int packedLight,
      int packedOverlay,
      float red,
      float green,
      float blue,
      float alpha,
      int layer
   ) {
      SkinnedMesh canonical = SkinnedMesh.canonical(species, mesh, layout);
      if (canonical == null) {
         return -1;
      }

      FaunaRenderTypes.Kind kind = FaunaRenderTypes.kindOf(type);
      InstancedBatches.Batch batch = BATCHES.get(type);
      if (batch == null) {
         batch = new InstancedBatches.Batch(canonical, type, kind);
         BATCHES.put(type, batch);
      } else if (batch.mesh != canonical) {
         flushOne(batch);
         batch = new InstancedBatches.Batch(canonical, type, kind);
         BATCHES.put(type, batch);
      }

      int bones = frame.pose().length;
      int rowsIfAdded = 2 * (batch.instances + 1) + batch.boneRows + bones;
      if (batch.instances > 0 && rowsIfAdded > maxRows()) {
         flushOne(batch);
      }

      int i = batch.instances;
      int floatsPerRow = 32;
      batch.rows = ensure(batch.rows, (batch.boneRows + bones + 2 * (i + 1)) * floatsPerRow);
      int boneRowStart = batch.boneRows;
      BonePaletteTexture.pack(frame.pose(), frame.normals(), draw, batch.rows, boneRowStart);
      batch.headers = ensure(batch.headers, (i + 1) * 2 * floatsPerRow);
      float[] hd = batch.headers;
      int h = i * 2 * floatsPerRow;
      modelView.get(hd, h);
      hd[h + 16] = rootNormal.m00();
      hd[h + 17] = rootNormal.m01();
      hd[h + 18] = rootNormal.m02();
      hd[h + 19] = 0.0F;
      hd[h + 20] = rootNormal.m10();
      hd[h + 21] = rootNormal.m11();
      hd[h + 22] = rootNormal.m12();
      hd[h + 23] = 0.0F;
      hd[h + 24] = rootNormal.m20();
      hd[h + 25] = rootNormal.m21();
      hd[h + 26] = rootNormal.m22();
      hd[h + 27] = 0.0F;
      hd[h + 28] = packedLight & 65535;
      hd[h + 29] = packedLight >> 16 & 65535;
      hd[h + 30] = packedOverlay & 65535;
      hd[h + 31] = packedOverlay >> 16 & 65535;
      int h1 = h + floatsPerRow;
      hd[h1] = red;
      hd[h1 + 1] = green;
      hd[h1 + 2] = blue;
      hd[h1 + 3] = alpha;
      hd[h1 + 4] = layer;
      hd[h1 + 5] = boneRowStart;
      hd[h1 + 6] = 0.0F;
      hd[h1 + 7] = 0.0F;
      batch.instances = i + 1;
      batch.boneRows += bones;
      instancesThisFrame++;
      return i;
   }

   public static boolean overlay(RenderType bodyType, int instance, RenderType overlayType, float red, float green, float blue, float alpha) {
      return overlay(bodyType, instance, null, overlayType, red, green, blue, alpha);
   }

   public static boolean overlay(
      RenderType bodyType, int instance, @Nullable SkinnedMesh mesh, RenderType overlayType, float red, float green, float blue, float alpha
   ) {
      InstancedBatches.Batch batch = BATCHES.get(bodyType);
      if (batch != null && instance >= 0 && instance < batch.instances && FaunaShaders.overlay() != null) {
         batch.overlays.add(new InstancedBatches.Overlay(instance, mesh, overlayType, red, green, blue, alpha));
         return true;
      } else {
         return false;
      }
   }

   private static float[] ensure(float[] array, int floats) {
      if (array.length >= floats) {
         return array;
      }

      int want = Math.max(floats, array.length * 2);
      float[] grown = new float[want];
      System.arraycopy(array, 0, grown, 0, array.length);
      return grown;
   }

   private static int maxRows() {
      if (maxRows < 0) {
         maxRows = GL11.glGetInteger(3379);
      }

      return maxRows;
   }

   public static void flush() {
      long t = CLOCK.start();

      try {
         for (InstancedBatches.Batch batch : BATCHES.values()) {
            if (batch.instances > 0) {
               flushOne(batch);
            }
         }

         drawnLastFrame = drawnThisFrame;
         instancesLastFrame = instancesThisFrame;
         drawnThisFrame = 0;
         instancesThisFrame = 0;
      } finally {
         CLOCK.end(t);
      }
   }

   private static void flushOne(InstancedBatches.Batch batch) {
      ShaderInstance shader = batch.kind == FaunaRenderTypes.Kind.HURT ? FaunaShaders.instancedCutout() : FaunaShaders.instanced();
      if (shader == null) {
         batch.instances = 0;
         batch.boneRows = 0;
         batch.overlays.clear();
      } else {
         int floatsPerRow = 32;
         int headerRows = 2 * batch.instances;
         int rows = batch.boneRows + headerRows;
         batch.rows = ensure(batch.rows, rows * floatsPerRow);
         System.arraycopy(batch.headers, 0, batch.rows, batch.boneRows * floatsPerRow, headerRows * floatsPerRow);
         int texture = BonePaletteTexture.uploadRows(batch.rows, rows);
         batch.type.setupRenderState();
         RenderSystem.setShaderTexture(3, texture);

         for (int i = 0; i < 12; i++) {
            shader.setSampler("Sampler" + i, RenderSystem.getShaderTexture(i));
         }

         if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
         }

         Uniform viewRotation = shader.getUniform("IViewRotMat");
         if (viewRotation != null) {
            viewRotation.set(ClientFrame.inverseViewRotation());
         }

         if (shader.COLOR_MODULATOR != null) {
            shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
         }

         if (shader.FOG_START != null) {
            shader.FOG_START.set(RenderSystem.getShaderFogStart());
         }

         if (shader.FOG_END != null) {
            shader.FOG_END.set(RenderSystem.getShaderFogEnd());
         }

         if (shader.FOG_COLOR != null) {
            shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
         }

         if (shader.FOG_SHAPE != null) {
            shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
         }

         if (shader.TEXTURE_MATRIX != null) {
            shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
         }

         if (shader.GAME_TIME != null) {
            shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
         }

         if (shader.SCREEN_SIZE != null) {
            Window window = Minecraft.getInstance().getWindow();
            shader.SCREEN_SIZE.set(window.getWidth(), window.getHeight());
         }

         RenderSystem.setupShaderLights(shader);
         shader.safeGetUniform("HeaderBase").set(batch.boneRows);
         shader.apply();
         VertexBuffer buffer = batch.mesh.buffer();
         buffer.bind();
         int indexCount = batch.mesh.quads() * 6;
         AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(Mode.QUADS);
         indices.bind(indexCount);
         GL31.glDrawElementsInstanced(Mode.QUADS.asGLMode, indexCount, indices.type().asGLType, 0L, batch.instances);
         VertexBuffer.unbind();
         shader.clear();
         RenderSystem.setShaderTexture(3, 0);
         batch.type.clearRenderState();

         for (InstancedBatches.Overlay o : batch.overlays) {
            int h = o.instance * 2 * floatsPerRow;
            OVERLAY_MODEL_VIEW.set(batch.headers, h);
            int boneBase = (int)batch.headers[h + floatsPerRow + 5];
            GpuSkin.drawOverlayMesh(o.mesh != null ? o.mesh : batch.mesh, o.type, OVERLAY_MODEL_VIEW, texture, boneBase, o.red, o.green, o.blue, o.alpha);
         }

         drawnThisFrame++;
         batch.instances = 0;
         batch.boneRows = 0;
         batch.overlays.clear();
      }
   }

   public static void discard() {
      for (InstancedBatches.Batch batch : BATCHES.values()) {
         batch.instances = 0;
         batch.boneRows = 0;
         batch.overlays.clear();
      }

      drawnThisFrame = 0;
      instancesThisFrame = 0;
   }

   public static void release() {
      BATCHES.clear();
      discard();
   }

   public static int drawsLastFrame() {
      return drawnLastFrame;
   }

   public static int instancesLastFrame() {
      return instancesLastFrame;
   }

   private static final class Batch {
      final SkinnedMesh mesh;
      final RenderType type;
      final FaunaRenderTypes.Kind kind;
      float[] rows = new float[0];
      float[] headers = new float[0];
      int instances;
      int boneRows;
      final List<InstancedBatches.Overlay> overlays = new ArrayList<>();

      Batch(SkinnedMesh mesh, RenderType type, FaunaRenderTypes.Kind kind) {
         this.mesh = mesh;
         this.type = type;
         this.kind = kind;
      }
   }

   private record Overlay(int instance, @Nullable SkinnedMesh mesh, RenderType type, float red, float green, float blue, float alpha) {
   }
}

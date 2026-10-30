package net.magister.liberanimalium.render.gpu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.BoneMatrices;
import net.magister.liberanimalium.compat.ClientFrame;
import net.magister.liberanimalium.render.BonePalettes;
import net.magister.liberanimalium.render.FaunaRenderTypes;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class GpuSkin {
   private static final PerfClock.Section PALETTE = PerfClock.client("gpu.palette");
   private static final PerfClock.Section DRAW = PerfClock.client("gpu.draw");
   private static volatile boolean enabled = true;
   private static int drawn;
   private static int drawnFrame = Integer.MIN_VALUE;
   private static final Map<Integer, GpuSkin.Drawn> DRAWN = new HashMap<>();
   private static int overlays;
   private static int overlaysFrame = Integer.MIN_VALUE;
   private static final int[] TALLY = new int[GpuSkin.Decline.values().length];
   private static final int[] LAST = new int[GpuSkin.Decline.values().length];
   private static int tallyFrame = Integer.MIN_VALUE;

   private GpuSkin() {
   }

   public static boolean enabled() {
      return enabled;
   }

   public static void setEnabled(boolean value) {
      enabled = value;
   }

   private static void countDrawn() {
      int frameId = PerfClock.frameId();
      if (frameId != drawnFrame) {
         drawnFrame = frameId;
         drawn = 0;
      }

      drawn++;
   }

   public static int drawnLastFrame() {
      return drawn;
   }

   private static GpuSkin.Drawn remember(int entityId, GpuSkin.Drawn.Mode mode, SkinnedMesh mesh, PoseStack root) {
      GpuSkin.Drawn d = DRAWN.computeIfAbsent(entityId, id -> new GpuSkin.Drawn());
      d.frame = PerfClock.frameId();
      d.mode = mode;
      d.mesh = mesh;
      d.batchType = null;
      d.instance = -1;
      d.root.set(root.last().pose());
      d.rootNormal.set(root.last().normal());
      return d;
   }

   public static void forget(int entityId) {
      DRAWN.remove(entityId);
   }

   public static void clear() {
      DRAWN.clear();
   }

   public static int overlaysLastFrame() {
      return overlays;
   }

   public static boolean drawOverlay(int entityId, RenderType type, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      return drawOver(entityId, null, type, packedLight, packedOverlay, red, green, blue, alpha);
   }

   public static boolean drawWorn(
      int entityId, AzBakedModel worn, RenderType type, int packedLight, int packedOverlay, float red, float green, float blue, float alpha
   ) {
      BonePalettes.Frame frame = BonePalettes.frame(entityId);
      if (frame != null && worn != null) {
         SkinnedMesh mesh = SkinnedMesh.worn(worn, frame.layout());
         return mesh != null && drawOver(entityId, mesh, type, packedLight, packedOverlay, red, green, blue, alpha);
      } else {
         return false;
      }
   }

   private static boolean drawOver(
      int entityId, @Nullable SkinnedMesh worn, RenderType type, int packedLight, int packedOverlay, float red, float green, float blue, float alpha
   ) {
      GpuSkin.Drawn d = DRAWN.get(entityId);
      if (d != null && d.frame == PerfClock.frameId()) {
         SkinnedMesh mesh = worn != null ? worn : d.mesh;
         boolean shown;
         switch (d.mode) {
            case INSTANCED:
               shown = InstancedBatches.overlay(d.batchType, d.instance, worn, type, red, green, blue, alpha);
               break;
            case IMMEDIATE: {
               if (FaunaShaders.overlay() == null) {
                  return false;
               }

               int texture = paletteFor(entityId, mesh);
               if (texture < 0) {
                  return false;
               }

               Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(d.root);
               drawOverlayMesh(mesh, type, modelView, texture, 0, red, green, blue, alpha);
               shown = true;
               break;
            }
            case FEEDBACK: {
               int texture = paletteFor(entityId, mesh);
               shown = texture >= 0 && SkinnedFeedback.draw(mesh, type, d.root, d.rootNormal, texture, packedLight, packedOverlay, red, green, blue, alpha);
               break;
            }
            default:
               shown = false;
         }

         if (shown) {
            int frameId = PerfClock.frameId();
            if (frameId != overlaysFrame) {
               overlaysFrame = frameId;
               overlays = 0;
            }

            overlays++;
         }

         return shown;
      } else {
         return false;
      }
   }

   private static int paletteFor(int entityId, SkinnedMesh mesh) {
      BonePalettes.Frame frame = BonePalettes.frame(entityId);
      if (frame != null && frame.pose().length == mesh.bones()) {
         long t0 = PALETTE.start();

         try {
            return BonePaletteTexture.upload(frame.pose(), frame.normals(), drawMask(frame.layout()));
         } finally {
            PALETTE.end(t0);
         }
      } else {
         return -1;
      }
   }

   static void drawOverlayMesh(
      SkinnedMesh mesh, RenderType type, Matrix4f modelView, int palette, int boneBase, float red, float green, float blue, float alpha
   ) {
      ShaderInstance program = FaunaShaders.overlay();
      if (program != null) {
         long t1 = DRAW.start();

         try {
            type.setupRenderState();
            RenderSystem.setShaderTexture(3, palette);
            program.safeGetUniform("VertexColor").set(red, green, blue, alpha);
            program.safeGetUniform("BoneBase").set(boneBase);
            VertexBuffer buffer = mesh.buffer();
            buffer.bind();
            buffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), program);
            VertexBuffer.unbind();
            RenderSystem.setShaderTexture(3, 0);
            type.clearRenderState();
         } finally {
            DRAW.end(t1);
         }
      }
   }

   public static void note(GpuSkin.Decline reason) {
      int frame = PerfClock.frameId();
      if (frame != tallyFrame) {
         System.arraycopy(TALLY, 0, LAST, 0, TALLY.length);
         Arrays.fill(TALLY, 0);
         tallyFrame = frame;
      }

      TALLY[reason.ordinal()]++;
   }

   public static String tally() {
      StringBuilder out = new StringBuilder();

      for (GpuSkin.Decline d : GpuSkin.Decline.values()) {
         int n = LAST[d.ordinal()];
         if (n > 0) {
            if (out.length() > 0) {
               out.append(" | ");
            }

            out.append(d.name().toLowerCase(Locale.ROOT)).append(' ').append(n);
         }
      }

      return out.length() == 0 ? "nothing drawn yet" : out.toString();
   }

   public static ShaderInstance shaderFor(RenderType type) {
      if (!FaunaRenderTypes.isSpecies(type)) {
         return null;
      }

      return switch (FaunaRenderTypes.kindOf(type)) {
         case BODY -> FaunaShaders.skinned();
         case HURT -> FaunaShaders.skinnedCutout();
         default -> null;
      };
   }

   public static GpuSkin.Decline why(int entityId, AzBakedModel model, RenderType type) {
      if (!enabled) {
         return GpuSkin.Decline.SWITCHED_OFF;
      }

      if (!FaunaShaders.available()) {
         return GpuSkin.Decline.NO_SHADER;
      }

      if (FaunaShaders.packActive()) {
         FaunaRenderTypes.Kind kind = FaunaRenderTypes.isBody(type) ? FaunaRenderTypes.Kind.BODY : FaunaRenderTypes.kindOf(type);
         if (kind == FaunaRenderTypes.Kind.OTHER) {
            return GpuSkin.Decline.RENDER_TYPE;
         }
      } else if (shaderFor(type) == null) {
         return FaunaRenderTypes.isBody(type) ? GpuSkin.Decline.NO_LAYER : GpuSkin.Decline.RENDER_TYPE;
      }

      if (model == null) {
         return GpuSkin.Decline.NO_MODEL;
      } else if (BonePalettes.frame(entityId) == null) {
         return GpuSkin.Decline.NO_PALETTE;
      } else {
         return SkinnedMesh.of(model) == null ? GpuSkin.Decline.NO_MESH : GpuSkin.Decline.NONE;
      }
   }

   public static boolean draw(
      int entityId,
      AzBakedModel model,
      RenderType type,
      PoseStack root,
      int packedLight,
      int packedOverlay,
      float red,
      float green,
      float blue,
      float alpha,
      int layer
   ) {
      boolean feedback = FaunaShaders.packActive();
      ShaderInstance shader = feedback ? null : shaderFor(type);
      if (!feedback && layer < 0) {
         return false;
      }

      BonePalettes.Frame frame = BonePalettes.frame(entityId);
      SkinnedMesh mesh = SkinnedMesh.of(model);
      if ((feedback || shader != null) && frame != null && mesh != null) {
         Matrix4f[] pose = frame.pose();
         Matrix3f[] normals = frame.normals();
         if (pose.length != mesh.bones()) {
            return false;
         }

         if (!feedback && SpeciesBatches.deferring()) {
            Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(root.last().pose());
            String species = FaunaRenderTypes.speciesOf(type);
            int instance = species == null
               ? -1
               : InstancedBatches.enqueue(
                  type,
                  species,
                  mesh,
                  frame.layout(),
                  frame,
                  drawMask(frame.layout()),
                  modelView,
                  root.last().normal(),
                  packedLight,
                  packedOverlay,
                  red,
                  green,
                  blue,
                  alpha,
                  layer
               );
            if (instance >= 0) {
               GpuSkin.Drawn d = remember(entityId, GpuSkin.Drawn.Mode.INSTANCED, mesh, root);
               d.batchType = type;
               d.instance = instance;
               countDrawn();
               return true;
            }
         }

         long t0 = PALETTE.start();

         int texture;
         try {
            texture = BonePaletteTexture.upload(pose, normals, drawMask(frame.layout()));
         } finally {
            PALETTE.end(t0);
         }

         if (feedback) {
            boolean shown = SkinnedFeedback.draw(mesh, type, root, texture, packedLight, packedOverlay, red, green, blue, alpha);
            if (shown) {
               remember(entityId, GpuSkin.Drawn.Mode.FEEDBACK, mesh, root);
               countDrawn();
            }

            return shown;
         } else {
            long t1 = DRAW.start();

            try {
               type.setupRenderState();
               RenderSystem.setShaderTexture(3, texture);
               Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(root.last().pose());
               shader.safeGetUniform("NormalMat").set(new Matrix3f(root.last().normal()));
               shader.safeGetUniform("VertexColor").set(red, green, blue, alpha);
               shader.safeGetUniform("LightUV").set(packedLight & 65535, packedLight >> 16 & 65535);
               shader.safeGetUniform("OverlayUV").set(packedOverlay & 65535, packedOverlay >> 16 & 65535);
               shader.safeGetUniform("Layer").set(layer);
               shader.safeGetUniform("IViewRotMat").set(ClientFrame.inverseViewRotation());
               VertexBuffer buffer = mesh.buffer();
               buffer.bind();
               buffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), shader);
               VertexBuffer.unbind();
               RenderSystem.setShaderTexture(3, 0);
               type.clearRenderState();
            } finally {
               DRAW.end(t1);
            }

            remember(entityId, GpuSkin.Drawn.Mode.IMMEDIATE, mesh, root);
            countDrawn();
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean[] drawMask(BoneMatrices layout) {
      int n = layout.size();
      boolean[] draw = new boolean[n];
      boolean[] reachable = reachableMask(layout);

      for (int i = 0; i < n; i++) {
         AzBone bone = layout.bone(i);
         draw[i] = reachable[i] && !bone.isHidden() && !collapsed(bone);
      }

      return draw;
   }

   public static boolean[] reachableMask(BoneMatrices layout) {
      int n = layout.size();
      boolean[] reachable = new boolean[n];

      for (int i = 0; i < n; i++) {
         int p = layout.parentOf(i);
         if (p < 0) {
            reachable[i] = true;
         } else {
            AzBone parent = layout.bone(p);
            reachable[i] = reachable[p] && !parent.isHidingChildren() && (parent.isHidden() || !collapsed(parent));
         }
      }

      return reachable;
   }

   private static boolean collapsed(AzBone bone) {
      return bone.getScaleX() == 0.0F && bone.getScaleY() == 0.0F && bone.getScaleZ() == 0.0F;
   }

   public enum Decline {
      NONE,
      SWITCHED_OFF,
      NO_SHADER,
      RE_RENDER,
      RENDER_TYPE,
      OUTLINE,
      TEXTURE_OVERRIDE,
      NO_MODEL,
      NO_PALETTE,
      NO_MESH,
      NO_LAYER;
   }

   private static final class Drawn {
      int frame = Integer.MIN_VALUE;
      GpuSkin.Drawn.Mode mode;
      SkinnedMesh mesh;
      RenderType batchType;
      int instance;
      final Matrix4f root = new Matrix4f();
      final Matrix3f rootNormal = new Matrix3f();

      enum Mode {
         INSTANCED,
         IMMEDIATE,
         FEEDBACK;
      }
   }
}

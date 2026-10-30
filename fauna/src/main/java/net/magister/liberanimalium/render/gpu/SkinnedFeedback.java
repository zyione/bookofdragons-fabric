package net.magister.liberanimalium.render.gpu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.nio.charset.StandardCharsets;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.magister.liberanimalium.compat.Meshes;
import net.magister.liberanimalium.mixin.ShaderInstanceAccessor;
import net.magister.liberanimalium.mixin.VertexBufferAccessor;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

public final class SkinnedFeedback {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final PerfClock.Section CLOCK_SKIN = PerfClock.client("gpu.feedback.skin");
   public static final PerfClock.Section CLOCK_DRAW = PerfClock.client("gpu.feedback.draw");
   private static int program;
   private static String failure;
   private static int uRootPose;
   private static int uRootNormal;
   private static int uVertexColor;
   private static int uLightUV;
   private static int uOverlayUV;
   private static int uEntityInfo;
   private static int uSampler3;
   private static final Map<SkinnedMesh, SkinnedFeedback.Output> OUTPUTS = new IdentityHashMap<>();
   private static final float[] MAT4 = new float[16];
   private static final float[] MAT3 = new float[9];

   private SkinnedFeedback() {
   }

   public static void compile(ResourceProvider provider) {
      release();
      failure = null;

      try {
         String source = read(provider, new ResourceLocation("liberanimalium", "shaders/core/fauna_feedback.vsh"));
         int shader = GL20.glCreateShader(35633);
         GL20.glShaderSource(shader, source);
         GL20.glCompileShader(shader);
         if (GL20.glGetShaderi(shader, 35713) == 0) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("fauna_feedback.vsh did not compile: " + log);
         }

         int prog = GL20.glCreateProgram();
         GL20.glAttachShader(prog, shader);
         List<String> names = SkinnedFormat.FORMAT.getElementAttributeNames();

         for (int i = 0; i < names.size(); i++) {
            GL20.glBindAttribLocation(prog, i, names.get(i));
         }

         GL30.glTransformFeedbackVaryings(prog, IrisEntityFormat.VARYINGS, 35980);
         GL20.glLinkProgram(prog);
         GL20.glDeleteShader(shader);
         if (GL20.glGetProgrami(prog, 35714) == 0) {
            String log = GL20.glGetProgramInfoLog(prog, 4096);
            GL20.glDeleteProgram(prog);
            throw new IllegalStateException("the feedback program did not link: " + log);
         }

         program = prog;
         uRootPose = GL20.glGetUniformLocation(prog, "RootPose");
         uRootNormal = GL20.glGetUniformLocation(prog, "RootNormal");
         uVertexColor = GL20.glGetUniformLocation(prog, "VertexColor");
         uLightUV = GL20.glGetUniformLocation(prog, "LightUV");
         uOverlayUV = GL20.glGetUniformLocation(prog, "OverlayUV");
         uEntityInfo = GL20.glGetUniformLocation(prog, "EntityInfo");
         uSampler3 = GL20.glGetUniformLocation(prog, "Sampler3");
      } catch (Exception e) {
         failure = String.valueOf(e.getMessage());
         program = 0;
         LOGGER.error("The feedback skinning program would not build; bodies draw on the CPU path under a shader pack. {}", failure);
      }
   }

   private static String read(ResourceProvider provider, ResourceLocation location) throws Exception {
      Optional<Resource> resource = provider.getResource(location);
      if (resource.isEmpty()) {
         throw new IllegalStateException(location + " is missing");
      }

      try (InputStream in = resource.get().open()) {
         return new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
   }

   public static String unavailableBecause() {
      if (failure != null) {
         return "feedback program: " + failure;
      }

      if (program == 0) {
         return "feedback program not built yet";
      }

      String iris = SkinnedFeedback.Iris.problem();
      return iris != null ? iris : null;
   }

   public static boolean available() {
      return unavailableBecause() == null;
   }

   private static SkinnedFeedback.Output outputFor(SkinnedMesh mesh) {
      SkinnedFeedback.Output out = OUTPUTS.get(mesh);
      if (out != null) {
         return out;
      }

      int vertices = mesh.quads() * 4;
      Meshes.Vertices zeros = new Meshes.Vertices(IrisEntityFormat.FORMAT, vertices);

      for (int v = 0; v < vertices; v++) {
         for (int e = 0; e < IrisEntityFormat.VARYINGS.length; e++) {
            zeros.nextElement();
         }

         zeros.nextVertex();
      }

      VertexBuffer buffer = new VertexBuffer(Usage.DYNAMIC);
      buffer.bind();
      Meshes.upload(buffer, Mode.QUADS, zeros);
      VertexBuffer.unbind();
      out = new SkinnedFeedback.Output(buffer, ((VertexBufferAccessor)buffer).liberanimalium$vertexBufferId(), vertices);
      OUTPUTS.put(mesh, out);
      return out;
   }

   public static void releaseOutputs() {
      for (SkinnedFeedback.Output out : OUTPUTS.values()) {
         out.close();
      }

      OUTPUTS.clear();
   }

   private static void release() {
      releaseOutputs();
      if (program != 0) {
         GL20.glDeleteProgram(program);
         program = 0;
      }
   }

   public static boolean draw(
      SkinnedMesh mesh, RenderType type, PoseStack root, int palette, int packedLight, int packedOverlay, float red, float green, float blue, float alpha
   ) {
      return draw(mesh, type, root.last().pose(), root.last().normal(), palette, packedLight, packedOverlay, red, green, blue, alpha);
   }

   public static boolean draw(
      SkinnedMesh mesh,
      RenderType type,
      Matrix4fc rootPose,
      Matrix3fc rootNormal,
      int palette,
      int packedLight,
      int packedOverlay,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      if (program == 0) {
         return false;
      }

      SkinnedFeedback.Output out = outputFor(mesh);
      long t0 = CLOCK_SKIN.start();

      try {
         int previousProgram = useProgram(program);
         rootPose.get(MAT4);
         GL20.glUniformMatrix4fv(uRootPose, false, MAT4);
         rootNormal.get(MAT3);
         GL20.glUniformMatrix3fv(uRootNormal, false, MAT3);
         GL20.glUniform4f(uVertexColor, red, green, blue, alpha);
         GL20.glUniform2i(uLightUV, packedLight & 65535, packedLight >> 16 & 65535);
         GL20.glUniform2i(uOverlayUV, packedOverlay & 65535, packedOverlay >> 16 & 65535);
         GL20.glUniform3i(uEntityInfo, SkinnedFeedback.Iris.entity(), SkinnedFeedback.Iris.blockEntity(), SkinnedFeedback.Iris.item());
         GL20.glUniform1i(uSampler3, 3);
         int previousUnit = GlStateManager._getActiveTexture();
         GlStateManager._activeTexture(33987);
         GlStateManager._bindTexture(palette);
         GlStateManager._activeTexture(previousUnit);
         mesh.buffer().bind();
         GL11.glEnable(35977);
         GL30.glBindBufferBase(35982, 0, out.vbo);
         GL30.glBeginTransformFeedback(0);
         GL11.glDrawArrays(0, 0, out.vertices);
         GL30.glEndTransformFeedback();
         GL30.glBindBufferBase(35982, 0, 0);
         GL11.glDisable(35977);
         VertexBuffer.unbind();
         releaseProgram(previousProgram);
      } finally {
         CLOCK_SKIN.end(t0);
      }

      long var24 = CLOCK_DRAW.start();

      try {
         type.setupRenderState();
         out.buffer.bind();
         out.buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
         VertexBuffer.unbind();
         type.clearRenderState();
      } finally {
         CLOCK_DRAW.end(var24);
      }

      return true;
   }

   static int useProgram(int program) {
      int previous = FaunaShaders.packActive() ? GL11.glGetInteger(35725) : 0;
      GL20.glUseProgram(program);
      return previous;
   }

   static void releaseProgram(int previous) {
      GL20.glUseProgram(previous);
      ShaderInstanceAccessor.liberanimalium$setLastProgramId(-1);
   }

   static final class Iris {
      private static boolean looked;
      private static String problem;
      private static MethodHandle entity;
      private static MethodHandle blockEntity;
      private static MethodHandle item;
      private static Object state;

      private static void look() {
         if (!looked) {
            looked = true;

            try {
               Class<?> formats = Class.forName("net.irisshaders.iris.vertices.IrisVertexFormats");
               VertexFormat theirs = (VertexFormat)formats.getField("ENTITY").get(null);
               List<String> names = theirs.getElementAttributeNames();
               if (!names.equals(IrisEntityFormat.IRIS_ENTITY_NAMES)) {
                  problem = "Iris's entity vertex is " + names + ", not the one this build twins";
                  return;
               }

               Class<?> captured = Class.forName("net.irisshaders.iris.uniforms.CapturedRenderingState");
               state = captured.getField("INSTANCE").get(null);
               Lookup lookup = MethodHandles.publicLookup();
               MethodType intOf = MethodType.methodType(int.class);
               entity = lookup.findVirtual(captured, "getCurrentRenderedEntity", intOf);
               blockEntity = lookup.findVirtual(captured, "getCurrentRenderedBlockEntity", intOf);
               item = lookup.findVirtual(captured, "getCurrentRenderedItem", intOf);
            } catch (Throwable t) {
               problem = "Iris/Oculus is not present or not the shape this build knows: " + t;
            }
         }
      }

      static String problem() {
         look();
         return problem;
      }

      static int entity() {
         return call(entity);
      }

      static int blockEntity() {
         return call(blockEntity);
      }

      static int item() {
         return call(item);
      }

      private static int call(MethodHandle handle) {
         if (handle == null) {
            return 0;
         }

         try {
            return (int)handle.invoke((Object)state);
         } catch (Throwable t) {
            return 0;
         }
      }
   }

   private static final class Output implements AutoCloseable {
      final VertexBuffer buffer;
      final int vbo;
      final int vertices;

      Output(VertexBuffer buffer, int vbo, int vertices) {
         this.buffer = buffer;
         this.vbo = vbo;
         this.vertices = vertices;
      }

      @Override
      public void close() {
         this.buffer.close();
      }
   }
}

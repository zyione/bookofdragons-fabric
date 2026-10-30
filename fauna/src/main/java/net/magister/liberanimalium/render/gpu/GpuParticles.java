package net.magister.liberanimalium.render.gpu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.magister.liberanimalium.compat.ClientFrame;
import net.magister.liberanimalium.compat.Meshes;
import net.magister.liberanimalium.compat.Verts;
import net.magister.liberanimalium.mixin.VertexBufferAccessor;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

public final class GpuParticles {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final PerfClock.Section CLOCK_TICK = PerfClock.client("gpu.particles.tick");
   public static final PerfClock.Section CLOCK_PACK = PerfClock.client("gpu.particles.pack");
   public static final PerfClock.Section CLOCK_DRAW = PerfClock.client("gpu.particles.draw");
   public static final PerfClock.Section CLOCK_FEEDBACK = PerfClock.client("gpu.particles.feedback");
   static final int RECORD_TEXELS = 6;
   private static final int RECORD_FLOATS = 24;
   private static final int MAX_PER_BATCH = 16384;
   private static final int IDLE_TICKS = 100;
   private static final long ANCHOR_LOST_MS = 1000L;
   private static final int FLAG_LEVEL = 1;
   private static final int FLAG_BACK = 2;
   private static final Map<ParticleRenderType, GpuParticles.Batch> BATCHES = new IdentityHashMap<>();
   private static final GpuParticles.Quad QUAD = new GpuParticles.Quad();
   private static final int RECORD_UNIT = 3;
   private static final float[] MAT3 = new float[9];
   private static final String[] VARYINGS = new String[]{"outPosition", "outUV0", "outColor", "outUV2"};
   private static int program;
   private static int vertexArray;
   private static int uRecords;
   private static int uOffset;
   private static int uBillboard;
   private static int uPartialTick;
   private static Field phaseField;
   private static boolean phaseLookupFailed;

   private GpuParticles() {
   }

   public static Particle adopt(GpuParticle particle) {
      ParticleRenderType type = particle.getRenderType();
      GpuParticles.Batch batch = BATCHES.computeIfAbsent(type, GpuParticles.Batch::new);
      batch.add(particle);
      return null;
   }

   public static int count() {
      int n = 0;

      for (GpuParticles.Batch batch : BATCHES.values()) {
         n += batch.particles.size();
      }

      return n;
   }

   public static void compile(ResourceProvider provider) {
      if (program != 0) {
         GL20.glDeleteProgram(program);
         program = 0;
      }

      try {
         String source = read(provider, new ResourceLocation("liberanimalium", "shaders/core/fauna_particle_feedback.vsh"));
         int shader = GL20.glCreateShader(35633);
         GL20.glShaderSource(shader, source);
         GL20.glCompileShader(shader);
         if (GL20.glGetShaderi(shader, 35713) == 0) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("fauna_particle_feedback.vsh did not compile: " + log);
         }

         int prog = GL20.glCreateProgram();
         GL20.glAttachShader(prog, shader);
         GL30.glTransformFeedbackVaryings(prog, VARYINGS, 35980);
         GL20.glLinkProgram(prog);
         GL20.glDeleteShader(shader);
         if (GL20.glGetProgrami(prog, 35714) == 0) {
            String log = GL20.glGetProgramInfoLog(prog, 4096);
            GL20.glDeleteProgram(prog);
            throw new IllegalStateException("the particle feedback program did not link: " + log);
         }

         program = prog;
         uRecords = GL20.glGetUniformLocation(prog, "Records");
         uOffset = GL20.glGetUniformLocation(prog, "Offset");
         uBillboard = GL20.glGetUniformLocation(prog, "Billboard");
         uPartialTick = GL20.glGetUniformLocation(prog, "PartialTick");
      } catch (Exception e) {
         program = 0;
         LOGGER.error("The particle feedback program would not build; particles draw on the CPU. {}", e.getMessage());
      }
   }

   public static boolean available() {
      return program != 0;
   }

   private static int emptyArray() {
      if (vertexArray == 0) {
         vertexArray = GL30.glGenVertexArrays();
      }

      return vertexArray;
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

   private static boolean isVanilla(ParticleRenderType type) {
      return type == ParticleRenderType.TERRAIN_SHEET
         || type == ParticleRenderType.PARTICLE_SHEET_OPAQUE
         || type == ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
         || type == ParticleRenderType.PARTICLE_SHEET_LIT
         || type == ParticleRenderType.CUSTOM
         || type == ParticleRenderType.NO_RENDER;
   }

   public static boolean irisOpaquePhase() {
      if (phaseLookupFailed) {
         return false;
      }

      try {
         if (phaseField == null) {
            Field found = null;

            for (Field f : ParticleEngine.class.getDeclaredFields()) {
               if (f.getType().isEnum() && "ParticleRenderingPhase".equals(f.getType().getSimpleName())) {
                  found = f;
                  break;
               }
            }

            if (found == null) {
               throw new NoSuchFieldException("ParticleRenderingPhase");
            }

            found.setAccessible(true);
            phaseField = found;
         }

         return phaseField.get(Minecraft.getInstance().particleEngine) instanceof Enum<?> e && "OPAQUE".equals(e.name());
      } catch (ReflectiveOperationException | RuntimeException e) {
         phaseLookupFailed = true;
         return false;
      }
   }

   private static final class Anchor extends Particle {
      private final GpuParticles.Batch batch;

      Anchor(ClientLevel level, GpuParticles.Batch batch) {
         super(level, 0.0, 0.0, 0.0);
         this.batch = batch;
         this.hasPhysics = false;
      }

      ClientLevel level() {
         return this.level;
      }

      public void tick() {
         if (this.batch.anchor != this) {
            this.remove();
         } else {
            this.batch.tick();
         }
      }

      public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
         if (this.batch.anchor == this) {
            this.batch.draw(buffer, camera, partialTicks);
         }
      }

      public ParticleRenderType getRenderType() {
         return this.batch.type;
      }

      public boolean shouldCull() {
         return false;
      }
   }

   private static final class Batch {
      final ParticleRenderType type;
      final List<GpuParticle> particles = new ArrayList<>();
      GpuParticles.Anchor anchor;
      ClientLevel level;
      long anchorTicked;
      int idle;
      boolean dirty;
      ByteBuffer storage;
      FloatBuffer records;
      int count;
      double ox;
      double oy;
      double oz;
      int recordBuffer;
      int recordTexture;
      VertexBuffer output;
      int outputVbo;
      int outputQuads;

      Batch(ParticleRenderType type) {
         this.type = type;
      }

      void add(GpuParticle particle) {
         ClientLevel now = Minecraft.getInstance().level;
         if (now != this.level) {
            for (GpuParticle p : this.particles) {
               p.remove();
            }

            this.particles.clear();
            this.level = now;
         }

         if (this.particles.size() >= 16384) {
            this.particles.remove(0).remove();
         }

         this.particles.add(particle);
         this.dirty = true;
         if (this.anchor == null || !this.anchor.isAlive() || this.anchor.level() != now || Util.getMillis() - this.anchorTicked > 1000L) {
            this.anchor = new GpuParticles.Anchor(now, this);
            this.anchorTicked = Util.getMillis();
            Minecraft.getInstance().particleEngine.add(this.anchor);
         }
      }

      void tick() {
         long t0 = GpuParticles.CLOCK_TICK.start();

         try {
            this.anchorTicked = Util.getMillis();
            int born = this.particles.size();
            int kept = 0;

            for (int i = 0; i < born; i++) {
               GpuParticle p = this.particles.get(i);
               if (p.isAlive()) {
                  p.tick();
               }

               if (p.isAlive()) {
                  this.particles.set(kept++, p);
               }
            }

            for (int i = born; i < this.particles.size(); i++) {
               this.particles.set(kept++, this.particles.get(i));
            }

            this.particles.subList(kept, this.particles.size()).clear();
            this.dirty = true;
            if (!this.particles.isEmpty()) {
               this.idle = 0;
            } else if (++this.idle > 100) {
               this.anchor.remove();
               this.anchor = null;
            }
         } finally {
            GpuParticles.CLOCK_TICK.end(t0);
         }
      }

      void draw(VertexConsumer buffer, Camera camera, float partialTicks) {
         if (this.dirty) {
            this.pack(camera);
            this.dirty = false;
         }

         if (this.count != 0 && !this.sitsOut()) {
            long t0 = GpuParticles.CLOCK_DRAW.start();

            try {
               if (GpuParticles.program != 0) {
                  this.drawOnGpu(camera, partialTicks);
               } else {
                  this.drawOnCpu(buffer, camera, partialTicks);
               }
            } finally {
               GpuParticles.CLOCK_DRAW.end(t0);
            }
         }
      }

      private void pack(Camera camera) {
         long t0 = GpuParticles.CLOCK_PACK.start();

         try {
            Vec3 eye = camera.getPosition();
            this.ox = eye.x;
            this.oy = eye.y;
            this.oz = eye.z;
            this.count = 0;
            GpuParticles.QUAD.batch = this;

            for (GpuParticle p : this.particles) {
               if (p.isAlive()) {
                  p.write(GpuParticles.QUAD.reset());
               }
            }

            GpuParticles.QUAD.batch = null;
            if (GpuParticles.program != 0 && this.count > 0) {
               this.upload();
            }
         } finally {
            GpuParticles.CLOCK_PACK.end(t0);
         }
      }

      FloatBuffer room() {
         int need = (this.count + 1) * 24;
         if (this.records == null || this.records.capacity() < need) {
            int floats = Math.max(6144, Integer.highestOneBit(need - 1) * 2);
            int bytes = floats * 4;
            long address = this.storage == null
               ? MemoryUtil.getAllocator(false).malloc(bytes)
               : MemoryUtil.getAllocator(false).realloc(MemoryUtil.memAddress0(this.storage), bytes);
            if (address == 0L) {
               throw new OutOfMemoryError("Failed to allocate " + bytes + " bytes");
            }

            this.storage = MemoryUtil.memByteBuffer(address, bytes);
            this.records = this.storage.asFloatBuffer();
         }

         return this.records;
      }

      private void upload() {
         boolean fresh = this.recordBuffer == 0;
         if (fresh) {
            this.recordBuffer = GL15.glGenBuffers();
            this.recordTexture = GL11.glGenTextures();
         }

         this.records.limit(this.count * 24).position(0);
         GL15.glBindBuffer(35882, this.recordBuffer);
         GL15.glBufferData(35882, this.records, 35040);
         GL15.glBindBuffer(35882, 0);
         this.records.clear();
         if (fresh) {
            int previousUnit = GlStateManager._getActiveTexture();
            GlStateManager._activeTexture(33987);
            GL11.glBindTexture(35882, this.recordTexture);
            GL31.glTexBuffer(35882, 34836, this.recordBuffer);
            GL11.glBindTexture(35882, 0);
            GlStateManager._activeTexture(previousUnit);
         }
      }

      private void ensureOutput() {
         if (this.output == null || this.outputQuads < this.count) {
            if (this.output != null) {
               this.output.close();
            }

            int quads = Math.max(256, Integer.highestOneBit(Math.max(1, this.count - 1)) * 2);
            int vertices = quads * 4;
            Meshes.Vertices zeros = new Meshes.Vertices(DefaultVertexFormat.PARTICLE, vertices);
            int elements = DefaultVertexFormat.PARTICLE.getElements().size();

            for (int v = 0; v < vertices; v++) {
               for (int e = 0; e < elements; e++) {
                  zeros.nextElement();
               }

               zeros.nextVertex();
            }

            this.output = new VertexBuffer(Usage.DYNAMIC);
            this.output.bind();
            Meshes.upload(this.output, Mode.QUADS, zeros);
            VertexBuffer.unbind();
            this.outputVbo = ((VertexBufferAccessor)this.output).liberanimalium$vertexBufferId();
            this.outputQuads = quads;
         }
      }

      private void drawOnGpu(Camera camera, float partialTicks) {
         this.ensureOutput();
         Vec3 eye = camera.getPosition();
         long t0 = GpuParticles.CLOCK_FEEDBACK.start();
         int previousProgram = SkinnedFeedback.useProgram(GpuParticles.program);
         GL20.glUniform3f(GpuParticles.uOffset, (float)(this.ox - eye.x), (float)(this.oy - eye.y), (float)(this.oz - eye.z));
         new Matrix3f().set(ClientFrame.billboard(camera)).get(GpuParticles.MAT3);
         GL20.glUniformMatrix3fv(GpuParticles.uBillboard, false, GpuParticles.MAT3);
         GL20.glUniform1f(GpuParticles.uPartialTick, partialTicks);
         GL20.glUniform1i(GpuParticles.uRecords, 3);
         int previousUnit = GlStateManager._getActiveTexture();
         GlStateManager._activeTexture(33987);
         GL11.glBindTexture(35882, this.recordTexture);
         VertexBuffer.unbind();
         GL30.glBindVertexArray(GpuParticles.emptyArray());
         GL11.glEnable(35977);
         GL30.glBindBufferBase(35982, 0, this.outputVbo);
         GL30.glBeginTransformFeedback(0);
         GL11.glDrawArrays(0, 0, this.count * 4);
         GL30.glEndTransformFeedback();
         GL30.glBindBufferBase(35982, 0, 0);
         GL11.glDisable(35977);
         VertexBuffer.unbind();
         GL11.glBindTexture(35882, 0);
         GlStateManager._activeTexture(previousUnit);
         SkinnedFeedback.releaseProgram(previousProgram);
         GpuParticles.CLOCK_FEEDBACK.end(t0);
         this.output.bind();
         ((VertexBufferAccessor)this.output).liberanimalium$setIndexCount(this.count * 6);
         this.output.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
         VertexBuffer.unbind();
      }

      private void drawOnCpu(VertexConsumer buffer, Camera camera, float partialTicks) {
         Vec3 eye = camera.getPosition();
         float offX = (float)(this.ox - eye.x);
         float offY = (float)(this.oy - eye.y);
         float offZ = (float)(this.oz - eye.z);
         Matrix3f billboard = new Matrix3f().set(ClientFrame.billboard(camera));
         Vector3f corner = new Vector3f();
         float t = partialTicks;

         for (int r = 0; r < this.count; r++) {
            int at = r * 24;
            int flags = (int)this.records.get(at + 20);
            float extent = t < this.records.get(at + 21) ? 0.0F : this.records.get(at + 3) + (this.records.get(at + 7) - this.records.get(at + 3)) * t;
            float spin = this.records.get(at + 16) + (this.records.get(at + 17) - this.records.get(at + 16)) * t;
            float c = (float)Math.cos(spin);
            float s = (float)Math.sin(spin);
            float cx = offX + this.records.get(at) + (this.records.get(at + 4) - this.records.get(at)) * t;
            float cy = offY + this.records.get(at + 1) + (this.records.get(at + 5) - this.records.get(at + 1)) * t;
            float cz = offZ + this.records.get(at + 2) + (this.records.get(at + 6) - this.records.get(at + 2)) * t;
            int lightU = (int)this.records.get(at + 18);
            int lightV = (int)this.records.get(at + 19);

            for (int k = 0; k < 4; k++) {
               int i = (flags & 2) != 0 ? 3 - k : k;
               float lx = (i < 2 ? -1.0F : 1.0F) * extent;
               float ly = (i != 1 && i != 2 ? -1.0F : 1.0F) * extent;
               corner.set(lx * c - ly * s, lx * s + ly * c, 0.0F);
               if ((flags & 1) != 0) {
                  corner.set(corner.x, 0.0F, corner.y);
               } else {
                  corner.mul(billboard);
               }

               float u = i < 2 ? this.records.get(at + 12) : this.records.get(at + 14);
               float v = i != 1 && i != 2 ? this.records.get(at + 15) : this.records.get(at + 13);
               Verts.particle(
                  buffer,
                  cx + corner.x,
                  cy + corner.y,
                  cz + corner.z,
                  u,
                  v,
                  this.records.get(at + 8),
                  this.records.get(at + 9),
                  this.records.get(at + 10),
                  this.records.get(at + 11),
                  lightU,
                  lightV
               );
            }
         }
      }

      private boolean sitsOut() {
         return !GpuParticles.isVanilla(this.type) && GpuParticles.irisOpaquePhase();
      }
   }

   public static final class Quad {
      private GpuParticles.Batch batch;
      private double px;
      private double py;
      private double pz;
      private double x;
      private double y;
      private double z;
      private float size0;
      private float size1;
      private float spin0;
      private float spin1;
      private float r;
      private float g;
      private float b;
      private float a;
      private float u0;
      private float v0;
      private float u1;
      private float v1;
      private float showFrom;
      private int lightU;
      private int lightV;
      private boolean level;

      private GpuParticles.Quad reset() {
         this.spin0 = this.spin1 = 0.0F;
         this.r = this.g = this.b = this.a = 1.0F;
         this.u0 = this.v0 = 0.0F;
         this.u1 = this.v1 = 1.0F;
         this.lightU = this.lightV = 0;
         this.showFrom = 0.0F;
         this.level = false;
         return this;
      }

      public GpuParticles.Quad move(double px, double py, double pz, double x, double y, double z) {
         this.px = px;
         this.py = py;
         this.pz = pz;
         this.x = x;
         this.y = y;
         this.z = z;
         return this;
      }

      public GpuParticles.Quad size(float before, float now) {
         this.size0 = before;
         this.size1 = now;
         return this;
      }

      public GpuParticles.Quad spin(float before, float now) {
         this.spin0 = before;
         this.spin1 = now;
         return this;
      }

      public GpuParticles.Quad color(float r, float g, float b, float a) {
         this.r = r;
         this.g = g;
         this.b = b;
         this.a = a;
         return this;
      }

      public GpuParticles.Quad uv(float u0, float v0, float u1, float v1) {
         this.u0 = u0;
         this.v0 = v0;
         this.u1 = u1;
         this.v1 = v1;
         return this;
      }

      public GpuParticles.Quad light(int u, int v) {
         this.lightU = u;
         this.lightV = v;
         return this;
      }

      public GpuParticles.Quad level(boolean level) {
         this.level = level;
         return this;
      }

      public GpuParticles.Quad showFrom(float fraction) {
         this.showFrom = fraction;
         return this;
      }

      public void emit() {
         this.put(this.level ? 1 : 0);
      }

      public void emitBack() {
         this.put((this.level ? 1 : 0) | 2);
      }

      private void put(int flags) {
         GpuParticles.Batch to = this.batch;
         FloatBuffer f = to.room();
         int at = to.count * 24;
         f.put(at, (float)(this.px - to.ox)).put(at + 1, (float)(this.py - to.oy)).put(at + 2, (float)(this.pz - to.oz)).put(at + 3, this.size0);
         f.put(at + 4, (float)(this.x - to.ox)).put(at + 5, (float)(this.y - to.oy)).put(at + 6, (float)(this.z - to.oz)).put(at + 7, this.size1);
         f.put(at + 8, this.r).put(at + 9, this.g).put(at + 10, this.b).put(at + 11, this.a);
         f.put(at + 12, this.u0).put(at + 13, this.v0).put(at + 14, this.u1).put(at + 15, this.v1);
         f.put(at + 16, this.spin0).put(at + 17, this.spin1).put(at + 18, this.lightU).put(at + 19, this.lightV);
         f.put(at + 20, flags).put(at + 21, this.showFrom).put(at + 22, 0.0F).put(at + 23, 0.0F);
         to.count++;
      }
   }
}

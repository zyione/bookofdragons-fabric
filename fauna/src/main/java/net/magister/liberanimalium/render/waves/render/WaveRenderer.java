package net.magister.liberanimalium.render.waves.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.magister.liberanimalium.compat.ClientFrame;
import net.magister.liberanimalium.compat.Verts;
import net.magister.liberanimalium.render.waves.FaunaWaveManager;
import net.magister.liberanimalium.render.waves.simulation.WaveSimulationChunk;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class WaveRenderer {
   private static final float OPACITY = 0.95F;
   private static final float STILL = 0.02F;
   private static final int OVERLAY_U = 0;
   private static final int OVERLAY_V = 10;
   private static final int CELLS = 64;
   private static final int STRIDE = 66;
   private static final int SIDE = 65;
   private static final float CELL = 0.25F;
   private final Map<Long, WaveDynamicTexture> textureCache = new HashMap<>();
   private final float[] heights = new float[4356];
   private final float[] rises = new float[4356];
   private final float[] lifts = new float[4225];
   private final float[] block = new float[4];
   private final float[] sky = new float[4];
   private static final PerfClock.Section CLOCK = PerfClock.client("waves.draw");

   public void render(PoseStack poseStack, Camera camera) {
      FaunaWaveManager manager = FaunaWaveManager.getInstance();
      List<WaveSimulationChunk> chunks = manager.getVisibleChunks(camera);
      if (!chunks.isEmpty()) {
         long clock = CLOCK.start();

         try {
            this.draw(poseStack, camera, manager, chunks);
         } finally {
            CLOCK.end(clock);
         }
      }
   }

   private void draw(PoseStack poseStack, Camera camera, FaunaWaveManager manager, List<WaveSimulationChunk> chunks) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      TextureManager textureManager = mc.getTextureManager();
      Vec3 camPos = camera.getPosition();
      float partialTick = ClientFrame.partialTick();
      BufferSource buffers = mc.renderBuffers().bufferSource();
      MutableBlockPos samplePos = new MutableBlockPos();
      Matrix4f matrix = poseStack.last().pose();
      Matrix3f normal = poseStack.last().normal();

      for (WaveSimulationChunk chunk : chunks) {
         if (!chunk.isDormant || chunk.isDirty) {
            long key = FaunaWaveManager.key(chunk);
            if (chunk.dry && !this.textureCache.containsKey(key)) {
               chunk.isDirty = false;
            } else {
               if (level != null && (chunk.biomeWaterColor == 4159204 || (chunk.x + chunk.z) % 16 == 0)) {
                  samplePos.set((chunk.x << 4) + 8, chunk.y, (chunk.z << 4) + 8);
                  chunk.biomeWaterColor = BiomeColors.getAverageWaterColor(level, samplePos);
               }

               WaveDynamicTexture texture = this.textureCache.computeIfAbsent(key, k -> new WaveDynamicTexture(chunk.x, chunk.y, chunk.z, 64, textureManager));
               texture.update(chunk);
               if (!chunk.dry && !(chunk.peak < 0.02F)) {
                  double x = chunk.x * 16 - camPos.x;
                  double y = chunk.y + 0.89088887F - camPos.y;
                  double z = chunk.z * 16 - camPos.z;
                  int x0 = chunk.x << 4;
                  int z0 = chunk.z << 4;
                  this.lightAt(0, level, samplePos, x0, chunk.y, z0);
                  this.lightAt(1, level, samplePos, x0, chunk.y, z0 + 15);
                  this.lightAt(2, level, samplePos, x0 + 15, chunk.y, z0 + 15);
                  this.lightAt(3, level, samplePos, x0 + 15, chunk.y, z0);
                  chunk.smoothedInto(this.heights, partialTick);
                  chunk.risesInto(this.rises);
                  boolean[] wet = chunk.waterCells();
                  boolean raised = WaveRelief.corners(this.heights, this.rises, wet, this.lifts) > 0.012F;
                  RenderType type = texture.renderType();
                  VertexConsumer buffer = buffers.getBuffer(type);

                  for (int j = 1; j <= 64; j++) {
                     for (int i = 1; i <= 64; i++) {
                        if (wet == null || wet[j * 66 + i]) {
                           int corner = (j - 1) * 65 + (i - 1);
                           boolean lifted = raised
                              && (
                                 this.lifts[corner] > 0.012F
                                    || this.lifts[corner + 1] > 0.012F
                                    || this.lifts[corner + 65] > 0.012F
                                    || this.lifts[corner + 65 + 1] > 0.012F
                              );
                           if (!lifted) {
                              float height = chunk.getSmoothedHeight(i, j);
                              if (height < 0.04F && height > -0.04F) {
                                 continue;
                              }
                           }

                           this.vertex(buffer, matrix, normal, x, y, z, i - 1, j - 1, raised);
                           this.vertex(buffer, matrix, normal, x, y, z, i - 1, j, raised);
                           this.vertex(buffer, matrix, normal, x, y, z, i, j, raised);
                           this.vertex(buffer, matrix, normal, x, y, z, i, j - 1, raised);
                        }
                     }
                  }

                  buffers.endBatch(type);
               }
            }
         }
      }

      if (this.textureCache.size() > chunks.size() + 8) {
         this.textureCache.entrySet().removeIf(entry -> {
            if (!manager.hasChunk(entry.getKey())) {
               entry.getValue().close();
               return true;
            } else {
               return false;
            }
         });
      }
   }

   private void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal, double x, double y, double z, int i, int j, boolean raised) {
      float u = i / 64.0F;
      float v = j / 64.0F;
      float blockHere = (this.block[0] + (this.block[3] - this.block[0]) * u) * (1.0F - v) + (this.block[1] + (this.block[2] - this.block[1]) * u) * v;
      float skyHere = (this.sky[0] + (this.sky[3] - this.sky[0]) * u) * (1.0F - v) + (this.sky[1] + (this.sky[2] - this.sky[1]) * u) * v;
      float lift = 0.0F;
      float shade = 1.0F;
      float nx = 0.0F;
      float ny = 1.0F;
      float nz = 0.0F;
      if (raised) {
         lift = this.lifts[j * 65 + i];
         float dx = WaveRelief.slopeX(this.lifts, i, j);
         float dz = WaveRelief.slopeZ(this.lifts, i, j);
         shade = WaveRelief.shade(dx, dz);
         float length = (float)Math.sqrt(1.0F + dx * dx + dz * dz);
         nx = -dx / length;
         ny = 1.0F / length;
         nz = -dz / length;
      }

      Verts.entity(
         buffer,
         matrix,
         normal,
         (float)(x + i * 0.25F),
         (float)(y + lift),
         (float)(z + j * 0.25F),
         shade,
         shade,
         shade,
         0.95F,
         u,
         v,
         0,
         10,
         Math.round(blockHere * 16.0F),
         Math.round(skyHere * 16.0F),
         nx,
         ny,
         nz
      );
   }

   private void lightAt(int k, ClientLevel level, MutableBlockPos pos, int x, int surfaceY, int z) {
      this.block[k] = 0.0F;
      this.sky[k] = 15.0F;
      if (level != null) {
         int top = level.getMaxBuildHeight() - 1;

         for (int up = 1; up <= 4; up++) {
            pos.set(x, Math.min(top, surfaceY + up), z);
            if (!level.getBlockState(pos).canOcclude()) {
               int light = LevelRenderer.getLightColor(level, pos);
               this.block[k] = LightTexture.block(light);
               this.sky[k] = LightTexture.sky(light);
               return;
            }
         }
      }
   }

   public void cleanup() {
      for (WaveDynamicTexture tex : this.textureCache.values()) {
         tex.close();
      }

      this.textureCache.clear();
   }
}

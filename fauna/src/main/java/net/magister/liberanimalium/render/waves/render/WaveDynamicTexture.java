package net.magister.liberanimalium.render.waves.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.magister.liberanimalium.render.waves.simulation.WaveSimulationChunk;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public class WaveDynamicTexture implements AutoCloseable {
   private final DynamicTexture texture;
   private final NativeImage image;
   private final ResourceLocation resourceLocation;
   private RenderType renderType;
   private final int size;
   private final int[] chunkPalette = new int[4];
   private int lastBiomeColor = -1;

   public WaveDynamicTexture(int chunkX, int chunkY, int chunkZ, int size, TextureManager textureManager) {
      this.size = size;
      this.image = new NativeImage(size, size, false);
      this.texture = new DynamicTexture(this.image);
      String id = "fauna_water_" + chunkX + "_" + chunkY + "_" + chunkZ;
      this.resourceLocation = textureManager.register(id, this.texture);
   }

   public ResourceLocation getResourceLocation() {
      return this.resourceLocation;
   }

   public RenderType renderType() {
      if (this.renderType == null) {
         this.renderType = WaveRenderTypes.surface(this.resourceLocation);
      }

      return this.renderType;
   }

   private void updateChunkPalette(int biomeColor) {
      int r = biomeColor >> 16 & 0xFF;
      int g = biomeColor >> 8 & 0xFF;
      int b = biomeColor & 0xFF;
      this.chunkPalette[0] = 0;
      int trR = r * 7 / 10;
      int trG = g * 7 / 10;
      int trB = b * 8 / 10;
      int trA = 60;
      this.chunkPalette[1] = trA << 24 | trB << 16 | trG << 8 | trR;
      int swR = r + ((255 - r) * 115 >> 8);
      int swG = g + ((255 - g) * 115 >> 8);
      int swB = b + ((255 - b) * 115 >> 8);
      int swA = 165;
      this.chunkPalette[2] = swA << 24 | swB << 16 | swG << 8 | swR;
      int crR = r + ((255 - r) * 235 >> 8);
      int crG = g + ((255 - g) * 235 >> 8);
      int crB = b + ((255 - b) * 235 >> 8);
      int crA = 225;
      this.chunkPalette[3] = crA << 24 | crB << 16 | crG << 8 | crR;
      this.lastBiomeColor = biomeColor;
   }

   public void update(WaveSimulationChunk chunk) {
      if (chunk.isDirty) {
         if (chunk.needsTextureClear) {
            for (int z = 0; z < this.size; z++) {
               for (int x = 0; x < this.size; x++) {
                  this.image.setPixelRGBA(x, z, 0);
               }
            }

            this.texture.upload();
            chunk.needsTextureClear = false;
            chunk.isDirty = false;
         } else {
            if (chunk.biomeWaterColor != this.lastBiomeColor) {
               this.updateChunkPalette(chunk.biomeWaterColor);
            }

            for (int z = 0; z < this.size; z++) {
               for (int x = 0; x < this.size; x++) {
                  float h = chunk.getSmoothedHeight(x + 1, z + 1);
                  int color;
                  if (h >= 0.6F) {
                     color = this.chunkPalette[3];
                  } else if (h >= 0.04F) {
                     color = this.chunkPalette[2];
                  } else if (h <= -0.04F) {
                     color = this.chunkPalette[1];
                  } else {
                     color = 0;
                  }

                  if (color != 0 && !chunk.isWater(x + 1, z + 1)) {
                     color = 0;
                  }

                  this.image.setPixelRGBA(x, z, color);
               }
            }

            this.texture.upload();
            chunk.isDirty = false;
         }
      }
   }

   @Override
   public void close() {
      this.texture.close();
   }
}

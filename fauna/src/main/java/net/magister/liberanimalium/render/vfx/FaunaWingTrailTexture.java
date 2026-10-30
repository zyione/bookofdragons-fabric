package net.magister.liberanimalium.render.vfx;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public class FaunaWingTrailTexture implements AutoCloseable {
   private static FaunaWingTrailTexture instance;
   private final DynamicTexture texture;
   private final NativeImage image;
   private final ResourceLocation resourceLocation;
   private final int width = 64;
   private final int height = 16;
   private float time = 0.0F;

   private FaunaWingTrailTexture() {
      this.image = new NativeImage(64, 16, false);
      this.texture = new DynamicTexture(this.image);
      this.resourceLocation = Minecraft.getInstance().getTextureManager().register("fauna_wing_trail_dynamic", this.texture);
      this.updateTexture();
   }

   public static FaunaWingTrailTexture getInstance() {
      if (instance == null) {
         instance = new FaunaWingTrailTexture();
      }

      return instance;
   }

   public ResourceLocation getResourceLocation() {
      return this.resourceLocation;
   }

   public void tick() {
      this.time += 0.2F;
      this.updateTexture();
   }

   private void updateTexture() {
      for (int x = 0; x < 64; x++) {
         for (int y = 0; y < 16; y++) {
            float u = x / 64.0F;
            float v = y / 16.0F;
            float widthFade = 1.0F - 2.0F * Math.abs(v - 0.5F);
            widthFade *= widthFade;
            float noise = (float)Math.sin(u * 10.0F - this.time) * 0.5F + 0.5F;
            float noise2 = (float)Math.cos(u * 20.0F + v * 5.0F + this.time * 0.5F) * 0.3F;
            float intensity = widthFade * (0.5F + 0.4F * noise + 0.1F * noise2);
            intensity = Math.max(0.0F, Math.min(1.0F, intensity));
            int alpha = (int)(intensity * 255.0F);
            int r = 255;
            int g = 255;
            int b = 255;
            if (intensity < 0.5) {
               b = 255;
               g = 255;
               r = 255;
            }

            int color = alpha << 24 | r << 16 | g << 8 | b;
            this.image.setPixelRGBA(x, y, color);
         }
      }

      this.texture.upload();
   }

   @Override
   public void close() {
      this.texture.close();
   }
}

package net.magister.liberanimalium.render.gpu;

import net.magister.liberanimalium.compat.ClientFrame;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public abstract class GpuSheetParticle extends GpuParticle {
   protected float quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
   protected TextureAtlasSprite sprite;

   protected GpuSheetParticle(ClientLevel level, double x, double y, double z) {
      super(level, x, y, z);
   }

   protected GpuSheetParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
      super(level, x, y, z, xd, yd, zd);
   }

   public float getQuadSize(float partialTicks) {
      return this.quadSize;
   }

   public Particle scale(float scale) {
      this.quadSize *= scale;
      return super.scale(scale);
   }

   protected void setSprite(TextureAtlasSprite sprite) {
      this.sprite = sprite;
   }

   public void pickSprite(SpriteSet sprites) {
      this.setSprite(sprites.get(this.random));
   }

   public void setSpriteFromAge(SpriteSet sprites) {
      if (!this.removed) {
         this.setSprite(sprites.get(this.age, this.lifetime));
      }
   }

   @Override
   protected void write(GpuParticles.Quad quad) {
      if (this.sprite != null) {
         int light = this.getLightColor(1.0F);
         quad.move(this.xo, this.yo, this.zo, this.x, this.y, this.z)
            .size(this.getQuadSize(0.0F), this.getQuadSize(1.0F))
            .spin(ClientFrame.billboardRoll(this.oRoll), ClientFrame.billboardRoll(this.roll))
            .color(this.rCol, this.gCol, this.bCol, this.alpha)
            .uv(this.sprite.getU1(), this.sprite.getV0(), this.sprite.getU0(), this.sprite.getV1())
            .light(light & 65535, light >> 16 & 65535)
            .emit();
      }
   }
}

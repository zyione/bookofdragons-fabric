package net.magister.liberanimalium.render.vfx;

import net.magister.liberanimalium.render.gpu.GpuParticles;
import net.magister.liberanimalium.render.gpu.GpuSheetParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;

public class WaterSprayParticle extends GpuSheetParticle {
   private static final float ALPHA = 0.8F;
   private static final float FADE_FROM = 0.7F;

   protected WaterSprayParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
      super(level, x, y, z, 0.0, 0.0, 0.0);
      this.xd = xd;
      this.yd = yd;
      this.zd = zd;
      this.friction = 0.98F;
      this.gravity = 0.9F;
      this.hasPhysics = true;
      float r = this.random.nextFloat();
      this.quadSize = 0.05F + r * r * 0.2F;
      this.lifetime = 24 + this.random.nextInt(14) + (int)(this.quadSize * 60.0F);
      this.alpha = 0.8F;
      this.pickSprite(sprites);
   }

   public void tick() {
      super.tick();
      if (!this.removed) {
         if (this.onGround) {
            this.remove();
         } else {
            if (this.yd < 0.0) {
               BlockPos at = BlockPos.containing(this.x, this.y, this.z);
               FluidState fluid = this.level.getFluidState(at);
               if (fluid.is(FluidTags.WATER) && this.y < at.getY() + fluid.getHeight(this.level, at)) {
                  this.remove();
                  return;
               }
            }

            float life = (float)this.age / this.lifetime;
            this.alpha = life < 0.7F ? 0.8F : 0.8F * (1.0F - life) / 0.3F;
         }
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
         return GpuParticles.adopt(new WaterSprayParticle(level, x, y, z, xd, yd, zd, this.sprites));
      }
   }
}

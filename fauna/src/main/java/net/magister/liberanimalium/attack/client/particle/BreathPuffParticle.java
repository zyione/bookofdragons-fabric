package net.magister.liberanimalium.attack.client.particle;

import net.magister.liberanimalium.render.gpu.GpuParticles;
import net.magister.liberanimalium.render.gpu.GpuSheetParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class BreathPuffParticle extends GpuSheetParticle {
   public static final float ALPHA = 0.32F;
   private final SpriteSet sprites;

   protected BreathPuffParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
      super(level, x, y, z, 0.0, 0.0, 0.0);
      this.sprites = sprites;
      this.xd = xd;
      this.yd = yd;
      this.zd = zd;
      this.friction = 0.94F;
      this.gravity = -0.004F;
      this.hasPhysics = false;
      this.quadSize = 0.09F + this.random.nextFloat() * 0.06F;
      this.lifetime = 18 + this.random.nextInt(8);
      this.rCol = 0.92F;
      this.gCol = 0.95F;
      this.bCol = 1.0F;
      this.alpha = 0.32F;
      this.setSpriteFromAge(sprites);
   }

   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
      float life = (float)this.age / this.lifetime;
      this.alpha = 0.32F * (1.0F - life);
      this.quadSize += 0.00375F;
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
         return GpuParticles.adopt(new BreathPuffParticle(level, x, y, z, xd, yd, zd, this.sprites));
      }
   }
}

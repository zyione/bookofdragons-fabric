package net.magister.bookofdragons.client.particle;

import net.magister.liberanimalium.render.gpu.GpuParticles;
import net.magister.liberanimalium.render.gpu.GpuSheetParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class FuryTrailParticle extends GpuSheetParticle {
   private final SpriteSet sprites;

   protected FuryTrailParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed, SpriteSet pSprites) {
      super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
      this.lifetime = 10 + this.random.nextInt(5);
      this.quadSize = 0.8F;
      this.sprites = pSprites;
      this.setSpriteFromAge(pSprites);
      this.friction = 0.9F;
   }

   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet spriteSet) {
         this.sprites = spriteSet;
      }

      public Particle createParticle(SimpleParticleType particleType, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
         return GpuParticles.adopt(new FuryTrailParticle(level, x, y, z, dx, dy, dz, this.sprites));
      }
   }
}

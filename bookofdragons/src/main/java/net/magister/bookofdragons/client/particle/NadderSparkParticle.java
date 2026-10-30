package net.magister.bookofdragons.client.particle;

import net.magister.liberanimalium.render.gpu.GpuParticles;
import net.magister.liberanimalium.render.gpu.GpuSheetParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class NadderSparkParticle extends GpuSheetParticle {
   private final SpriteSet sprites;

   protected NadderSparkParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed, SpriteSet pSprites) {
      super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
      this.sprites = pSprites;
      this.friction = 0.8F;
      this.xd = pXSpeed;
      this.yd = pYSpeed;
      this.zd = pZSpeed;
      this.quadSize *= 1.3F;
      this.lifetime = 8 + this.random.nextInt(5);
      this.setSpriteFromAge(pSprites);
      this.rCol = 1.0F;
      this.gCol = 0.8F + this.random.nextFloat() * 0.2F;
      this.bCol = 0.2F + this.random.nextFloat() * 0.3F;
      this.alpha = 1.0F;
   }

   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
      this.alpha = 1.0F - (float)this.age / this.lifetime;
      this.yd -= 0.001;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet pSprites) {
         this.sprites = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         return GpuParticles.adopt(new NadderSparkParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, this.sprites));
      }
   }
}

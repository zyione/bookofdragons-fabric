package net.magister.bookofdragons.client.particle;

import net.magister.liberanimalium.render.gpu.GpuParticles;
import net.magister.liberanimalium.render.gpu.GpuSheetParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class FireballParticle extends GpuSheetParticle {
   private final SpriteSet sprites;

   FireballParticle(ClientLevel world, double x, double y, double z, SpriteSet spriteProvider, double power) {
      super(world, x, y, z, 0.0, 0.0, 0.0);
      this.sprites = spriteProvider;
      this.lifetime = 9;
      this.quadSize = (float)power;
      this.setSpriteFromAge(spriteProvider);
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.setSpriteFromAge(this.sprites);
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_LIT;
   }

   protected int getLightColor(float pPartialTick) {
      return 15728880;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet spriteSet) {
         this.sprites = spriteSet;
      }

      public Particle createParticle(SimpleParticleType particleType, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
         return GpuParticles.adopt(new FireballParticle(level, x, y, z, this.sprites, dx));
      }
   }
}

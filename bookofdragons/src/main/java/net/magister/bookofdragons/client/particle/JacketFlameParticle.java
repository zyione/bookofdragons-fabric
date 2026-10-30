package net.magister.bookofdragons.client.particle;

import net.magister.liberanimalium.render.gpu.GpuParticles;
import net.magister.liberanimalium.render.gpu.GpuSheetParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class JacketFlameParticle extends GpuSheetParticle {
   private static final int ALL_FRAMES = 8;
   private static final int FIRE_FRAMES = 6;
   private static final int SOOT_FROM = 5;
   private static final int FULL_BRIGHT = 15728880;
   private final SpriteSet sprites;
   private final int frames;
   private final float spin;

   JacketFlameParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
      super(level, x, y, z);
      this.sprites = sprites;
      this.lifetime = 12 + this.random.nextInt(7);
      this.quadSize = (9 + this.random.nextInt(7)) / 32.0F;
      this.frames = this.random.nextFloat() < 0.2F ? 8 : 6;
      this.xd = (this.random.nextDouble() - 0.5) * 0.0125;
      this.zd = (this.random.nextDouble() - 0.5) * 0.0125;
      this.yd = 0.03 + this.random.nextDouble() * 0.025;
      this.friction = 0.96F;
      this.gravity = 0.0F;
      this.hasPhysics = false;
      this.roll = this.random.nextFloat() * (float) (Math.PI * 2);
      this.oRoll = this.roll;
      this.spin = (this.random.nextFloat() - 0.5F) * 0.4F;
      this.setSprite(sprites.get(0, 7));
   }

   private int frame() {
      return Math.min(this.frames - 1, this.age * this.frames / Math.max(1, this.lifetime));
   }

   public void tick() {
      super.tick();
      if (!this.removed) {
         this.oRoll = this.roll;
         this.roll = this.roll + this.spin;
         this.setSprite(this.sprites.get(this.frame(), 7));
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   protected int getLightColor(float partialTick) {
      return this.frame() < 5 ? 15728880 : super.getLightColor(partialTick);
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
         return GpuParticles.adopt(new JacketFlameParticle(level, x, y, z, this.sprites));
      }
   }
}

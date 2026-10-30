package net.magister.liberanimalium.render.vfx;

import org.jetbrains.annotations.Nullable;
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
import net.minecraft.util.Mth;

public class SleepBubbleParticle extends GpuSheetParticle {
   public static final float ALPHA = 0.6F;
   private static final float FADE_FROM = 0.65F;
   public static final float SWIM_SIZE = 1.4F;
   private static final float SWIM_FRICTION = 0.9F;
   private static final float SWIM_BUOYANCY = -0.12F;
   @Nullable
   private static SpriteSet shared;
   private final float startSize;
   private final float endSize;
   private final float wobblePhase;
   private final boolean inWater;

   protected SleepBubbleParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
      this(level, x, y, z, xd, yd, zd, sprites, false);
   }

   protected SleepBubbleParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, boolean inWater) {
      super(level, x, y, z, 0.0, 0.0, 0.0);
      this.xd = xd;
      this.yd = yd;
      this.zd = zd;
      this.inWater = inWater;
      this.friction = inWater ? 0.9F : 0.95F;
      this.gravity = inWater ? -0.12F : -0.02F;
      this.hasPhysics = false;
      float r = this.random.nextFloat();
      float size = inWater ? 1.4F : 1.0F;
      this.endSize = size * (this.random.nextFloat() < 0.1F ? 0.16F + r * 0.06F : 0.04F + r * r * 0.1F);
      this.startSize = size * (0.012F + this.random.nextFloat() * 0.008F);
      this.lifetime = 28 + this.random.nextInt(16) + (int)(this.endSize * 120.0F);
      this.quadSize = this.startSize;
      this.wobblePhase = this.random.nextFloat() * (float) (Math.PI * 2);
      this.alpha = 0.6F;
      this.pickSprite(sprites);
   }

   public void tick() {
      super.tick();
      if (!this.removed) {
         if (this.inWater && !this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER)) {
            this.remove();
         } else {
            float life = (float)this.age / this.lifetime;
            float swell = 1.0F - (1.0F - life) * (1.0F - life);
            this.quadSize = Mth.lerp(swell, this.startSize, this.endSize);
            this.alpha = life < 0.65F ? 0.6F : 0.6F * (1.0F - life) / 0.35000002F;
            this.xd = this.xd + Mth.sin(this.age * 0.3F + this.wobblePhase) * 8.0E-4F;
         }
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   @Nullable
   public static Particle underWater(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
      return shared == null ? null : new SleepBubbleParticle(level, x, y, z, xd, yd, zd, shared, true);
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
         SleepBubbleParticle.shared = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
         return GpuParticles.adopt(new SleepBubbleParticle(level, x, y, z, xd, yd, zd, this.sprites));
      }
   }
}

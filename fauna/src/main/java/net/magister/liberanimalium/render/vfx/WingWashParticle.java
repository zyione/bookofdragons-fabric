package net.magister.liberanimalium.render.vfx;

import net.magister.liberanimalium.render.gpu.GpuParticles;
import net.magister.liberanimalium.render.gpu.GpuSheetParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public class WingWashParticle extends GpuSheetParticle {
   private static final float DUST_LIFT = 0.35F;
   private static final float DEBRIS_LIFT = 0.15F;
   private static final double WALL_REACH = 0.75;
   private static final int GRASS_FALLBACK = 9551193;
   private final SpriteSet sprites;
   private final boolean dust;
   private final int spriteBase;
   private final int variant;
   private final boolean leaf;
   private final boolean sways;
   private final float alphaAtBirth;
   private float spin;
   private final float swayPhase;
   private final float swayX;
   private final float swayZ;

   protected WingWashParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, boolean dust) {
      super(level, x, y, z, 0.0, 0.0, 0.0);
      this.sprites = sprites;
      this.dust = dust;
      this.xd = xd;
      this.yd = yd;
      this.zd = zd;
      BlockPos under = BlockPos.containing(x, y - (dust ? 0.5 : 0.25), z);
      if (level.getBlockState(under).isAir()) {
         for (Direction side : Plane.HORIZONTAL) {
            BlockPos beside = BlockPos.containing(x + side.getStepX() * 0.75, y, z + side.getStepZ() * 0.75);
            if (!level.getBlockState(beside).isAir()) {
               under = beside;
               break;
            }
         }
      }

      BlockState state = level.getBlockState(under);
      WingWashGround ground = WingWashGround.of(level, under);
      if (dust) {
         this.spriteBase = 0;
         this.variant = 0;
         this.leaf = false;
         this.sways = false;
         this.gravity = -0.1F;
         this.friction = 0.88F;
         this.hasPhysics = false;
         this.lifetime = 20 + this.random.nextInt(9);
         this.quadSize = 0.5F * (0.85F + this.random.nextFloat() * 0.3F);
         this.alphaAtBirth = ground.dustAlpha > 0.0F ? ground.dustAlpha : 0.6F;
         boolean soil = ground == WingWashGround.GRASS || ground == WingWashGround.SOIL;
         this.tint(
            lift(
               ground == WingWashGround.SNOW ? MapColor.SNOW.col : (soil ? MapColor.DIRT.col : colourOf(state.getMapColor(level, under))),
               0.35F
            )
         );
         this.setSpriteFromAge(sprites);
      } else {
         WingWashGround kind = ground.debrisSprites < 0 ? WingWashGround.STONE : ground;
         this.spriteBase = kind.debrisSprites;
         this.variant = this.random.nextInt(6);
         this.leaf = kind == WingWashGround.LEAVES;
         boolean cone = kind == WingWashGround.PINE && this.variant == 0;
         this.sways = this.leaf || kind == WingWashGround.SNOW || kind == WingWashGround.PINE && !cone;
         this.hasPhysics = true;
         this.quadSize = 0.25F;
         this.alphaAtBirth = 1.0F;
         this.roll = this.random.nextFloat() * (float) (Math.PI * 2);
         this.oRoll = this.roll;
         float spinMin;
         float spinSpan;
         switch (kind) {
            case GRASS: {
               this.gravity = 0.55F;
               this.friction = 0.94F;
               spinMin = 0.15F;
               spinSpan = 0.25F;
               this.lifetime = 20 + this.random.nextInt(10);
               int c = this.blockTint(state, under);
               this.tint(c == -1 ? 9551193 : c);
               break;
            }
            case SAND:
               this.gravity = 0.7F;
               this.friction = 0.95F;
               spinMin = 0.05F;
               spinSpan = 0.25F;
               this.lifetime = 12 + this.random.nextInt(8);
               this.tint(lift(state.getMapColor(level, under).col, 0.15F));
               break;
            case LEAVES: {
               this.gravity = 0.22F;
               this.friction = 0.94F;
               spinMin = 0.08F;
               spinSpan = 0.12F;
               this.lifetime = 40 + this.random.nextInt(20);
               int c = this.blockTint(state, under);
               this.tint(c == -1 ? state.getMapColor(level, under).col : c);
               break;
            }
            case PINE:
               this.lifetime = 35 + this.random.nextInt(20);
               if (cone) {
                  this.gravity = 0.8F;
                  this.friction = 0.96F;
                  spinMin = 0.15F;
                  spinSpan = 0.25F;
               } else {
                  this.gravity = 0.3F;
                  this.friction = 0.94F;
                  spinMin = 0.1F;
                  spinSpan = 0.2F;
                  int c = this.blockTint(state, under);
                  this.tint(c == -1 ? state.getMapColor(level, under).col : c);
               }
               break;
            case SNOW:
               this.gravity = 0.3F;
               this.friction = 0.93F;
               spinMin = 0.05F;
               spinSpan = 0.15F;
               this.lifetime = 25 + this.random.nextInt(16);
               break;
            default:
               this.gravity = 1.0F;
               this.friction = 0.96F;
               spinMin = 0.2F;
               spinSpan = 0.3F;
               this.lifetime = 22 + this.random.nextInt(10);
               this.tint(lift(state.getMapColor(level, under).col, 0.15F));
         }

         this.spin = (spinMin + this.random.nextFloat() * spinSpan) * (this.random.nextBoolean() ? 1 : -1);
         this.setSprite(sprites.get(this.spriteBase + this.variant, 35));
      }

      float sway = this.random.nextFloat() * (float) (Math.PI * 2);
      this.swayPhase = this.random.nextFloat() * (float) (Math.PI * 2);
      this.swayX = Mth.cos(sway);
      this.swayZ = Mth.sin(sway);
      this.alpha = this.alphaAtBirth;
   }

   public void tick() {
      super.tick();
      if (!this.removed) {
         float life = (float)this.age / this.lifetime;
         if (this.dust) {
            this.setSpriteFromAge(this.sprites);
            this.alpha = this.alphaAtBirth * (life < 0.6F ? 1.0F : (1.0F - life) / 0.4F);
         } else {
            this.oRoll = this.roll;
            this.roll = this.roll + this.spin;
            this.spin = this.spin * (this.onGround ? 0.5F : 0.98F);
            if (this.sways && !this.onGround) {
               float swing = Mth.sin(this.age * 0.35F + this.swayPhase) * 0.01F;
               this.xd = this.xd + swing * this.swayX;
               this.zd = this.zd + swing * this.swayZ;
            }

            if (this.leaf) {
               this.setSprite(this.sprites.get(this.spriteBase + (this.age / 3 + this.variant) % 6, 35));
            }

            float fadeFrom = this.sways ? 0.8F : 0.75F;
            this.alpha = life < fadeFrom ? 1.0F : (1.0F - life) / (1.0F - fadeFrom);
         }
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   private int blockTint(BlockState state, BlockPos pos) {
      return Minecraft.getInstance().getBlockColors().getColor(state, this.level, pos, 0);
   }

   private void tint(int rgb) {
      this.rCol = (rgb >> 16 & 0xFF) / 255.0F;
      this.gCol = (rgb >> 8 & 0xFF) / 255.0F;
      this.bCol = (rgb & 0xFF) / 255.0F;
   }

   private static int colourOf(MapColor colour) {
      return colour == MapColor.NONE ? MapColor.STONE.col : colour.col;
   }

   private static int lift(int rgb, float toward) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      r += Math.round((255 - r) * toward);
      g += Math.round((255 - g) * toward);
      b += Math.round((255 - b) * toward);
      return r << 16 | g << 8 | b;
   }

   public static class DebrisProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public DebrisProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
         return GpuParticles.adopt(new WingWashParticle(level, x, y, z, xd, yd, zd, this.sprites, false));
      }
   }

   public static class DustProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public DustProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
         return GpuParticles.adopt(new WingWashParticle(level, x, y, z, xd, yd, zd, this.sprites, true));
      }
   }
}

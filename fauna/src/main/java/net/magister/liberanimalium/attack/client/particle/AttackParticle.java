package net.magister.liberanimalium.attack.client.particle;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.attack.AttackManager;
import net.magister.liberanimalium.attack.data.AttackDefinition;
import net.magister.liberanimalium.attack.data.EmitterConfig;
import net.magister.liberanimalium.attack.data.VisualEmitter;
import net.magister.liberanimalium.attack.data.impact.ExtinguishImpact;
import net.magister.liberanimalium.attack.data.render.BillboardRender;
import net.magister.liberanimalium.attack.data.render.PixelQuadRender;
import net.magister.liberanimalium.attack.sim.BreathParticle;
import net.magister.liberanimalium.attack.sim.BreathStepper;
import net.magister.liberanimalium.render.gpu.FaunaShaders;
import net.magister.liberanimalium.render.gpu.GpuParticle;
import net.magister.liberanimalium.render.gpu.GpuParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;

public class AttackParticle extends GpuParticle {
   private static final ResourceLocation TEXTURE = new ResourceLocation("liberanimalium", "textures/particle/white_pixel.png");
   private final BreathParticle state;
   private final boolean additive;
   @Nullable
   private final BillboardRender sprite;
   private final boolean collides;
   private final ResourceLocation attackId;
   private final boolean emissive;
   private final float shaderAlpha;
   private static final Map<ResourceLocation, Set<AttackParticle>> LIVE = new HashMap<>();
   private static int births;
   private static final int SWEEP_EVERY = 512;
   private final boolean sourceAnchored;
   private final boolean projectileAnchored;
   private final boolean hasExtinguish;

   public static void clearNear(ResourceLocation attackId, double x, double y, double z, float radius) {
      Set<AttackParticle> live = LIVE.get(attackId);
      if (live != null) {
         double r2 = (double)radius * radius;

         for (AttackParticle p : new ArrayList<>(live)) {
            double dx = p.x - x;
            double dy = p.y - y;
            double dz = p.z - z;
            if (!p.isAlive() || dx * dx + dy * dy + dz * dz <= r2) {
               p.remove();
            }
         }
      }
   }

   public void remove() {
      super.remove();
      Set<AttackParticle> live = LIVE.get(this.attackId);
      if (live != null && live.remove(this) && live.isEmpty()) {
         LIVE.remove(this.attackId);
      }
   }

   protected AttackParticle(
      ClientLevel level,
      double x,
      double y,
      double z,
      double xSpeed,
      double ySpeed,
      double zSpeed,
      VisualEmitter emitter,
      int emitterIndex,
      boolean hasExtinguish,
      float scale,
      long seed,
      ResourceLocation attackId
   ) {
      super(level, x, y, z);
      this.attackId = attackId;
      if (++births % 512 == 0) {
         LIVE.values().forEach(set -> set.removeIf(q -> !q.isAlive()));
      }

      LIVE.computeIfAbsent(attackId, k -> new HashSet<>()).add(this);
      this.state = BreathStepper.begin(emitter, x, y, z, xSpeed, ySpeed, zSpeed, scale, emitterIndex, seed);
      this.xd = this.state.vx;
      this.yd = this.state.vy;
      this.zd = this.state.vz;
      this.sourceAnchored = emitter.isSourceAnchored();
      this.projectileAnchored = emitter.isProjectileAnchored();
      this.hasExtinguish = hasExtinguish;
      this.lifetime = this.state.life;
      this.collides = emitter.sim().collide();
      this.hasPhysics = this.collides;
      this.gravity = 0.0F;
      this.sprite = emitter.render() instanceof BillboardRender b ? b : null;
      this.additive = this.sprite != null
         ? "additive".equals(this.sprite.blend())
         : !(emitter.render() instanceof PixelQuadRender quad && !"additive".equals(quad.blend()));
      this.emissive = !(emitter.render() instanceof PixelQuadRender quad && !quad.emissive());
      this.shaderAlpha = emitter.render() instanceof PixelQuadRender quad ? quad.shaderAlpha() : (this.sprite != null ? this.sprite.shaderAlpha() : 1.0F);
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.state.px = this.xo;
      this.state.py = this.yo;
      this.state.pz = this.zo;
      this.state.x = this.x;
      this.state.y = this.y;
      this.state.z = this.z;
      if (!BreathStepper.tick(this.state)) {
         this.remove();
      } else {
         this.age = this.state.age;
         this.xd = this.state.vx * this.state.activeFraction;
         this.yd = this.state.vy * this.state.activeFraction;
         this.zd = this.state.vz * this.state.activeFraction;
         this.move(this.xd, this.yd, this.zd);
         this.state.x = this.x;
         this.state.y = this.y;
         this.state.z = this.z;
         if (!this.checkImpact()) {
            BreathStepper.settle(this.state);
         }
      }
   }

   private boolean checkImpact() {
      if (!this.sourceAnchored && !this.projectileAnchored) {
         return false;
      } else {
         BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
         BlockState blockState = this.level.getBlockState(pos);
         if (!this.collides && !blockState.getCollisionShape(this.level, pos).isEmpty()) {
            this.remove();
            return true;
         } else if (this.sourceAnchored && this.hasExtinguish && this.level.getFluidState(pos).is(FluidTags.WATER)) {
            this.level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.x, this.y + 0.05, this.z, 0.0, 0.03, 0.0);
            this.remove();
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   protected void write(GpuParticles.Quad quad) {
      if (!(this.state.delay >= 1.0F)) {
         BreathStepper.BreathQuad q = BreathStepper.quad(this.state, 1.0F);
         if (!(q.size() <= 0.0F) && !(q.a() <= 0.0F)) {
            BreathStepper.BreathSpan span = BreathStepper.span(this.state);
            int light = this.getLightColor(1.0F);
            boolean lit = this.sprite != null ? this.sprite.litAt(this.state.lifeFraction()) : this.emissive;
            int uLight = lit ? 240 : light & 65535;
            int vLight = light >> 16 & 65535;
            float u0 = 0.0F;
            float u1 = 1.0F;
            float v0 = 0.0F;
            float v1 = 1.0F;
            if (this.sprite != null) {
               TextureAtlasSprite cell = AttackParticle.Sheet.sprite(this.sprite.frame(q.frame()));
               if (cell != null) {
                  u0 = cell.getU0();
                  u1 = cell.getU1();
                  v0 = cell.getV0();
                  v1 = cell.getV1();
               }
            }

            float a = q.a() * (FaunaShaders.packActive() ? this.shaderAlpha : 1.0F);
            boolean flat = this.sprite != null && this.sprite.flat();
            quad.move(span.px(), span.py(), span.pz(), span.x(), span.y(), span.z())
               .size(q.size() / 2.0F, q.size() / 2.0F)
               .spin(span.spin0(), span.spin1())
               .color(q.r(), q.g(), q.b(), a)
               .uv(u0, v0, u1, v1)
               .light(uLight, vLight)
               .level(flat)
               .showFrom(span.showFrom())
               .emit();
            if (flat) {
               quad.emitBack();
            }
         }
      }
   }

   public ParticleRenderType getRenderType() {
      if (this.sprite != null) {
         return this.additive ? AttackParticle.RenderTypes.SHEET_ADDITIVE : AttackParticle.RenderTypes.SHEET_ALPHA;
      } else {
         return this.additive ? AttackParticle.RenderTypes.ADDITIVE : AttackParticle.RenderTypes.ALPHA;
      }
   }

   public static class Provider implements ParticleProvider<AttackParticleOptions> {
      public Provider(SpriteSet spriteSet) {
      }

      public Particle createParticle(
         AttackParticleOptions options, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
      ) {
         AttackDefinition def = AttackManager.getInstance().getAttack(options.getAttackId());
         if (def == null) {
            return null;
         }

         List<EmitterConfig> emitters = def.emitters();
         int index = options.getEmitterIndex();
         if (index >= 0 && index < emitters.size()) {
            if (emitters.get(index) instanceof VisualEmitter visual) {
               boolean var20 = def.impact().stream().anyMatch(i -> i instanceof ExtinguishImpact);
               return GpuParticles.adopt(
                  new AttackParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, visual, index, var20, options.getScale(), options.getSeed(), options.getAttackId())
               );
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   static final class RenderTypes {
      static final ParticleRenderType ADDITIVE = build(DestFactor.ONE, "ADDITIVE", AttackParticle.TEXTURE);
      static final ParticleRenderType ALPHA = build(DestFactor.ONE_MINUS_SRC_ALPHA, "ALPHA", AttackParticle.TEXTURE);
      static final ResourceLocation PARTICLE_ATLAS = new ResourceLocation("textures/atlas/particles.png");
      static final ParticleRenderType SHEET_ADDITIVE = build(DestFactor.ONE, "SHEET_ADDITIVE", PARTICLE_ATLAS);
      static final ParticleRenderType SHEET_ALPHA = build(DestFactor.ONE_MINUS_SRC_ALPHA, "SHEET_ALPHA", PARTICLE_ATLAS);

      static ParticleRenderType build(final DestFactor dstFactor, final String name, final ResourceLocation texture) {
         return new ParticleRenderType() {
            private boolean sitOut;

            public void begin(BufferBuilder bufferBuilder, TextureManager textureManager) {
               this.sitOut = GpuParticles.irisOpaquePhase();
               RenderSystem.depthMask(false);
               RenderSystem.setShaderTexture(0, texture);
               RenderSystem.enableBlend();
               RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, dstFactor);
               bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.PARTICLE);
            }

            public void end(Tesselator tesselator) {
               if (this.sitOut) {
                  tesselator.getBuilder().end().release();
               } else {
                  tesselator.end();
               }

               RenderSystem.disableBlend();
               RenderSystem.depthMask(true);
            }

            @Override
            public String toString() {
               return "BREATH_" + name;
            }
         };
      }
   }

   static final class Sheet {
      private static Field atlasField;
      private static boolean lookupFailed;

      @Nullable
      static TextureAtlasSprite sprite(ResourceLocation id) {
         TextureAtlas atlas = atlas();
         return atlas == null ? null : atlas.getSprite(id);
      }

      @Nullable
      private static TextureAtlas atlas() {
         if (lookupFailed) {
            return null;
         }

         try {
            if (atlasField == null) {
               Field found = null;

               for (Field f : ParticleEngine.class.getDeclaredFields()) {
                  if (f.getType() == TextureAtlas.class) {
                     found = f;
                     break;
                  }
               }

               if (found == null) {
                  throw new NoSuchFieldException("TextureAtlas");
               }

               found.setAccessible(true);
               atlasField = found;
            }

            return (TextureAtlas)atlasField.get(Minecraft.getInstance().particleEngine);
         } catch (ReflectiveOperationException | RuntimeException e) {
            lookupFailed = true;
            return null;
         }
      }
   }
}

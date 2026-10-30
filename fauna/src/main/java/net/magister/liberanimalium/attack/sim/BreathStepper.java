package net.magister.liberanimalium.attack.sim;

import java.util.List;
import net.magister.liberanimalium.attack.client.FireColorModel;
import net.magister.liberanimalium.attack.client.ParticleSim;
import net.magister.liberanimalium.attack.data.EmissionConfig;
import net.magister.liberanimalium.attack.data.EmitterSim;
import net.magister.liberanimalium.attack.data.VisualEmitter;
import net.magister.liberanimalium.attack.data.color.PaletteLUT;
import net.magister.liberanimalium.attack.data.render.BillboardRender;
import net.magister.liberanimalium.attack.data.render.CurveSet;
import net.magister.liberanimalium.attack.data.render.PixelQuadRender;
import net.magister.liberanimalium.attack.data.render.RenderConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class BreathStepper {
   private static final PaletteLUT.ColorBand WHITE = new PaletteLUT.ColorBand(List.of(255, 255, 255), 1.0F);
   private static final long SPIN_SALT = 25214903917L;

   private BreathStepper() {
   }

   public static BreathStepper.Launch sample(VisualEmitter visual, Vec3 direction, long seed) {
      BreathRandom random = new BreathRandom(seed);
      EmissionConfig emission = visual.emission();
      float speed = visual.sim().speed();
      EmissionConfig.OffsetSample sample = emission.sample(direction, random, speed);
      Vec3 velocity = emission.sampleVelocity(sample.offset(), direction, speed);
      return new BreathStepper.Launch(sample.offset(), velocity, sample.delay());
   }

   public static BreathParticle begin(
      VisualEmitter visual, double x, double y, double z, double vx, double vy, double vz, float sizeScale, int emitterIndex, long seed
   ) {
      EmissionConfig.OffsetSample sample = visual.emission().sample(new Vec3(vx, vy, vz), new BreathRandom(seed), visual.sim().speed());
      return begin(visual, x, y, z, vx, vy, vz, sizeScale, emitterIndex, seed, sample.delay());
   }

   public static BreathParticle begin(
      VisualEmitter visual, double x, double y, double z, double vx, double vy, double vz, float sizeScale, int emitterIndex, long seed, float delay
   ) {
      BreathRandom random = new BreathRandom(seed ^ 25214903917L);
      EmitterSim sim = visual.sim();
      BreathParticle p = new BreathParticle();
      p.setPos(x, y, z);
      p.setVel(vx, vy, vz);
      p.life = sim.lifetimeTicks();
      p.temperature = sim.initialTemperature();
      p.scale = sizeScale;
      p.emitterIndex = emitterIndex;
      p.sim = sim;
      p.render = visual.render();
      p.delay = delay;
      p.initialDelay = delay;
      p.activeFraction = delay > 0.0F ? 0.0F : 1.0F;
      boolean spins = spins(visual.render());
      p.rot = spins ? random.nextFloat() * (float) (Math.PI * 2) : 0.0F;
      p.oRot = p.rot;
      p.rotSpeed = spins ? (random.nextFloat() - 0.5F) * 0.4F : 0.0F;
      p.seed = random.state();
      return p;
   }

   public static BreathParticle spawn(VisualEmitter visual, Vec3 origin, Vec3 direction, float sizeScale, int emitterIndex, long seed) {
      BreathStepper.Launch launch = sample(visual, direction, seed);
      return begin(
         visual,
         origin.x + launch.offset().x,
         origin.y + launch.offset().y,
         origin.z + launch.offset().z,
         launch.velocity().x,
         launch.velocity().y,
         launch.velocity().z,
         sizeScale,
         emitterIndex,
         seed,
         launch.delay()
      );
   }

   public static boolean tick(BreathParticle p) {
      p.px = p.x;
      p.py = p.y;
      p.pz = p.z;
      if (p.delay >= 1.0F) {
         p.delay--;
         p.activeFraction = 0.0F;
         return true;
      }

      if (p.delay > 0.0F) {
         p.activeFraction = 1.0F - p.delay;
         p.delay = 0.0F;
      } else {
         p.initialDelay = 0.0F;
         p.activeFraction = 1.0F;
      }

      if (p.age++ >= p.life) {
         p.removed = true;
         return false;
      } else {
         p.oRot = p.rot;
         p.rot = p.rot + p.rotSpeed * p.activeFraction;
         BreathRandom random = new BreathRandom(p.seed);
         Vec3 velocity = ParticleSim.step(new Vec3(p.vx, p.vy, p.vz), p.sim, p.age, null, random);
         p.seed = random.state();
         p.vx = velocity.x;
         p.vy = velocity.y;
         p.vz = velocity.z;
         return true;
      }
   }

   public static void advance(BreathParticle p) {
      p.x = p.x + p.vx * p.activeFraction;
      p.y = p.y + p.vy * p.activeFraction;
      p.z = p.z + p.vz * p.activeFraction;
   }

   public static void settle(BreathParticle p) {
      if (p.activeFraction > 0.0F) {
         p.temperature = ParticleSim.cool(p.temperature, p.sim);
      }
   }

   public static BreathStepper.BreathQuad quad(BreathParticle p, float partialTick) {
      if (p.delay >= 1.0F) {
         return new BreathStepper.BreathQuad(p.x, p.y, p.z, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F, true, 0);
      }

      if (p.initialDelay > 0.0F) {
         if (partialTick < p.initialDelay) {
            return new BreathStepper.BreathQuad(p.x, p.y, p.z, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F, true, 0);
         }

         double t = partialTick - p.initialDelay;
         double x = p.px + p.vx * t;
         double y = p.py + p.vy * t;
         double z = p.pz + p.vz * t;
         float spin = p.oRot + p.rotSpeed * (float)t;
         return renderQuad(p, x, y, z, spin);
      } else {
         double x = Mth.lerp(partialTick, p.px, p.x);
         double y = Mth.lerp(partialTick, p.py, p.y);
         double z = Mth.lerp(partialTick, p.pz, p.z);
         float spin = Mth.lerp(partialTick, p.oRot, p.rot);
         return renderQuad(p, x, y, z, spin);
      }
   }

   public static BreathStepper.BreathSpan span(BreathParticle p) {
      if (p.initialDelay > 0.0F) {
         double d = p.initialDelay;
         return new BreathStepper.BreathSpan(
            p.px - p.vx * d,
            p.py - p.vy * d,
            p.pz - p.vz * d,
            p.px + p.vx * (1.0 - d),
            p.py + p.vy * (1.0 - d),
            p.pz + p.vz * (1.0 - d),
            p.oRot - p.rotSpeed * (float)d,
            p.oRot + p.rotSpeed * (float)(1.0 - d),
            (float)d
         );
      } else {
         return new BreathStepper.BreathSpan(p.px, p.py, p.pz, p.x, p.y, p.z, p.oRot, p.rot, 0.0F);
      }
   }

   public static boolean spins(RenderConfig render) {
      if (render instanceof PixelQuadRender quad) {
         return quad.spin();
      } else {
         return render instanceof BillboardRender sprite ? sprite.spin() : true;
      }
   }

   private static BreathStepper.BreathQuad renderQuad(BreathParticle p, double x, double y, double z, float spin) {
      if (p.render instanceof BillboardRender sprite) {
         float size = envelope(p, base(sprite.size()), sprite.curves(), 0);
         float alpha = sprite.curves().alphaAt(p.lifeFraction());
         return new BreathStepper.BreathQuad(x, y, z, size, spin, 1.0F, 1.0F, 1.0F, alpha, "additive".equals(sprite.blend()), sprite.frameAt(p.age, p.life));
      } else if (p.render instanceof PixelQuadRender cfg) {
         PaletteLUT.ColorBand var17 = cfg.palette().map(lut -> FireColorModel.sample(lut, p.temperature)).orElse(WHITE);
         float size = sizeOf(p, cfg);
         float alpha = var17.alpha() * cfg.curves().alphaAt(p.lifeFraction());
         float r = var17.color().size() >= 3 ? var17.color().get(0).intValue() / 255.0F : 1.0F;
         float g = var17.color().size() >= 3 ? var17.color().get(1).intValue() / 255.0F : 1.0F;
         float b = var17.color().size() >= 3 ? var17.color().get(2).intValue() / 255.0F : 1.0F;
         return new BreathStepper.BreathQuad(x, y, z, size, spin, r, g, b, alpha, "additive".equals(cfg.blend()), 0);
      } else {
         return new BreathStepper.BreathQuad(x, y, z, 0.0F, spin, 1.0F, 1.0F, 1.0F, 0.0F, true, 0);
      }
   }

   public static float sizeOf(BreathParticle p, PixelQuadRender cfg) {
      return envelope(p, base(cfg.size()), cfg.curves(), cfg.snap());
   }

   private static float base(List<Float> size) {
      return size.isEmpty() ? 0.25F : size.get(0);
   }

   private static float envelope(BreathParticle p, float base, CurveSet curves, int snap) {
      return curves.size().isPresent()
         ? PixelQuadRender.snapSize(snap, base * curves.sizeAt(p.lifeFraction(), 1.0F) * p.scale)
         : PixelQuadRender.snapSize(snap, ParticleSim.shrinkSize(base, p.sim, p.temperature) * ParticleSim.growScale(p.age, p.sim.growInTicks()) * p.scale);
   }

   public static boolean sizeIsCurved(RenderConfig render) {
      return render instanceof BillboardRender sprite
         ? sprite.curves().size().isPresent()
         : render instanceof PixelQuadRender cfg && cfg.curves().size().isPresent();
   }

   public record BreathQuad(double x, double y, double z, float size, float spin, float r, float g, float b, float a, boolean additive, int frame) {
   }

   public record BreathSpan(double px, double py, double pz, double x, double y, double z, float spin0, float spin1, float showFrom) {
   }

   public record Launch(Vec3 offset, Vec3 velocity, float delay) {
      public Launch(Vec3 offset, Vec3 velocity) {
         this(offset, velocity, 0.0F);
      }
   }
}

package net.magister.liberanimalium.attack.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import net.magister.liberanimalium.attack.data.AttackDefinition;
import net.magister.liberanimalium.attack.data.EmitterConfig;
import net.magister.liberanimalium.attack.data.ProjectileEmitter;
import net.magister.liberanimalium.attack.data.VisualEmitter;
import net.magister.liberanimalium.attack.movement.MovementModifier;
import net.magister.liberanimalium.attack.runtime.AttackInvoker;
import net.magister.liberanimalium.attack.runtime.AttackPhaseClock;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.minecraft.world.phys.Vec3;

public final class BreathSim {
   private final List<BreathParticle> particles = new ArrayList<>();
   private final List<BreathSim.Body> bodies = new ArrayList<>();
   private int tick;
   private long runSeed = 1L;
   private int bodySerial;

   public List<BreathParticle> particles() {
      return this.particles;
   }

   public List<BreathSim.Body> bodies() {
      return this.bodies;
   }

   public int tick() {
      return this.tick;
   }

   public void clear() {
      this.particles.clear();
      this.bodies.clear();
      this.tick = 0;
      this.bodySerial = 0;
   }

   public void reroll() {
      this.runSeed = this.runSeed * 6364136223846793005L + 1442695040888963407L;
      this.clear();
   }

   public void advance(AttackDefinition def, Vec3 origin, Vec3 direction, AttackPhaseClock.Phase phase, float progress, IntPredicate audible, double impactAt) {
      if (def != null) {
         IntPredicate audibleOr = audible == null ? i -> true : audible;
         this.spawnSourceSwarm(def, origin, direction, phase, progress, audibleOr);
         this.spawnBodies(def, origin, direction, phase, progress, audibleOr);
         this.stepParticles();
         this.stepBodies(def, audibleOr, impactAt);
         this.tick++;
      }
   }

   private void spawnSourceSwarm(AttackDefinition def, Vec3 origin, Vec3 direction, AttackPhaseClock.Phase phase, float progress, IntPredicate audible) {
      if (phase != AttackPhaseClock.Phase.IDLE) {
         List<EmitterConfig> emitters = def.emitters();

         for (int i = 0; i < emitters.size(); i++) {
            if (emitters.get(i) instanceof VisualEmitter visual
               && visual.isSourceAnchored()
               && audible.test(i)
               && OmniAttackHandler.replaysThisTick(visual, phase, this.tick)) {
               this.emit(visual, i, origin, direction, progress);
            }
         }
      }
   }

   private void spawnBodies(AttackDefinition def, Vec3 origin, Vec3 direction, AttackPhaseClock.Phase phase, float progress, IntPredicate audible) {
      if (phase != AttackPhaseClock.Phase.IDLE && phase != AttackPhaseClock.Phase.CHARGE) {
         List<EmitterConfig> emitters = def.emitters();

         for (int i = 0; i < emitters.size(); i++) {
            if (emitters.get(i) instanceof ProjectileEmitter pe
               && audible.test(i)
               && this.tick % Math.max(1, pe.emission().rateTicks()) == 0
               && this.bodies.size() <= 64) {
               float scale = phase == AttackPhaseClock.Phase.FIRE ? 1.0F : Math.max(0.05F, progress);

               for (int n = 0; n < Math.max(1, pe.emission().count()); n++) {
                  long seed = BreathRandom.seedFor(this.runSeed, i, this.tick, n);
                  BreathRandom random = new BreathRandom(seed);
                  Vec3 offset = pe.emission().sampleOffset(direction, random);
                  Vec3 velocity = direction.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, pe.sim().speed()) : direction.normalize().scale(pe.sim().speed());
                  this.bodies.add(new BreathSim.Body(++this.bodySerial, i, scale, origin.add(offset), velocity));
               }
            }
         }
      }
   }

   private void emit(VisualEmitter visual, int index, Vec3 origin, Vec3 direction, float progress) {
      int count = AttackInvoker.scaledCount(visual.emission().count(), visual, progress);
      float sizeScale = AttackInvoker.scaledSize(visual, progress);

      for (int ordinal = 0; ordinal < count; ordinal++) {
         if (this.particles.size() >= 20000) {
            return;
         }

         long seed = BreathRandom.seedFor(this.runSeed, index, this.tick, ordinal);
         this.particles.add(BreathStepper.spawn(visual, origin, direction, sizeScale, index, seed));
      }
   }

   private void stepParticles() {
      for (int i = this.particles.size() - 1; i >= 0; i--) {
         BreathParticle p = this.particles.get(i);
         if (!BreathStepper.tick(p)) {
            this.particles.remove(i);
         } else {
            BreathStepper.advance(p);
            BreathStepper.settle(p);
         }
      }
   }

   private void stepBodies(AttackDefinition def, IntPredicate audible, double impactAt) {
      List<EmitterConfig> emitters = def.emitters();

      for (int i = this.bodies.size() - 1; i >= 0; i--) {
         BreathSim.Body body = this.bodies.get(i);
         ProjectileEmitter pe = emitters.size() > body.emitterIndex && emitters.get(body.emitterIndex) instanceof ProjectileEmitter p ? p : null;
         if (pe == null) {
            this.bodies.remove(i);
         } else {
            body.age++;
            Vec3 velocity = new Vec3(body.vx, body.vy, body.vz);

            for (MovementModifier mod : pe.sim().movement()) {
               velocity = mod.apply(velocity, null, body.age);
            }

            body.vx = velocity.x;
            body.vy = velocity.y;
            body.vz = velocity.z;
            body.px = body.x;
            body.py = body.y;
            body.pz = body.z;
            body.x = body.x + body.vx;
            body.y = body.y + body.vy;
            body.z = body.z + body.vz;

            for (int e = 0; e < emitters.size(); e++) {
               if (emitters.get(e) instanceof VisualEmitter visual
                  && visual.isProjectileAnchored()
                  && audible.test(e)
                  && body.age % Math.max(1, visual.emission().rateTicks()) == 0) {
                  this.emit(visual, e, body.pos(), body.velocity(), 1.0F);
               }
            }

            boolean hit = impactAt > 0.0 && body.pos().distanceTo(body.origin) >= impactAt;
            if (hit || body.age > pe.sim().lifetimeTicks()) {
               if (hit) {
                  for (int e = 0; e < emitters.size(); e++) {
                     if (emitters.get(e) instanceof VisualEmitter visual && visual.isImpactAnchored() && audible.test(e)) {
                        this.emit(visual, e, body.pos(), Vec3.ZERO, 1.0F);
                     }
                  }
               }

               this.bodies.remove(i);
            }
         }
      }
   }

   public int[] census(int emitterCount) {
      int[] out = new int[Math.max(0, emitterCount)];

      for (BreathParticle p : this.particles) {
         if (p.emitterIndex >= 0 && p.emitterIndex < out.length) {
            out[p.emitterIndex]++;
         }
      }

      return out;
   }

   public static final class Body {
      public final int id;
      public final int emitterIndex;
      public final float chargeScale;
      public double x;
      public double y;
      public double z;
      public double px;
      public double py;
      public double pz;
      public double vx;
      public double vy;
      public double vz;
      public int age;
      public final Vec3 origin;

      Body(int id, int emitterIndex, float chargeScale, Vec3 at, Vec3 velocity) {
         this.id = id;
         this.emitterIndex = emitterIndex;
         this.chargeScale = chargeScale;
         this.x = this.px = at.x;
         this.y = this.py = at.y;
         this.z = this.pz = at.z;
         this.vx = velocity.x;
         this.vy = velocity.y;
         this.vz = velocity.z;
         this.origin = at;
      }

      public Vec3 pos() {
         return new Vec3(this.x, this.y, this.z);
      }

      public Vec3 velocity() {
         return new Vec3(this.vx, this.vy, this.vz);
      }
   }
}

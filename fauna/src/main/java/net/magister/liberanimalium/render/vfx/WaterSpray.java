package net.magister.liberanimalium.render.vfx;

import net.magister.liberanimalium.attack.client.particle.FaunaParticles;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class WaterSpray {
   private static final double WAKE_FROM = 0.45;
   private static final double WAKE_FULL = 1.6;
   private static final int WAKE_BUDGET = 24;
   private static final int BEAT_BUDGET = 36;

   private WaterSpray() {
   }

   public static void wake(Level level, FaunaBase body, Vec3 at, Vec3 velocity, double speed, float closeness) {
      double top = Math.max(0.05, body.getAttributeValue(Attributes.FLYING_SPEED));
      double share = speed / top;
      if (!(share < 0.45) && !(closeness <= 0.0F)) {
         float strength = (float)Mth.clamp((share - 0.45) / 1.1500000000000001, 0.0, 1.0) * closeness;
         if (!(strength <= 0.02F)) {
            RandomSource random = level.random;
            Vec3 dir = new Vec3(velocity.x, 0.0, velocity.z).normalize();
            Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
            float width = body.getBbWidth();
            int perSheet = Math.min(8, Math.round(strength * (2.0F + width * 1.5F)));

            for (int sheet = -1; sheet <= 1; sheet += 2) {
               for (int i = 0; i < perSheet; i++) {
                  Vec3 from = at.add(side.scale(sheet * width * 0.45 * (0.8 + random.nextDouble() * 0.4)))
                     .subtract(dir.scale(random.nextDouble() * width * 1.5))
                     .add(0.0, 0.05, 0.0);
                  double out = (0.08 + 0.25 * strength) * (0.6 + random.nextDouble() * 0.6);
                  double up = (0.12 + 0.35 * strength) * (0.7 + random.nextDouble() * 0.6);
                  Vec3 v = side.scale(sheet * out).add(dir.scale(speed * 0.25)).add(0.0, up, 0.0);
                  level.addParticle(
                     (ParticleOptions)FaunaParticles.WATER_SPRAY.get(), from.x, from.y, from.z, v.x, v.y, v.z
                  );
               }
            }

            if (share > 1.0) {
               int plume = Math.min(24 - perSheet * 2, Math.round(strength * (2.0F + width)));

               for (int i = 0; i < plume; i++) {
                  Vec3 from = at.subtract(dir.scale(width * (0.5 + random.nextDouble() * 1.5))).add(0.0, 0.05, 0.0);
                  double up = (0.3 + 0.4 * strength) * (0.7 + random.nextDouble() * 0.6);
                  Vec3 v = dir.scale(speed * 0.35).add(side.scale((random.nextDouble() - 0.5) * 0.12)).add(0.0, up, 0.0);
                  level.addParticle(
                     (ParticleOptions)FaunaParticles.WATER_SPRAY.get(), from.x, from.y, from.z, v.x, v.y, v.z
                  );
               }
            }
         }
      }
   }

   public static void beat(Level level, FaunaBase body, Vec3 at, float closeness) {
      if (!(closeness <= 0.0F)) {
         RandomSource random = level.random;
         float width = body.getBbWidth();
         float size = 1.0F + body.getGrowthStage() * 0.2F;
         int count = Math.min(36, Math.round((6.0F + width * 4.0F) * closeness * size));
         double radius = Math.max(0.6, width * 0.4);

         for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * (float) (Math.PI * 2);
            Vec3 out = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            Vec3 from = at.add(out.scale(radius * (0.3 + random.nextDouble() * 0.7))).add(0.0, 0.05, 0.0);
            double push = (0.15 + random.nextDouble() * 0.25) * closeness;
            double up = (0.15 + random.nextDouble() * 0.25) * closeness;
            level.addParticle(
               (ParticleOptions)FaunaParticles.WATER_SPRAY.get(), from.x, from.y, from.z, out.x * push, up, out.z * push
            );
         }
      }
   }

   public static void crest(Level level, double x, double y, double z, float rise) {
      RandomSource random = level.random;
      double up = 0.08 + Math.min(0.5F, rise) * 0.8;
      level.addParticle((ParticleOptions)FaunaParticles.WATER_SPRAY.get(), x, y, z, (random.nextDouble() - 0.5) * 0.08, up, (random.nextDouble() - 0.5) * 0.08);
   }
}

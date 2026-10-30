package net.magister.liberanimalium.render.vfx;

import net.magister.liberanimalium.attack.client.particle.FaunaParticles;
import net.magister.liberanimalium.body.data.FaunaWeightClass;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public final class FootDust {
   static final double MOVING = 0.02;
   static final double FROM = 0.15;
   static final double FULL = 0.3;
   static final float FLOOR = 0.35F;
   private static final float[] RUN = new float[]{0.5F, 1.0F, 2.0F, 3.2F, 4.5F};
   private static final float[] WALK = new float[]{0.0F, 0.0F, 0.45F, 1.1F, 1.8F};
   private static final float[] GROWN = new float[]{0.35F, 0.65F, 1.0F, 1.15F};
   public static final int STRIDE = 8;
   private static final double KICK = 0.45;
   private static final double KICK_WALK = 0.25;

   private FootDust() {
   }

   static float strength(double speed) {
      return speed < 0.15 ? 0.0F : 0.35F + 0.65F * (float)Mth.clamp((speed - 0.15) / 0.15, 0.0, 1.0);
   }

   static float cadence(int strideTicks) {
      return Mth.clamp(strideTicks / 8.0F, 0.4F, 1.6F);
   }

   private static float grown(int stage) {
      return GROWN[Mth.clamp(stage, 0, GROWN.length - 1)];
   }

   static float runBudget(FaunaWeightClass weight, int stage, double speed, int strideTicks) {
      return RUN[weight.ordinal()] * grown(stage) * strength(speed) * cadence(strideTicks);
   }

   static float walkBudget(FaunaWeightClass weight, int stage, double speed, int strideTicks) {
      return speed < 0.02 ? 0.0F : WALK[weight.ordinal()] * grown(stage) * cadence(strideTicks);
   }

   static int whole(float expected, float roll) {
      int n = (int)expected;
      return roll < expected - n ? n + 1 : n;
   }

   public static void footfall(FaunaBase body, double x, double groundY, double z, double vx, double vz, int strideTicks) {
      Level level = body.level();
      if (level.isClientSide() && body.animRig().vfx().runDust() && !body.isInWater()) {
         double speed = Math.hypot(vx, vz);
         FaunaWeightClass weight = body.getWeightClass();
         int stage = body.getGrowthStage();
         float run = runBudget(weight, stage, speed, strideTicks);
         float walk = walkBudget(weight, stage, speed, strideTicks);
         if (!(run <= 0.0F) || !(walk <= 0.0F)) {
            BlockPos under = BlockPos.containing(x, groundY - 0.1, z);
            if (!level.getBlockState(under).isAir() && level.getFluidState(under.above()).isEmpty()) {
               WingWashGround ground = WingWashGround.of(level, under);
               RandomSource random = body.getRandom();
               double backX = -vx / speed;
               double backZ = -vz / speed;
               int dust = whole(run * ground.dustShare, random.nextFloat());

               for (int i = 0; i < dust; i++) {
                  double out = 0.03 + random.nextDouble() * 0.05;
                  level.addParticle(
                     (ParticleOptions)FaunaParticles.WING_DUST.get(),
                     x + (random.nextDouble() - 0.5) * 0.3,
                     groundY + 0.4,
                     z + (random.nextDouble() - 0.5) * 0.3,
                     backX * out + (random.nextDouble() - 0.5) * 0.04,
                     0.0,
                     backZ * out + (random.nextDouble() - 0.5) * 0.04
                  );
               }

               int debris = whole(Math.max(run, walk) * ground.debrisShare, random.nextFloat());
               double kick = run >= walk ? 0.45 : 0.25;

               for (int i = 0; i < debris; i++) {
                  double turn = (random.nextDouble() - 0.5) * Math.PI * 0.5;
                  double cos = Math.cos(turn);
                  double sin = Math.sin(turn);
                  double dirX = backX * cos - backZ * sin;
                  double dirZ = backX * sin + backZ * cos;
                  double out = (ground.outMin + random.nextDouble() * ground.outSpan) * kick;
                  level.addParticle(
                     (ParticleOptions)FaunaParticles.WING_DEBRIS.get(),
                     x + (random.nextDouble() - 0.5) * 0.3,
                     groundY + 0.15,
                     z + (random.nextDouble() - 0.5) * 0.3,
                     dirX * out,
                     ground.upMin + random.nextDouble() * ground.upSpan,
                     dirZ * out
                  );
               }
            }
         }
      }
   }
}

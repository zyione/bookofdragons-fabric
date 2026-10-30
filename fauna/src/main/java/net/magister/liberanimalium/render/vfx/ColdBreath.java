package net.magister.liberanimalium.render.vfx;

import net.magister.liberanimalium.attack.client.particle.FaunaParticles;
import net.magister.liberanimalium.attack.runtime.AttackPhaseClock;
import net.magister.liberanimalium.body.BabyArt;
import net.magister.liberanimalium.body.anim.data.PerEntityHeadMatrixExtractor;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

public final class ColdBreath {
   public static final int PERIOD_CALM = 56;
   public static final int PERIOD_WORKING = 34;
   public static final int PERIOD_ASLEEP = 90;
   public static final double DRIFT = 0.045;
   private static final double MAX_DISTANCE_SQ = 2304.0;

   private ColdBreath() {
   }

   public static void tick(FaunaBase body) {
      Level level = body.level();
      if (level.isClientSide() && body.animRig().vfx().coldBreath()) {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null && !(client.player.distanceToSqr(body) > 2304.0)) {
            if ((level.getGameTime() + body.getId() * 7L) % period(body) == 0L) {
               Vec3 mouth = PerEntityHeadMatrixExtractor.resolveMouthPosition(body);
               BlockPos at = BlockPos.containing(mouth);
               if (((Biome)level.getBiome(at).value()).coldEnoughToSnow(at)) {
                  if (!level.getFluidState(at).isSource() && !body.isUnderWater()) {
                     OmniAttackHandler breath = body.componentRegistry.get(OmniAttackHandler.class);
                     if (breath != null) {
                        AttackPhaseClock.Phase phase = breath.phaseClock().phase();
                        if (phase == AttackPhaseClock.Phase.RELEASE || phase == AttackPhaseClock.Phase.FIRE) {
                           return;
                        }
                     }

                     Vec3 out = Vec3.directionFromRotation(body.getXRot(), body.getYHeadRot());
                     boolean fromNose = false;

                     for (String bone : NoseRule.known(BabyArt.modelKey(body))) {
                        Vec3 spot = body.animBoneSpot(bone);
                        if (spot != null) {
                           exhale(level, spot, out, 1, body.getRandom());
                           fromNose = true;
                        }
                     }

                     if (!fromNose) {
                        exhale(level, mouth, out, 2, body.getRandom());
                     }
                  }
               }
            }
         }
      }
   }

   static int period(FaunaBase body) {
      if (body.hasFaunaPose(PoseFlag.SLEEPING)) {
         return 90;
      } else {
         return !body.isFlying() && !body.hasFaunaPose(PoseFlag.SPRINTING) ? 56 : 34;
      }
   }

   private static void exhale(Level level, Vec3 at, Vec3 out, int count, RandomSource random) {
      Vec3 drift = out.scale(0.045).add(0.0, 0.01125, 0.0);

      for (int i = 0; i < count; i++) {
         level.addParticle(
            (ParticleOptions)FaunaParticles.BREATH_PUFF.get(),
            at.x + (random.nextDouble() - 0.5) * 0.08,
            at.y + (random.nextDouble() - 0.5) * 0.08,
            at.z + (random.nextDouble() - 0.5) * 0.08,
            drift.x + (random.nextDouble() - 0.5) * 0.01,
            drift.y + random.nextDouble() * 0.008,
            drift.z + (random.nextDouble() - 0.5) * 0.01
         );
      }
   }
}

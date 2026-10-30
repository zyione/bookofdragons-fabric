package net.magister.liberanimalium.render.vfx;

import java.util.ArrayList;
import java.util.List;
import net.magister.liberanimalium.attack.client.particle.FaunaParticles;
import net.magister.liberanimalium.body.BabyArt;
import net.magister.liberanimalium.body.anim.data.PerEntityHeadMatrixExtractor;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.fauna.core.Seeds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SleepBubbles {
   private static final int EXHALE_TICKS = 36;
   private static final int MOST_PER_BREATH = 3;
   private static final double LIFT = 0.01;
   private static final double LIFT_SPREAD = 0.006;
   private static final double ALONG = 0.003;
   private static final double MAX_DISTANCE_SQ = 1024.0;

   private SleepBubbles() {
   }

   public static void tick(FaunaBase body) {
      Level level = body.level();
      if (level.isClientSide() && body.animRig().vfx().sleepBubbles()) {
         if (body.hasFaunaPose(PoseFlag.SLEEPING)) {
            long beat = level.getGameTime() + body.getId() * 7L;
            int phase = (int)Math.floorMod(beat, 90L);
            if (phase < 36) {
               long draw = Seeds.mix(Math.floorDiv(beat, 90L) * 31L + body.getId());
               int count = 1 + (int)Math.floorMod(draw, 3L);
               int due = 0;

               for (int i = 0; i < count; i++) {
                  if (Math.floorMod(draw >>> 8 + i * 9, 36L) == phase) {
                     due |= 1 << i;
                  }
               }

               if (due != 0) {
                  Minecraft client = Minecraft.getInstance();
                  if (client.player != null && !(client.player.distanceToSqr(body) > 1024.0)) {
                     List<Vec3> nostrils = new ArrayList<>(2);

                     for (String bone : NoseRule.known(BabyArt.modelKey(body))) {
                        Vec3 spot = body.animBoneSpot(bone);
                        if (spot != null) {
                           nostrils.add(spot);
                        }
                     }

                     if (nostrils.isEmpty()) {
                        nostrils.add(PerEntityHeadMatrixExtractor.resolveMouthPosition(body));
                     }

                     RandomSource random = body.getRandom();
                     Vec3 out = Vec3.directionFromRotation(body.getXRot(), body.getYHeadRot());

                     for (int i = 0; i < count; i++) {
                        if ((due & 1 << i) != 0) {
                           int nostril = (int)(draw >>> 40 + i & 1L) % nostrils.size();
                           blow(level, nostrils.get(nostril), out, random);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void blow(Level level, Vec3 at, Vec3 out, RandomSource random) {
      if (!level.getFluidState(BlockPos.containing(at)).is(FluidTags.WATER)) {
         level.addParticle(
            (ParticleOptions)FaunaParticles.SLEEP_BUBBLE.get(),
            at.x + (random.nextDouble() - 0.5) * 0.04,
            at.y + (random.nextDouble() - 0.5) * 0.04,
            at.z + (random.nextDouble() - 0.5) * 0.04,
            out.x * 0.003 + (random.nextDouble() - 0.5) * 0.003,
            0.01 + random.nextDouble() * 0.006,
            out.z * 0.003 + (random.nextDouble() - 0.5) * 0.003
         );
      } else {
         for (int i = 0; i < 2 + random.nextInt(2); i++) {
            level.addParticle(
               ParticleTypes.BUBBLE,
               at.x,
               at.y,
               at.z,
               out.x * 0.05 + (random.nextDouble() - 0.5) * 0.02,
               0.08 + random.nextDouble() * 0.04,
               out.z * 0.05 + (random.nextDouble() - 0.5) * 0.02
            );
         }
      }
   }
}

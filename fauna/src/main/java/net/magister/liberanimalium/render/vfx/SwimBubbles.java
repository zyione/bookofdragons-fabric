package net.magister.liberanimalium.render.vfx;

import java.util.ArrayList;
import net.magister.liberanimalium.body.anim.data.PerEntityHeadMatrixExtractor;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.fauna.core.Seeds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SwimBubbles {
   private static final float EXHALE_SHARE = 0.4F;
   private static final int FEWEST_PER_BREATH = 5;
   private static final int MORE_PER_BREATH = 4;
   private static final double LIFT = 0.03;
   private static final double LIFT_SPREAD = 0.03;
   private static final double ALONG = 0.03;
   private static final double MAX_DISTANCE_SQ = 1024.0;

   private SwimBubbles() {
   }

   public static void tick(FaunaBase body) {
      Level level = body.level();
      if (level.isClientSide() && body.isInWater() && body.animRig().vfx().swimBubbles()) {
         if (!body.hasFaunaPose(PoseFlag.SLEEPING)) {
            int period = ColdBreath.period(body);
            int exhale = Math.max(1, (int)(period * 0.4F));
            long beat = level.getGameTime() + body.getId() * 7L;
            int phase = (int)Math.floorMod((long)beat, (long)period);
            if (phase < exhale) {
               long draw = Seeds.mix(Math.floorDiv((long)beat, (long)period) * 31L + body.getId());
               int count = 5 + (int)Math.floorMod(draw, 4L);
               int due = 0;

               for (int i = 0; i < count; i++) {
                  if (Math.floorMod((long)(draw >>> 4 + i * 6), (long)exhale) == phase) {
                     due++;
                  }
               }

               if (due != 0) {
                  Minecraft client = Minecraft.getInstance();
                  if (client.player != null && !(client.player.distanceToSqr(body) > 1024.0)) {
                     if (level instanceof ClientLevel world) {
                        ArrayList<Vec3> nostrils = new ArrayList<>(2);

                        for (String bone : NoseRule.known(body.animSpeciesId())) {
                           Vec3 spot = body.animBoneSpot(bone);
                           if (spot != null) {
                              nostrils.add(spot);
                           }
                        }

                        if (nostrils.isEmpty()) {
                           nostrils.add(PerEntityHeadMatrixExtractor.resolveMouthPosition(body));
                        }

                        nostrils.removeIf(atx -> !level.getFluidState(BlockPos.containing(atx)).is(FluidTags.WATER));
                        if (!nostrils.isEmpty()) {
                           RandomSource random = body.getRandom();
                           Vec3 out = Vec3.directionFromRotation(body.getXRot(), body.getYHeadRot());

                           for (int i = 0; i < due; i++) {
                              Vec3 at = (Vec3)nostrils.get(random.nextInt(nostrils.size()));
                              Particle bubble = SleepBubbleParticle.underWater(
                                 world,
                                 at.x + (random.nextDouble() - 0.5) * 0.08,
                                 at.y + (random.nextDouble() - 0.5) * 0.08,
                                 at.z + (random.nextDouble() - 0.5) * 0.08,
                                 out.x * 0.03 + (random.nextDouble() - 0.5) * 0.02,
                                 0.03 + random.nextDouble() * 0.03,
                                 out.z * 0.03 + (random.nextDouble() - 0.5) * 0.02
                              );
                              if (bubble != null) {
                                 client.particleEngine.add(bubble);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}

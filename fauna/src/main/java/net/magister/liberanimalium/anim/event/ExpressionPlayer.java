package net.magister.liberanimalium.anim.event;

import net.magister.liberanimalium.anim.api.AnimatedBody;
import net.magister.liberanimalium.platform.registry.Regs;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ExpressionPlayer {
   private ExpressionPlayer() {
   }

   public static void play(AnimatedBody body, EventRow row) {
      String clip = row.resolveClip(body.animClipPrefix());
      if (clip != null && body.hasClip(clip)) {
         body.playClip(row.layer(), clip);
      }

      Level level = body.animLevel();
      Vec3 at = body.animPosition();
      if (row.sound() != null) {
         body.playVoice(row.sound());
      }

      if (row.wash() > 0) {
         body.animWash(row.wash());
      }

      if (row.particle() != null) {
         ParticleOptions particle = particleOf(row.particle());
         if (particle != null) {
            boolean placed = false;

            for (String bone : row.bones()) {
               Vec3 spot = body.animBoneSpot(bone);
               if (spot != null) {
                  puff(level, particle, row, spot);
                  placed = true;
               }
            }

            if (!placed) {
               puff(level, particle, row, at);
            }
         }
      }
   }

   private static void puff(Level level, ParticleOptions particle, EventRow row, Vec3 at) {
      double spread = row.particleSpread();

      for (int i = 0; i < row.particleCount(); i++) {
         level.addParticle(
            particle,
            at.x + (level.random.nextDouble() - 0.5) * spread * 2.0,
            at.y + level.random.nextDouble() * spread * 2.0,
            at.z + (level.random.nextDouble() - 0.5) * spread * 2.0,
            0.0,
            row.particleRise(),
            0.0
         );
      }
   }

   public static ParticleOptions particleOf(String id) {
      return Regs.particleType(ResourceLocation.tryParse(id)) instanceof ParticleOptions options ? options : null;
   }
}

package net.magister.bookofdragons.sound;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.sound.FaunaSoundHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class RefusalTelegraph {
   private RefusalTelegraph() {
   }

   public static void emit(FaunaBase dragon) {
      if (dragon.level() instanceof ServerLevel srv) {
         Vec3 at = dragon.moodPoint();
         double spread = FaunaBase.moodSpread();
         srv.sendParticles(ParticleTypes.ANGRY_VILLAGER, at.x, at.y, at.z, 5, spread, spread, spread, 0.02);
      }

      FaunaSoundHelper.playRoarSound(dragon);
      dragon.pulseRefusal();
   }
}

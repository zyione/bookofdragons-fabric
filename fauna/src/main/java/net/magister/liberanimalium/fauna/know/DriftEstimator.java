package net.magister.liberanimalium.fauna.know;

import net.minecraft.world.phys.Vec3;

public final class DriftEstimator implements PositionEstimator {
   public static final DriftEstimator INSTANCE = new DriftEstimator();

   private DriftEstimator() {
   }

   @Override
   public Vec3 estimate(Representation rep, long now, float sightRadius) {
      long since = rep.visible() ? rep.sinceLook(now) : rep.age(now);
      long lead = Math.min(since, 40L);
      Vec3 drift = rep.lastVelocity().scale(lead);
      double length = drift.length();
      if (length > sightRadius && length > 0.0) {
         drift = drift.scale(sightRadius / length);
      }

      return rep.lastKnownPos().add(drift);
   }
}

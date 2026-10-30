package net.magister.liberanimalium.fauna.api;

import net.minecraft.world.phys.Vec3;

public record MoveIntent(Vec3 direction, float speed, MoveMode mode, boolean running) {
   public static MoveIntent hold(MoveMode mode) {
      return new MoveIntent(Vec3.ZERO, 0.0F, mode, false);
   }

   public boolean moving() {
      return this.speed > 0.0F && this.direction.lengthSqr() > 1.0E-6;
   }
}

package net.magister.liberanimalium.fauna.maneuver;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public record Subject(UUID id, Vec3 estimate, Vec3 velocity, Vec3 facing, float apparentSize) {
   public Vec3 flatEstimate(double y) {
      return new Vec3(this.estimate.x, y, this.estimate.z);
   }

   public double eyeHeight() {
      return Math.sqrt(Math.max(this.apparentSize, 0.0F));
   }

   public double girth() {
      return Math.sqrt(Math.max(this.apparentSize, 0.0F));
   }

   public double attentionOn(Vec3 point) {
      Vec3 toward = point.subtract(this.estimate);
      return toward.lengthSqr() < 1.0E-6 ? 1.0 : Math.max(0.0, this.facing.dot(toward.normalize()));
   }
}

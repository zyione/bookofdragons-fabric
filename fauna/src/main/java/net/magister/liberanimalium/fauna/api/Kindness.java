package net.magister.liberanimalium.fauna.api;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public record Kindness(UUID sourceId, Kind kind, Vec3 position, long tick, Delivery delivery, float craving, float relish) {
   public Kindness(UUID sourceId, Kind kind, Vec3 position, long tick, Delivery delivery, float craving) {
      this(sourceId, kind, position, tick, delivery, craving, 1.0F);
   }

   public Kindness(UUID sourceId, Kind kind, Vec3 position, long tick, Delivery delivery) {
      this(sourceId, kind, position, tick, delivery, 1.0F);
   }

   public Kindness(UUID sourceId, Kind kind, Vec3 position, long tick) {
      this(sourceId, kind, position, tick, Delivery.HAND);
   }

   public float share() {
      return this.delivery.share();
   }
}

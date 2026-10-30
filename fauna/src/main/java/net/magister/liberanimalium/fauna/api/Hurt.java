package net.magister.liberanimalium.fauna.api;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;

public record Hurt(@Nullable UUID attackerId, Kind kind, Vec3 position, float amount, long tick) {
   public boolean hasAttacker() {
      return this.attackerId != null;
   }
}

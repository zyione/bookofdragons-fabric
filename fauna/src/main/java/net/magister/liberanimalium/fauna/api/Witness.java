package net.magister.liberanimalium.fauna.api;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;

public record Witness(UUID victimId, @Nullable UUID attackerId, Kind attackerKind, Vec3 position, float severity, long tick) {
   public boolean hasAttacker() {
      return this.attackerId != null;
   }
}

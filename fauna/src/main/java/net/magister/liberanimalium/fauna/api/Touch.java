package net.magister.liberanimalium.fauna.api;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public record Touch(UUID sourceId, Kind kind, Vec3 position, long tick, int heldTicks, boolean fits) {
}

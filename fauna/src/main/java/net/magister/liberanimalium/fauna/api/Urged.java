package net.magister.liberanimalium.fauna.api;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public record Urged(UUID ownerId, UUID targetId, Kind targetKind, Vec3 position, boolean calm, long tick) {
}

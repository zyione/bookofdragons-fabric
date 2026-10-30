package net.magister.liberanimalium.fauna.api;

import java.util.UUID;

public record Buck(UUID riderId, boolean survived, long tick) {
}

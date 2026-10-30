package net.magister.liberanimalium.fauna.api;

import java.util.UUID;
import net.minecraft.core.BlockPos;

public record ContainerOpened(UUID opener, BlockPos at) {
}

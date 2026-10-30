package net.magister.liberanimalium.platform.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface ChunkTickets {
   boolean force(ServerLevel var1, BlockPos var2, int var3, int var4, boolean var5, boolean var6);
}

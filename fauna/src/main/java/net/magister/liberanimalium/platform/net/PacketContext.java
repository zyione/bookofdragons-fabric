package net.magister.liberanimalium.platform.net;

import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerPlayer;

public interface PacketContext {
   void enqueueWork(Runnable var1);

   @Nullable
   ServerPlayer sender();
}

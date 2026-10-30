package net.magister.liberanimalium.platform.net;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public interface Channel {
   <T> void toServer(String var1, Class<T> var2, BiConsumer<T, FriendlyByteBuf> var3, Function<FriendlyByteBuf, T> var4, BiConsumer<T, PacketContext> var5);

   <T> void toClient(String var1, Class<T> var2, BiConsumer<T, FriendlyByteBuf> var3, Function<FriendlyByteBuf, T> var4, BiConsumer<T, PacketContext> var5);

   void sendToServer(Object var1);

   void sendToPlayer(Object var1, ServerPlayer var2);

   void sendToAll(Object var1);

   void sendToTracking(Object var1, Entity var2);

   void sendToTrackingAndSelf(Object var1, Entity var2);

   void sendToNear(Object var1, ServerLevel var2, double var3, double var5, double var7, double var9);
}

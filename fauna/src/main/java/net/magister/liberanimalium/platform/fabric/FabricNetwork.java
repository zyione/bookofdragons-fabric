package net.magister.liberanimalium.platform.fabric;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class FabricNetwork implements NetworkFactory {
    public Channel channel(ResourceLocation id, String protocolVersion, boolean optional) {
        return new NativeChannel(id, protocolVersion, optional);
    }
    private record Message<T>(ResourceLocation id, BiConsumer<T, FriendlyByteBuf> encode, boolean serverbound) {
        FriendlyByteBuf encodeObject(Object object) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            try { encode.accept((T) object, buf); return buf; }
            catch (RuntimeException failure) { buf.release(); throw failure; }
        }
    }
    private static final class NativeChannel implements Channel {
        private static final int MAX_PAYLOAD = 1_048_576;
        private final ResourceLocation channel;
        private final String protocol;
        private final boolean optional;
        private final Map<Class<?>, Message<?>> messages = new HashMap<>();
        private final java.util.Set<String> names = new java.util.HashSet<>();
        NativeChannel(ResourceLocation channel, String protocol, boolean optional) {
            this.channel = channel; this.protocol = protocol; this.optional = optional;
            ResourceLocation handshake = new ResourceLocation(channel.getNamespace(), channel.getPath() + "/protocol");
            net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
                FriendlyByteBuf query = PacketByteBufs.create();
                query.writeUtf(protocol, 64);
                sender.sendPacket(handshake, query);
            });
            net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking.registerGlobalReceiver(handshake, (server, handler, understood, buf, synchronizer, sender) -> {
                boolean accepted = optional && !understood;
                if (understood) {
                    try { accepted = protocol.equals(buf.readUtf(64)) && !buf.isReadable(); }
                    catch (RuntimeException malformed) { accepted = false; }
                }
                if (!accepted) synchronizer.waitFor(server.submit(() -> handler.disconnect(Component.literal("This server requires " + channel + " protocol " + protocol + " (Fabric 1.20.1)"))));
            });
            if (Services.platform().isClient()) Client.handshake(handshake, protocol);
        }
        private <T> Message<T> add(String name, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, boolean serverbound) {
            if (!name.matches("[a-z0-9/._-]+") || !names.add(name) || messages.containsKey(type))
                throw new IllegalArgumentException("Duplicate or invalid message " + channel + "/" + name);
            Message<T> message = new Message<>(new ResourceLocation(channel.getNamespace(), channel.getPath() + "/v" + protocol + "/" + name), encoder, serverbound);
            messages.put(type, message);
            return message;
        }
        public <T> void toServer(String name, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
                                 Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler) {
            Message<T> message = add(name, type, encoder, true);
            ServerPlayNetworking.PlayChannelHandler receiver = (server, player, connection, buf, response) -> {
                try {
                    if (buf.readableBytes() > MAX_PAYLOAD) throw new IllegalArgumentException("Oversized payload");
                    T decoded = decoder.apply(buf);
                    if (buf.isReadable()) throw new IllegalArgumentException("Trailing payload bytes");
                    server.execute(() -> {
                        if (!player.isRemoved() && connection.isAcceptingMessages())
                            handler.accept(decoded, new PacketContext() {
                                public void enqueueWork(Runnable work) { server.execute(work); }
                                public ServerPlayer sender() { return player; }
                            });
                    });
                } catch (RuntimeException malformed) {
                    server.execute(() -> connection.disconnect(Component.literal("Invalid " + message.id() + " packet")));
                    org.slf4j.LoggerFactory.getLogger("liberanimalium").warn("Rejected malformed {} from {}", message.id(), player.getUUID(), malformed);
                }
            };
            ServerPlayNetworking.registerGlobalReceiver(message.id(), receiver);
            FragmentedC2S.register(message.id(), receiver);
        }
        public <T> void toClient(String name, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
                                 Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler) {
            Message<T> message = add(name, type, encoder, false);
            if (Services.platform().isClient()) Client.register(message.id(), decoder, handler);
        }
        private Message<?> message(Object value, boolean serverbound) {
            Message<?> message = messages.get(value.getClass());
            if (message == null || message.serverbound() != serverbound)
                throw new IllegalArgumentException("Unregistered message or wrong direction: " + value.getClass());
            return message;
        }
        public void sendToServer(Object value) {
            Message<?> message = message(value, true);
            Client.send(message.id(), message.encodeObject(value));
        }
        public void sendToPlayer(Object value, ServerPlayer player) {
            Message<?> message = message(value, false);
            if (optional && !ServerPlayNetworking.canSend(player, message.id())) return;
            FriendlyByteBuf buf = message.encodeObject(value);
            ServerPlayNetworking.send(player, message.id(), buf);
        }
        public void sendToAll(Object value) {
            for (ServerPlayer player : PlayerLookup.all(FabricServer.current())) sendToPlayer(value, player);
        }
        public void sendToTracking(Object value, Entity entity) {
            for (ServerPlayer player : PlayerLookup.tracking(entity)) sendToPlayer(value, player);
        }
        public void sendToTrackingAndSelf(Object value, Entity entity) {
            var players = new java.util.HashSet<>(PlayerLookup.tracking(entity));
            if (entity instanceof ServerPlayer player) players.add(player);
            for (ServerPlayer player : players) sendToPlayer(value, player);
        }
        public void sendToNear(Object value, ServerLevel level, double x, double y, double z, double radius) {
            for (ServerPlayer player : PlayerLookup.around(level, new net.minecraft.world.phys.Vec3(x, y, z), radius)) sendToPlayer(value, player);
        }
    }
    private static final class Client {
        static void handshake(ResourceLocation id, String protocol) {
            net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking.registerGlobalReceiver(id, (client, handler, buf, listeners) -> {
                buf.readUtf(64);
                if (buf.isReadable()) throw new IllegalArgumentException("Malformed channel handshake " + id);
                FriendlyByteBuf response = PacketByteBufs.create();
                response.writeUtf(protocol, 64);
                return java.util.concurrent.CompletableFuture.completedFuture(response);
            });
        }
        static <T> void register(ResourceLocation id, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(id, (client, connection, buf, response) -> {
                if (buf.readableBytes() > NativeChannel.MAX_PAYLOAD) throw new IllegalArgumentException("Oversized " + id);
                T decoded = decoder.apply(buf);
                if (buf.isReadable()) throw new IllegalArgumentException("Trailing " + id + " bytes");
                client.execute(() -> handler.accept(decoded, new PacketContext() {
                    public void enqueueWork(Runnable work) { client.execute(work); }
                    public ServerPlayer sender() { return null; }
                }));
            });
        }
        static void send(ResourceLocation id, FriendlyByteBuf buf) {
            FragmentedC2S.send(id, buf);
        }
    }
}

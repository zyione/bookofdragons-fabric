package net.magister.liberanimalium.platform.fabric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.magister.liberanimalium.platform.net.FragmentAssembler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Preserves the book's 100-page note saves beyond vanilla's 32 KiB C2S limit. */
public final class FragmentedC2S {
    private static final ResourceLocation ID = new ResourceLocation("liberanimalium", "fragment_c2s");
    private static final Map<ResourceLocation, ServerPlayNetworking.PlayChannelHandler> TARGETS = new ConcurrentHashMap<>();
    private static final FragmentAssembler ASSEMBLER = new FragmentAssembler();
    public static void register(ResourceLocation id, ServerPlayNetworking.PlayChannelHandler handler) {
        if (TARGETS.putIfAbsent(id, handler) != null) throw new IllegalArgumentException("Duplicate fragmented target " + id);
    }
    public static void install() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, connection, buf, response) -> {
            try {
                ResourceLocation target = buf.readResourceLocation();
                var receiver = TARGETS.get(target);
                if (receiver == null) throw new IllegalArgumentException("Unknown fragmented target");
                int transfer = buf.readVarInt(), index = buf.readVarInt(), total = buf.readVarInt();
                if (buf.readableBytes() > FragmentAssembler.CHUNK_BYTES) throw new IllegalArgumentException("Oversized chunk");
                byte[] chunk = new byte[buf.readableBytes()];
                buf.readBytes(chunk);
                byte[] complete = ASSEMBLER.accept(player.getUUID(), target.toString(), transfer, index, total, chunk, System.nanoTime());
                if (complete != null) {
                    FriendlyByteBuf assembled = new FriendlyByteBuf(Unpooled.wrappedBuffer(complete));
                    try { receiver.receive(server, player, connection, assembled, response); }
                    finally { assembled.release(); }
                }
            } catch (RuntimeException malformed) {
                ASSEMBLER.forget(player.getUUID());
                server.execute(() -> connection.disconnect(Component.literal("Invalid fragmented mod packet")));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((connection, server) -> ASSEMBLER.forget(connection.player.getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(server -> ASSEMBLER.expire(System.nanoTime()));
    }
    public static void send(ResourceLocation target, FriendlyByteBuf data) { Client.send(target, data); }
    private static final class Client {
        private static final AtomicInteger TRANSFERS = new AtomicInteger();
        static void send(ResourceLocation target, FriendlyByteBuf data) {
            int total = data.readableBytes();
            if (total <= FragmentAssembler.CHUNK_BYTES) {
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(target, data);
                return;
            }
            try {
                if (total > FragmentAssembler.MAX_BYTES) throw new IllegalArgumentException("Mod message exceeds transfer limit");
                int transfer = TRANSFERS.incrementAndGet(), index = 0;
                while (data.isReadable()) {
                    FriendlyByteBuf frame = PacketByteBufs.create();
                    frame.writeResourceLocation(target);
                    frame.writeVarInt(transfer); frame.writeVarInt(index++); frame.writeVarInt(total);
                    frame.writeBytes(data, Math.min(data.readableBytes(), FragmentAssembler.CHUNK_BYTES));
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(ID, frame);
                }
            } finally { data.release(); }
        }
    }
}

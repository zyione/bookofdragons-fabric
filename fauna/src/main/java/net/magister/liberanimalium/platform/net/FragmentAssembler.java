package net.magister.liberanimalium.platform.net;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded, ordered reassembly; one transfer per connection avoids retained partial floods. */
public final class FragmentAssembler {
    public static final int MAX_BYTES = 1_048_576;
    public static final int CHUNK_BYTES = 30_000;
    private static final long TIMEOUT_NS = 10_000_000_000L;
    private final Map<UUID, Pending> pending = new HashMap<>();
    private static final class Pending {
        final String channel;
        final int transfer, total;
        final long started;
        final ByteArrayOutputStream bytes;
        int next;
        Pending(String channel, int transfer, int total, long now) {
            this.channel = channel; this.transfer = transfer; this.total = total; this.started = now;
            this.bytes = new ByteArrayOutputStream(Math.min(total, CHUNK_BYTES));
        }
    }
    public synchronized byte[] accept(UUID player, String channel, int transfer, int index, int total, byte[] bytes, long now) {
        try {
            if (total <= CHUNK_BYTES || total > MAX_BYTES || index < 0 || bytes.length == 0 || bytes.length > CHUNK_BYTES)
                throw new IllegalArgumentException("Invalid fragment bounds");
            Pending state = pending.get(player);
            if (index == 0) {
                if (state != null && now - state.started <= TIMEOUT_NS) throw new IllegalArgumentException("Overlapping transfer");
                state = new Pending(channel, transfer, total, now);
                pending.put(player, state);
            }
            if (state == null || now - state.started > TIMEOUT_NS || !state.channel.equals(channel) || state.transfer != transfer || state.total != total || state.next != index)
                throw new IllegalArgumentException("Out-of-order or expired transfer");
            int left = total - state.bytes.size();
            if (bytes.length != Math.min(CHUNK_BYTES, left)) throw new IllegalArgumentException("Truncated or oversized fragment");
            state.bytes.writeBytes(bytes);
            state.next++;
            if (state.bytes.size() != total) return null;
            pending.remove(player);
            return state.bytes.toByteArray();
        } catch (RuntimeException failure) {
            pending.remove(player);
            throw failure;
        }
    }
    public synchronized void forget(UUID player) { pending.remove(player); }
    public synchronized void expire(long now) { pending.values().removeIf(value -> now - value.started > TIMEOUT_NS); }
    public synchronized int pendingTransfers() { return pending.size(); }
}

package net.magister.liberanimalium.platform.net;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FragmentAssemblerTest {
    @Test void largeNoteSavesRoundTripWithIndependentPlayers() {
        FragmentAssembler assembler = new FragmentAssembler();
        Random random = new Random(1201);
        for (int size : new int[]{30_001, 60_000, 102_400, FragmentAssembler.MAX_BYTES}) {
            byte[] payload = new byte[size]; random.nextBytes(payload);
            UUID first = UUID.randomUUID(), second = UUID.randomUUID();
            for (int offset = 0, index = 0; offset < size; offset += FragmentAssembler.CHUNK_BYTES, index++) {
                byte[] chunk = Arrays.copyOfRange(payload, offset, Math.min(size, offset + FragmentAssembler.CHUNK_BYTES));
                byte[] a = assembler.accept(first, "book:notes", 3, index, size, chunk, index);
                byte[] b = assembler.accept(second, "book:notes", 3, index, size, chunk, index);
                if (offset + chunk.length < size) { assertNull(a); assertNull(b); }
                else { assertArrayEquals(payload, a); assertArrayEquals(payload, b); }
            }
            assertEquals(0, assembler.pendingTransfers());
        }
    }
    @Test void malformedAndReplayedTransfersCannotLeaveRetainedState() {
        FragmentAssembler assembler = new FragmentAssembler();
        UUID player = UUID.randomUUID();
        byte[] first = new byte[30_000];
        assertNull(assembler.accept(player, "book:notes", 7, 0, 40_000, first, 0));
        assertThrows(IllegalArgumentException.class, () -> assembler.accept(player, "book:notes", 7, 0, 40_000, first, 1));
        assertEquals(0, assembler.pendingTransfers());
        assertThrows(IllegalArgumentException.class, () -> assembler.accept(player, "book:notes", 7, 1, 40_000, new byte[10_000], 2));
        assertThrows(IllegalArgumentException.class, () -> assembler.accept(player, "book:notes", 7, 0, Integer.MAX_VALUE, first, 3));
        assertNull(assembler.accept(player, "book:notes", 8, 0, 40_000, first, 4));
        assertThrows(IllegalArgumentException.class, () -> assembler.accept(player, "other:packet", 8, 1, 40_000, new byte[10_000], 5));
        assertEquals(0, assembler.pendingTransfers());
    }
    @Test void disconnectAndTimeoutAllowFreshConnectionsAndTransfers() {
        FragmentAssembler assembler = new FragmentAssembler();
        UUID player = UUID.randomUUID();
        byte[] first = new byte[30_000];
        assembler.accept(player, "book:notes", 1, 0, 40_000, first, 0);
        assembler.forget(player);
        assertEquals(0, assembler.pendingTransfers());
        assembler.accept(player, "book:notes", 2, 0, 40_000, first, 10);
        assembler.expire(20_000_000_000L);
        assertEquals(0, assembler.pendingTransfers());
        assembler.accept(player, "book:notes", 3, 0, 40_000, first, 30_000_000_000L);
        assertThrows(IllegalArgumentException.class, () -> assembler.accept(player, "book:notes", 3, 1, 40_000, new byte[10_000], 50_000_000_000L));
        assertEquals(0, assembler.pendingTransfers());
    }
}

package net.magister.liberanimalium.fauna.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;

public final class PairSession {
   public static final String DISPUTE = "territory_contest";
   public static final String PLAY = "play";
   public static final String SPAR = "spar";
   private static final Map<PairKey, PairSession> LIVE = new HashMap<>();
   private static final Map<PairSession.Settlement, Long> SETTLED = new HashMap<>();
   private final PairKey pair;
   private final String kind;
   private final long startedTick;
   private final int hand;
   @Nullable
   private UUID leader;
   private int rounds;
   private long lastProgressTick;
   private boolean loJoined;
   private boolean hiJoined;
   private static volatile long settledGrace = 20L;
   private static long sweptTick = Long.MIN_VALUE;

   private PairSession(PairKey pair, String kind, long startedTick) {
      this.pair = pair;
      this.kind = kind;
      this.startedTick = startedTick;
      this.lastProgressTick = startedTick;
      this.hand = rollHand(pair, startedTick);
   }

   private static int rollHand(PairKey pair, long startedTick) {
      return Seeds.unitPair(pair.lo(), pair.hi(), startedTick) < 0.5F ? 1 : -1;
   }

   public static PairSession open(UUID a, UUID b, String kind, long gameTime) {
      PairKey key = PairKey.of(a, b);
      PairSession existing = LIVE.get(key);
      if (existing != null && existing.kind.equals(kind)) {
         return existing;
      }

      PairSession session = new PairSession(key, kind, gameTime);
      LIVE.put(key, session);
      return session;
   }

   @Nullable
   public static PairSession find(UUID a, UUID b) {
      return LIVE.get(PairKey.of(a, b));
   }

   public static void close(UUID a, UUID b) {
      LIVE.remove(PairKey.of(a, b));
   }

   public static void closeAll(UUID id) {
      LIVE.keySet().removeIf(pair -> pair.involves(id));
   }

   public static void noteSettled(UUID a, UUID b, String kind, long gameTime) {
      SETTLED.put(new PairSession.Settlement(PairKey.of(a, b), kind), gameTime);
   }

   public static void noteLongestStrike(int ticks) {
      settledGrace = Math.max(20L, ticks + 8);
   }

   public static void breakTruce(UUID a, UUID b, String kind) {
      SETTLED.remove(new PairSession.Settlement(PairKey.of(a, b), kind));
   }

   public static void breakTruce(UUID a, UUID b, String kind, long blowGameTime) {
      if (!settledRecently(a, b, kind, blowGameTime, settledGrace)) {
         breakTruce(a, b, kind);
      }
   }

   public static boolean settledRecently(UUID a, UUID b, String kind, long gameTime, long window) {
      Long when = SETTLED.get(new PairSession.Settlement(PairKey.of(a, b), kind));
      return when != null && gameTime - when < window;
   }

   public static void closeStale(long gameTime, long maxIdleTicks, long settledKeepTicks) {
      if (gameTime != sweptTick) {
         sweptTick = gameTime;
         LIVE.entrySet().removeIf(e -> gameTime - e.getValue().lastProgressTick > maxIdleTicks);
         SETTLED.entrySet().removeIf(e -> gameTime - e.getValue() > settledKeepTicks);
      }
   }

   public static void joinAll(UUID party, String kind) {
      for (Entry<PairKey, PairSession> live : LIVE.entrySet()) {
         if (live.getKey().involves(party) && live.getValue().kind.equals(kind)) {
            live.getValue().join(party);
         }
      }
   }

   public static boolean involves(UUID id) {
      for (PairKey key : LIVE.keySet()) {
         if (key.lo().equals(id) || key.hi().equals(id)) {
            return true;
         }
      }

      return false;
   }

   public static void clearAll() {
      sweptTick = Long.MIN_VALUE;
      LIVE.clear();
      SETTLED.clear();
   }

   public String kind() {
      return this.kind;
   }

   public long startedTick() {
      return this.startedTick;
   }

   public int hand() {
      return this.hand;
   }

   @Nullable
   public UUID leader() {
      return this.leader;
   }

   public boolean isLeader(UUID id) {
      return this.leader != null && this.leader.equals(id);
   }

   public float draw() {
      return Seeds.unitPair(this.pair.lo(), this.pair.hi(), this.startedTick + this.kind.hashCode());
   }

   public int rounds() {
      return this.rounds;
   }

   public long lastProgressTick() {
      return this.lastProgressTick;
   }

   public void setLeader(UUID id) {
      if (!this.pair.involves(id)) {
         throw new IllegalArgumentException(id + " cannot lead " + this.pair);
      }

      this.leader = id;
   }

   public void swapLeader() {
      if (this.leader != null) {
         this.leader = this.pair.other(this.leader);
      }
   }

   public void completeRound(long gameTime) {
      this.rounds++;
      this.lastProgressTick = gameTime;
   }

   public void noteProgress(long gameTime) {
      this.lastProgressTick = gameTime;
   }

   public void join(UUID id) {
      if (this.pair.lo().equals(id)) {
         this.loJoined = true;
      } else if (this.pair.hi().equals(id)) {
         this.hiJoined = true;
      }
   }

   public boolean joined(UUID id) {
      return this.pair.lo().equals(id) ? this.loJoined : this.pair.hi().equals(id) && this.hiJoined;
   }

   private record Settlement(PairKey pair, String kind) {
   }
}

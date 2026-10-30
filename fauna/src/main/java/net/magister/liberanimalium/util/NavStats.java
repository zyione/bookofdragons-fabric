package net.magister.liberanimalium.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

public final class NavStats {
   private static final double RATE = 0.025;
   private static final double TICKS_PER_SECOND = 20.0;
   public static final int POCKET_PROBE_NODES = 256;
   public static final int POCKET_UNKNOWN = -2;
   public static final int POCKET_OPEN = -1;
   private static final double NANOS_PER_MILLI = 1000000.0;
   private static final double MICROS_PER_SECOND = 1000000.0;
   private static final AtomicLong SUBMITS = new AtomicLong();
   private static final AtomicLong SOLVES = new AtomicLong();
   private static final AtomicLong STALE = new AtomicLong();
   private static final AtomicLong SKIPPED = new AtomicLong();
   private static final AtomicLong NODES = new AtomicLong();
   private static final AtomicLong NANOS = new AtomicLong();
   private static final Map<String, AtomicLong> OUTCOMES = new ConcurrentHashMap<>();
   private static final AtomicInteger IN_FLIGHT = new AtomicInteger();
   private static double emaSubmits;
   private static double emaSolves;
   private static double emaStale;
   private static double emaSkipped;
   private static double emaNodes;
   private static double emaNanos;
   private static boolean primed;
   private static final Map<String, NavStats.OutcomeStat> DETAIL = new ConcurrentHashMap<>();
   private static final Map<String, NavStats.ModeStat> MODES = new ConcurrentHashMap<>();
   private static final String CAPPED = "NODE_CAP";

   private NavStats() {
   }

   public static void submitted() {
      SUBMITS.incrementAndGet();
      IN_FLIGHT.incrementAndGet();
   }

   public static void solved(long nanos) {
      SOLVES.incrementAndGet();
      NANOS.addAndGet(nanos);
      IN_FLIGHT.decrementAndGet();
   }

   public static void searched(NavStats.Search s) {
      NODES.addAndGet(s.nodes());
      OUTCOMES.computeIfAbsent(s.outcome(), k -> new AtomicLong()).incrementAndGet();
      NavStats.OutcomeStat d = DETAIL.computeIfAbsent(s.outcome(), k -> new NavStats.OutcomeStat());
      d.n.incrementAndGet();
      d.nodes.addAndGet(s.nodes());
      d.goalDistance.add(s.goalDistance());
      d.ball.add(s.ball());
      d.closest.add(s.closest());
      d.closestXz.add(s.closestXz());
      d.closestY.add(s.closestY());
      if (s.goalStandable() != null) {
         (s.goalStandable() ? d.standableYes : d.standableNo).incrementAndGet();
      }

      if (s.goalPocket() == -1) {
         d.pocketOpen.incrementAndGet();
      } else if (s.goalPocket() >= 0) {
         d.pocketClosed.incrementAndGet();
         d.pocketNodes.addAndGet(s.goalPocket());
      }

      if (!Float.isNaN(s.hRatio())) {
         d.hRatio.add(s.hRatio());
         d.hRatioN.incrementAndGet();
      }

      if (!Float.isNaN(s.malusPerBlock())) {
         d.malus.add(s.malusPerBlock());
         d.malusN.incrementAndGet();
      }

      NavStats.ModeStat m = MODES.computeIfAbsent(s.goalMode(), k -> new NavStats.ModeStat());
      m.n.incrementAndGet();
      if ("NODE_CAP".equals(s.outcome())) {
         m.capped.incrementAndGet();
      }
   }

   public static void stale() {
      STALE.incrementAndGet();
   }

   public static void skipped() {
      SKIPPED.incrementAndGet();
      IN_FLIGHT.decrementAndGet();
   }

   public static void roll() {
      long submits = SUBMITS.getAndSet(0L);
      long solves = SOLVES.getAndSet(0L);
      long stale = STALE.getAndSet(0L);
      long skipped = SKIPPED.getAndSet(0L);
      long nodes = NODES.getAndSet(0L);
      long nanos = NANOS.getAndSet(0L);
      if (!primed) {
         emaSubmits = submits;
         emaSolves = solves;
         emaStale = stale;
         emaSkipped = skipped;
         emaNodes = nodes;
         emaNanos = nanos;
         primed = true;
      } else {
         emaSubmits = emaSubmits + (submits - emaSubmits) * 0.025;
         emaSolves = emaSolves + (solves - emaSolves) * 0.025;
         emaStale = emaStale + (stale - emaStale) * 0.025;
         emaSkipped = emaSkipped + (skipped - emaSkipped) * 0.025;
         emaNodes = emaNodes + (nodes - emaNodes) * 0.025;
         emaNanos = emaNanos + (nanos - emaNanos) * 0.025;
      }
   }

   public static double solvesPerSecond() {
      return emaSolves * 20.0;
   }

   public static double submitsPerSecond() {
      return emaSubmits * 20.0;
   }

   public static double stalePerSecond() {
      return emaStale * 20.0;
   }

   public static double skippedPerSecond() {
      return emaSkipped * 20.0;
   }

   public static double nodesPerSolve() {
      return emaSolves <= 0.0 ? 0.0 : emaNodes / emaSolves;
   }

   public static double millisPerSolve() {
      return emaSolves <= 0.0 ? 0.0 : emaNanos / emaSolves / 1000000.0;
   }

   public static double workerLoad(int workers) {
      if (workers <= 0) {
         return 0.0;
      }

      double microsPerSecond = emaNanos * 20.0 / 1000.0;
      return microsPerSecond / (workers * 1000000.0);
   }

   public static int inFlight() {
      return IN_FLIGHT.get();
   }

   public static String outcomeLine() {
      long total = 0L;

      for (AtomicLong n : OUTCOMES.values()) {
         total += n.get();
      }

      if (total == 0L) {
         return "outcomes  none yet";
      }

      StringBuilder out = new StringBuilder("outcomes ");

      for (Entry<String, AtomicLong> e : OUTCOMES.entrySet()) {
         out.append(String.format(Locale.ROOT, " %s %.0f%%", e.getKey(), 100.0 * e.getValue().get() / total));
      }

      return out.toString();
   }

   public static List<String> detailLines() {
      List<String> out = new ArrayList<>();

      for (Entry<String, NavStats.OutcomeStat> e : DETAIL.entrySet()) {
         NavStats.OutcomeStat d = e.getValue();
         long n = d.n.get();
         if (n != 0L) {
            String h = d.hRatioN.get() == 0L ? "hx?" : String.format(Locale.ROOT, "hx%.2f", d.hRatio.sum() / d.hRatioN.get());
            String malus = d.malusN.get() == 0L ? "+? malus/blk" : String.format(Locale.ROOT, "+%.2f malus/blk", d.malus.sum() / d.malusN.get());
            long known = d.standableYes.get() + d.standableNo.get();
            String standable = known == 0L
               ? "goal standable ?"
               : String.format(Locale.ROOT, "goal standable %.0f%% of %d", 100.0 * d.standableYes.get() / known, known);
            long probed = d.pocketClosed.get() + d.pocketOpen.get();
            String pocket = probed == 0L
               ? "goal pocket ?"
               : (
                  d.pocketClosed.get() == 0L
                     ? String.format(Locale.ROOT, "goal pocket closed 0%% of %d", probed)
                     : String.format(
                        Locale.ROOT,
                        "goal pocket closed %.0f%% of %d (avg %d nodes)",
                        100.0 * d.pocketClosed.get() / probed,
                        probed,
                        d.pocketNodes.get() / d.pocketClosed.get()
                     )
               );
            out.add(
               String.format(
                  Locale.ROOT,
                  "nav  %-9s n=%d | %.0f nodes | goal %.0f blk | ball %.0f blk | closest %.0f blk (xz %.0f, y %.0f) | %s | %s | %s | %s",
                  e.getKey(),
                  n,
                  (double)d.nodes.get() / n,
                  d.goalDistance.sum() / n,
                  d.ball.sum() / n,
                  d.closest.sum() / n,
                  d.closestXz.sum() / n,
                  d.closestY.sum() / n,
                  standable,
                  pocket,
                  h,
                  malus
               )
            );
         }
      }

      long total = 0L;

      for (NavStats.ModeStat m : MODES.values()) {
         total += m.n.get();
      }

      if (total > 0L) {
         StringBuilder modes = new StringBuilder("nav  modes    ");

         for (Entry<String, NavStats.ModeStat> e : MODES.entrySet()) {
            long n = e.getValue().n.get();
            if (n != 0L) {
               modes.append(String.format(Locale.ROOT, " %s %.0f%% (capped %.0f%%)", e.getKey(), 100.0 * n / total, 100.0 * e.getValue().capped.get() / n));
            }
         }

         out.add(modes.toString());
      }

      return out;
   }

   public static String line(int workers, int queued) {
      return String.format(
         Locale.ROOT,
         "nav  solves %.1f/s | submits %.1f/s | stale %.1f/s | skipped %.1f/s | %.0f nodes | %.2f ms/solve | workers %.0f%% of %d | queue %d | in flight %d",
         solvesPerSecond(),
         submitsPerSecond(),
         stalePerSecond(),
         skippedPerSecond(),
         nodesPerSolve(),
         millisPerSolve(),
         100.0 * workerLoad(workers),
         workers,
         queued,
         inFlight()
      );
   }

   static void reset() {
      SUBMITS.set(0L);
      SOLVES.set(0L);
      STALE.set(0L);
      SKIPPED.set(0L);
      NODES.set(0L);
      NANOS.set(0L);
      IN_FLIGHT.set(0);
      OUTCOMES.clear();
      DETAIL.clear();
      MODES.clear();
      emaNanos = 0.0;
      emaNodes = 0.0;
      emaSkipped = 0.0;
      emaStale = 0.0;
      emaSolves = 0.0;
      emaSubmits = 0.0;
      primed = false;
   }

   private static final class ModeStat {
      final AtomicLong n = new AtomicLong();
      final AtomicLong capped = new AtomicLong();
   }

   private static final class OutcomeStat {
      final AtomicLong n = new AtomicLong();
      final AtomicLong nodes = new AtomicLong();
      final DoubleAdder goalDistance = new DoubleAdder();
      final DoubleAdder ball = new DoubleAdder();
      final DoubleAdder closest = new DoubleAdder();
      final DoubleAdder closestXz = new DoubleAdder();
      final DoubleAdder closestY = new DoubleAdder();
      final AtomicLong standableYes = new AtomicLong();
      final AtomicLong standableNo = new AtomicLong();
      final AtomicLong pocketClosed = new AtomicLong();
      final AtomicLong pocketOpen = new AtomicLong();
      final AtomicLong pocketNodes = new AtomicLong();
      final DoubleAdder hRatio = new DoubleAdder();
      final AtomicLong hRatioN = new AtomicLong();
      final DoubleAdder malus = new DoubleAdder();
      final AtomicLong malusN = new AtomicLong();
   }

   public record Search(
      int nodes,
      String outcome,
      String goalMode,
      float goalDistance,
      float ball,
      float closest,
      float closestXz,
      float closestY,
      Boolean goalStandable,
      int goalPocket,
      float hRatio,
      float malusPerBlock
   ) {
   }
}

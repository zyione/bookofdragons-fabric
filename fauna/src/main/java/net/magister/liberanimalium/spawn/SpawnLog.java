package net.magister.liberanimalium.spawn;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class SpawnLog {
   private static final Map<String, SpawnLog> BY_HOST = new ConcurrentHashMap<>();
   private long tries;
   private long passes;
   private final Deque<String> last = new ArrayDeque<>();
   private final Map<String, Map<String, Long>> outcomes = new TreeMap<>();

   private SpawnLog() {
   }

   public static SpawnLog of(String host) {
      return BY_HOST.computeIfAbsent(host, h -> new SpawnLog());
   }

   public static void clearAll() {
      BY_HOST.clear();
   }

   public synchronized void tried(boolean passed, String outcome) {
      this.tries++;
      if (passed) {
         this.passes++;
      }

      this.last.addFirst(outcome);

      while (this.last.size() > 20) {
         this.last.removeLast();
      }
   }

   public synchronized void refused(String outcome) {
      this.last.addFirst(outcome);

      while (this.last.size() > 20) {
         this.last.removeLast();
      }
   }

   public synchronized void refused(String rule, String outcome) {
      this.outcomes.computeIfAbsent(rule, r -> new HashMap<>()).merge(outcome, 1L, Long::sum);
   }

   public synchronized Map<String, List<String>> outcomes() {
      Map<String, List<String>> out = new TreeMap<>();
      this.outcomes
         .forEach(
            (rule, counts) -> out.put(
               rule, counts.entrySet().stream().sorted(Entry.<String, Long>comparingByValue().reversed()).map(e -> e.getValue() + " " + e.getKey()).toList()
            )
         );
      return out;
   }

   public synchronized long tries() {
      return this.tries;
   }

   public synchronized double passFraction() {
      return this.tries == 0L ? 0.0 : (double)this.passes / this.tries;
   }

   public synchronized List<String> last() {
      return List.copyOf(this.last);
   }
}

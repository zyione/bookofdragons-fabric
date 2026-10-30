package net.magister.liberanimalium.fauna.debug;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.magister.liberanimalium.fauna.api.HeardSound;
import net.magister.liberanimalium.fauna.api.Hurt;

public final class StimulusLog {
   public static final int LIFETIME_TICKS = 40;
   private static final int CAPACITY = 12;
   private static final Map<UUID, Deque<StimulusLog.Entry>> LOGS = new ConcurrentHashMap<>();

   private StimulusLog() {
   }

   public static void note(UUID animalId, HeardSound sound, float strength) {
      push(
         animalId,
         new StimulusLog.Entry(
            true, sound.kind().ordinal(), sound.position().x, sound.position().y, sound.position().z, strength, sound.tick()
         )
      );
   }

   public static void note(UUID animalId, Hurt hurt, float severity) {
      push(animalId, new StimulusLog.Entry(false, -1, hurt.position().x, hurt.position().y, hurt.position().z, severity, hurt.tick()));
   }

   public static List<FaunaSnapshot.Ping> pings(UUID animalId, long now) {
      Deque<StimulusLog.Entry> log = LOGS.get(animalId);
      if (log != null && !log.isEmpty()) {
         List<FaunaSnapshot.Ping> out = new ArrayList<>(log.size());
         log.removeIf(ex -> now - ex.tick() > 40L);

         for (StimulusLog.Entry e : log) {
            int age = (int)Math.max(0L, now - e.tick());
            out.add(new FaunaSnapshot.Ping(e.sound(), e.kind(), (float)e.x(), (float)e.y(), (float)e.z(), e.strength(), age));
         }

         return out;
      } else {
         return List.of();
      }
   }

   public static void clear(UUID animalId) {
      LOGS.remove(animalId);
   }

   public static void clearAll() {
      LOGS.clear();
   }

   private static void push(UUID animalId, StimulusLog.Entry entry) {
      Deque<StimulusLog.Entry> log = LOGS.computeIfAbsent(animalId, id -> new ArrayDeque<>());
      synchronized (log) {
         while (log.size() >= 12) {
            log.pollFirst();
         }

         log.addLast(entry);
      }
   }

   private record Entry(boolean sound, int kind, double x, double y, double z, float strength, long tick) {
   }
}

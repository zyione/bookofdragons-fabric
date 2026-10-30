package net.magister.liberanimalium.anim.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.fauna.api.ExpressionKey;

public final class ExpressionSchedule {
   public static final long WINDOW_MAX_TICKS = 6000L;
   private final Map<String, Long> openedAt = new HashMap<>();
   private final Map<String, Long> nextAllowed = new HashMap<>();
   private final Map<String, Long> lingerUntil = new HashMap<>();
   private static final String[] OPENING = new String[]{ExpressionKey.Edge.ENTER.word(), ExpressionKey.Edge.BEGIN.word(), ExpressionKey.Edge.RAISE.word()};
   private static final String[] CLOSING = new String[]{ExpressionKey.Edge.EXIT.word(), ExpressionKey.Edge.END.word(), ExpressionKey.Edge.DROP.word()};

   public String accept(String key, long now, EventTable table) {
      String base = baseOf(key);
      if (base != null) {
         if (opens(key)) {
            this.openedAt.put(base, now);
         } else {
            this.openedAt.remove(base);
            this.nextAllowed.remove(key(base, key));
         }
      }

      if (!this.allowed(key, now, table)) {
         return null;
      }

      EventRow row = table.row(key);
      if (row != null && row.lingers()) {
         this.lingerUntil.put(key, now + row.linger());
      }

      return key;
   }

   public List<String> due(long now, EventTable table) {
      if (this.openedAt.isEmpty() && this.lingerUntil.isEmpty()) {
         return List.of();
      }

      List<String> out = new ArrayList<>(1);
      if (!this.lingerUntil.isEmpty()) {
         List<String> closed = null;

         for (Entry<String, Long> window : this.lingerUntil.entrySet()) {
            if (now >= window.getValue()) {
               if (closed == null) {
                  closed = new ArrayList<>(1);
               }

               closed.add(window.getKey());
            } else if (this.allowed(window.getKey(), now, table)) {
               out.add(window.getKey());
            }
         }

         if (closed != null) {
            for (String key : closed) {
               this.lingerUntil.remove(key);
            }
         }
      }

      if (this.openedAt.isEmpty()) {
         return out;
      }

      List<String> expired = null;

      for (Entry<String, Long> window : this.openedAt.entrySet()) {
         if (now - window.getValue() > 6000L) {
            if (expired == null) {
               expired = new ArrayList<>(1);
            }

            expired.add(window.getKey());
         } else {
            for (String opening : OPENING) {
               String key = window.getKey() + "." + opening;
               EventRow row = table.row(key);
               if (row != null && row.repeatEvery() > 0 && this.allowed(key, now, table)) {
                  out.add(key);
               }
            }
         }
      }

      if (expired != null) {
         for (String base : expired) {
            this.openedAt.remove(base);
         }
      }

      return out;
   }

   public boolean isOpen(String base) {
      return this.openedAt.containsKey(base);
   }

   public boolean isLingering(String key) {
      return this.lingerUntil.containsKey(key);
   }

   private boolean allowed(String key, long now, EventTable table) {
      EventRow row = table.row(key);
      if (row == null) {
         return false;
      }

      Long next = this.nextAllowed.get(key);
      if (next != null && now < next) {
         return false;
      }

      int gap = Math.max(row.cooldown(), row.repeatEvery());
      if (gap > 0) {
         this.nextAllowed.put(key, now + gap);
      }

      return true;
   }

   private static boolean opens(String key) {
      for (String word : OPENING) {
         if (key.endsWith("." + word)) {
            return true;
         }
      }

      return false;
   }

   private static String baseOf(String key) {
      for (String word : OPENING) {
         if (key.endsWith("." + word)) {
            return key.substring(0, key.length() - word.length() - 1);
         }
      }

      for (String word : CLOSING) {
         if (key.endsWith("." + word)) {
            return key.substring(0, key.length() - word.length() - 1);
         }
      }

      return null;
   }

   private static String key(String base, String closingKey) {
      for (int i = 0; i < CLOSING.length; i++) {
         if (closingKey.endsWith("." + CLOSING[i])) {
            return base + "." + OPENING[i];
         }
      }

      return closingKey;
   }
}

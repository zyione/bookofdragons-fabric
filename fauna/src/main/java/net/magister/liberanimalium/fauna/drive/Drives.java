package net.magister.liberanimalium.fauna.drive;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Drives {
   public static final List<String> BUILT_IN = List.of(
      "beg",
      "buck",
      "command",
      "enmity",
      "explore",
      "forage",
      "friend",
      "loaf",
      "noise",
      "pack",
      "perch",
      "pilfer",
      "play",
      "prey",
      "rest",
      "rival",
      "space",
      "spar",
      "stuck",
      "threat",
      "wander",
      "warn"
   );
   private static final int MAX = 255;
   private static final List<String> HOST = new CopyOnWriteArrayList<>();

   private Drives() {
   }

   public static synchronized void register(String id) {
      if (!BUILT_IN.contains(id) && !HOST.contains(id)) {
         if (BUILT_IN.size() + HOST.size() >= 255) {
            throw new IllegalStateException("no activity ordinal is left for drive '" + id + "'");
         }

         HOST.add(id);
      } else {
         throw new IllegalArgumentException("drive '" + id + "' is already registered");
      }
   }

   public static List<String> all() {
      if (HOST.isEmpty()) {
         return BUILT_IN;
      }

      List<String> out = new ArrayList<>(BUILT_IN.size() + HOST.size());
      out.addAll(BUILT_IN);
      out.addAll(HOST);
      return List.copyOf(out);
   }

   public static int indexOf(String id) {
      int i = BUILT_IN.indexOf(id);
      if (i >= 0) {
         return i;
      }

      int h = HOST.indexOf(id);
      return h < 0 ? -1 : BUILT_IN.size() + h;
   }

   public static String at(int index) {
      if (index < 0) {
         return null;
      }

      if (index < BUILT_IN.size()) {
         return BUILT_IN.get(index);
      }

      int h = index - BUILT_IN.size();
      return h < HOST.size() ? HOST.get(h) : null;
   }
}

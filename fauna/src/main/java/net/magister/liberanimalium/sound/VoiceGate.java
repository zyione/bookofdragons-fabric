package net.magister.liberanimalium.sound;

import java.util.HashMap;
import java.util.Map;

public final class VoiceGate {
   public static final int GAP_TICKS = 20;
   private final Map<String, Integer> lastStart = new HashMap<>();

   public boolean admit(String voice, int tick) {
      Integer last = this.lastStart.get(voice);
      if (last != null && tick >= last && tick - last < 20) {
         return false;
      }

      this.lastStart.put(voice, tick);
      return true;
   }
}

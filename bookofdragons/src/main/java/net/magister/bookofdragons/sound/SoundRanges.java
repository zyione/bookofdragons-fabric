package net.magister.bookofdragons.sound;

public final class SoundRanges {
   private static final int VOICE = quarterFarther(16);
   private static final int WARNING = halfFarther(32);
   private static final int FURY_SEEN = 80;

   private SoundRanges() {
   }

   public static int of(String event) {
      String e = event;
      if (e.startsWith("nightfury_dive_")) {
         return 80;
      } else if (e.contains("roar") || e.contains("call") || e.contains("dive_")) {
         return WARNING;
      } else if (e.equals("large_flap")) {
         return halfFarther(halfFarther(16));
      } else if (e.equals("medium_flap")) {
         return halfFarther(quarterFarther(16));
      } else if (e.equals("small_flap")) {
         return halfFarther(16);
      } else {
         return e.equals("dragon_whistle") ? 64 : VOICE;
      }
   }

   private static int quarterFarther(int blocks) {
      return blocks * 5 / 4;
   }

   private static int halfFarther(int blocks) {
      return blocks * 3 / 2;
   }
}

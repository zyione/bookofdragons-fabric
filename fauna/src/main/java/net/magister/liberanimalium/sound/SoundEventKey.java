package net.magister.liberanimalium.sound;

import java.util.Locale;

public enum SoundEventKey {
   HURT,
   DEATH,
   FLAP,
   BITE,
   CHARGE,
   SHOOT,
   BLAST,
   ROAR;

   public String voiceName() {
      return this.name().toLowerCase(Locale.ROOT);
   }
}

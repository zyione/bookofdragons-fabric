package net.magister.liberanimalium.fauna.maneuver;

import java.util.Locale;

public enum Situation {
   HUNT,
   FIGHT,
   POSTURE,
   SPACE,
   FLEE,
   TRAVEL,
   IDLE,
   SOCIAL,
   COURT;

   public String id() {
      return this.name().toLowerCase(Locale.ROOT);
   }
}

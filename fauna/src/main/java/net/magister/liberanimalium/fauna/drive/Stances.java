package net.magister.liberanimalium.fauna.drive;

import net.magister.liberanimalium.fauna.api.Stance;

public final class Stances {
   private Stances() {
   }

   public static Stance shownBy(String driveId) {
      if ("threat".equals(driveId)) {
         return Stance.RETREAT;
      } else if ("prey".equals(driveId) || "enmity".equals(driveId)) {
         return Stance.ADVANCE;
      } else if ("rival".equals(driveId) || "warn".equals(driveId)) {
         return Stance.POSTURE;
      } else {
         return "space".equals(driveId) ? Stance.SOFT_REPEL : Stance.NONE;
      }
   }
}

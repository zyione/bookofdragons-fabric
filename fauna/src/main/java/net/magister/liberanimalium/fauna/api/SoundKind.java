package net.magister.liberanimalium.fauna.api;

public enum SoundKind {
   COMBAT,
   CONSTRUCTION,
   VOCAL,
   LOCOMOTION,
   FORAGING,
   CALL,
   ROAR;

   public boolean happening() {
      return this == COMBAT || this == CONSTRUCTION;
   }

   public boolean meal() {
      return this == FORAGING;
   }
}

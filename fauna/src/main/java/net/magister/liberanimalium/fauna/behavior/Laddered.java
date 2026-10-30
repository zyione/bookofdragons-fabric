package net.magister.liberanimalium.fauna.behavior;

public interface Laddered {
   String rungName();

   default boolean quietRung() {
      return false;
   }
}

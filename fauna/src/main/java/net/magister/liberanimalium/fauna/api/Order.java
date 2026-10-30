package net.magister.liberanimalium.fauna.api;

public enum Order {
   STAY,
   FOLLOW,
   FREE;

   public static Order of(net.magister.liberanimalium.chassis.component.command.Stance stance) {
      return values()[stance.ordinal()];
   }
}

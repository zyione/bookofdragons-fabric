package net.magister.liberanimalium.body.grip;

public enum GripState {
   NONE,
   REACHING,
   HOLDING;

   private static final GripState[] VALUES = values();

   public static GripState of(int ordinal) {
      return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NONE;
   }
}

package net.magister.liberanimalium.debug.client;

public final class GripWatch {
   public static volatile boolean watching;
   public static volatile float weight = Float.NaN;

   private GripWatch() {
   }
}

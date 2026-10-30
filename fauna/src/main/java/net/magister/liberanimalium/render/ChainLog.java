package net.magister.liberanimalium.render;

public final class ChainLog {
   private ChainLog() {
   }

   public static void start(int entityId, int ticks) {
      ChainPass.log(entityId, ticks);
   }
}

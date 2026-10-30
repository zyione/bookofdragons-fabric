package net.magister.liberanimalium.event;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.magister.liberanimalium.fauna.core.PairSession;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class PlayBow {
   public static final int CROUCHES = 3;
   public static final int WINDOW_TICKS = 30;
   private static final Map<UUID, PlayBow.Burst> BURSTS = new HashMap<>();

   private PlayBow() {
   }

   public static void onLevelTick(Level level) {
      if (level instanceof ServerLevel server) {
         long now = server.getGameTime();

         for (ServerPlayer person : server.players()) {
            PlayBow.Burst burst = BURSTS.computeIfAbsent(person.getUUID(), id -> new PlayBow.Burst());
            if (burst.crouch(person.isShiftKeyDown(), now)) {
               PairSession.joinAll(person.getUUID(), "play");
            }
         }
      }
   }

   public static void forget(ServerPlayer person) {
      BURSTS.remove(person.getUUID());
   }

   public static final class Burst {
      private final ArrayDeque<Long> downs = new ArrayDeque<>();
      private boolean was;

      public boolean crouch(boolean down, long now) {
         boolean pressed = down && !this.was;
         this.was = down;
         if (!pressed) {
            return false;
         }

         this.downs.addLast(now);

         while (!this.downs.isEmpty() && now - this.downs.peekFirst() > 30L) {
            this.downs.removeFirst();
         }

         if (this.downs.size() < 3) {
            return false;
         }

         this.downs.clear();
         return true;
      }
   }
}

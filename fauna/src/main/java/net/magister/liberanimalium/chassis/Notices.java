package net.magister.liberanimalium.chassis;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class Notices {
   private static final Notices.Sink ACTION_BAR = (who, line) -> who.displayClientMessage(line, true);
   private static volatile Notices.Sink sink = ACTION_BAR;

   private Notices() {
   }

   public static void sink(Notices.Sink host) {
      sink = host == null ? ACTION_BAR : host;
   }

   public static void tell(Player who, Component line) {
      sink.tell(who, line);
   }

   @FunctionalInterface
   public interface Sink {
      void tell(Player var1, Component var2);
   }
}

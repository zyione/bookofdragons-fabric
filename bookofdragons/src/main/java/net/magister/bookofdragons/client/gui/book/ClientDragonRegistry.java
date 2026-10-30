package net.magister.bookofdragons.client.gui.book;

import java.util.Collections;
import java.util.List;
import net.magister.bookofdragons.network.message.SyncTamedDragonsMessage;

public class ClientDragonRegistry {
   private static List<SyncTamedDragonsMessage.TamedDragonRecord> dragons = Collections.emptyList();
   private static int version;

   public static void update(List<SyncTamedDragonsMessage.TamedDragonRecord> incoming) {
      dragons = List.copyOf(incoming);
      version++;
   }

   public static int version() {
      return version;
   }

   public static List<SyncTamedDragonsMessage.TamedDragonRecord> getDragons() {
      return dragons;
   }
}

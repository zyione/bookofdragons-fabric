package net.magister.bookofdragons.client.gui.book;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.network.ControlNetwork;
import net.magister.bookofdragons.network.message.DragonPageMessage;
import net.magister.bookofdragons.network.message.RequestDragonPageMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class ClientDragonPages {
   private static final long ASK_EVERY_MS = 2000L;
   private static final Map<UUID, DragonPageMessage.Page> PAGES = new HashMap<>();
   private static final Map<UUID, Long> ASKED = new HashMap<>();

   private ClientDragonPages() {
   }

   public static void clear() {
      PAGES.clear();
      ASKED.clear();
   }

   public static void put(DragonPageMessage.Page page) {
      PAGES.put(page.uuid(), page);
   }

   public static DragonPageMessage.Page get(UUID uuid) {
      return PAGES.get(uuid);
   }

   public static DragonBase loaded(UUID uuid) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return null;
      }

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof DragonBase dragon && dragon.getUUID().equals(uuid)) {
            return dragon;
         }
      }

      return null;
   }

   public static void ask(UUID uuid) {
      long now = System.currentTimeMillis();
      Long last = ASKED.get(uuid);
      if (last == null || now - last >= 2000L) {
         ASKED.put(uuid, now);
         ControlNetwork.sendToServer(new RequestDragonPageMessage(uuid));
      }
   }
}

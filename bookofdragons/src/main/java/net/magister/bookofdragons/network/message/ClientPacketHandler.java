package net.magister.bookofdragons.network.message;

import net.magister.bookofdragons.client.gui.book.data.SpeciesFacts;
import net.magister.bookofdragons.client.gui.hud.BookStrip;
import net.magister.bookofdragons.client.gui.hud.StripText;
import net.magister.bookofdragons.datagen.data.DragonDiscoveryData;
import net.magister.bookofdragons.discovery.DiscoveryStore;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

public class ClientPacketHandler {
   public static void handleBookStrip(int dragonId, String kind, String act) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         if (mc.level.getEntity(dragonId) instanceof DragonBase dragon) {
            Component line = SpeciesFacts.strip(dragon, kind, act);
            if (line != null) {
               BookStrip.say(line);
            }
         }
      }
   }

   public static void handleStripLine(StripLineMessage.Text text) {
      Component line;
      if (text.key().isEmpty()) {
         line = text.plain();
      } else {
         Component[] args = new Component[text.args().size()];

         for (int i = 0; i < args.length; i++) {
            args[i] = text.args().get(i).plain();
         }

         line = StripText.style(text.key(), args);
      }

      BookStrip.say(line);
   }

   public static void handleLocalStripLine(Player who, StripLineMessage.Text text) {
      if (who == Minecraft.getInstance().player) {
         handleStripLine(text);
      }
   }

   public static void handleSyncDiscoveryData(DragonDiscoveryData data) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         DiscoveryStore.get(player).ifPresent(capability -> {
            int before = factCount(capability);
            capability.copyFrom(data);
            if (factCount(capability) > before && player.tickCount > 20) {
               Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0F));
            }
         });
      }
   }

   private static int factCount(DragonDiscoveryData data) {
      int count = 0;

      for (DragonType type : DragonType.values()) {
         count += data.facts(type).size();
      }

      return count;
   }

   public static void handleDiscoveryLevelUpdate(DragonType dragonType, boolean encountered, boolean killed, boolean tamed, boolean bonded) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         DiscoveryStore.get(player).ifPresent(data -> {
            int oldTier = data.getDiscoveryLevel(dragonType).getTier();
            data.setEncountered(dragonType, encountered);
            data.setKilled(dragonType, killed);
            data.setTamed(dragonType, tamed);
            data.setBonded(dragonType, bonded);
            int newTier = data.getDiscoveryLevel(dragonType).getTier();
            if (newTier > oldTier) {
               Minecraft mc = Minecraft.getInstance();
               mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0F));
               mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F));
            }
         });
      }
   }
}

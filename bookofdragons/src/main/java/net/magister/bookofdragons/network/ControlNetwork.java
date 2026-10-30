package net.magister.bookofdragons.network;

import net.magister.bookofdragons.network.message.BookStripMessage;
import net.magister.bookofdragons.network.message.DiscoveryLevelUpdateMessage;
import net.magister.bookofdragons.network.message.DragonPageMessage;
import net.magister.bookofdragons.network.message.GiveDragonMessage;
import net.magister.bookofdragons.network.message.OpenDragonGUIFromBookMessage;
import net.magister.bookofdragons.network.message.OpenDragonGUIMessage;
import net.magister.bookofdragons.network.message.ReleaseDragonMessage;
import net.magister.bookofdragons.network.message.RequestDragonPageMessage;
import net.magister.bookofdragons.network.message.RequestTamedDragonsMessage;
import net.magister.bookofdragons.network.message.SaveNotesMessage;
import net.magister.bookofdragons.network.message.StripLineMessage;
import net.magister.bookofdragons.network.message.SyncDiscoveryDataMessage;
import net.magister.bookofdragons.network.message.SyncTamedDragonsMessage;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.Channel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class ControlNetwork {
   private static final String PROTOCOL_VERSION = "6";
   private static final Channel INSTANCE = Services.network().channel(new ResourceLocation("bookofdragons", "main"), "6", false);

   public static void register() {
      INSTANCE.toServer(
         "open_dragon_gui_from_book",
         OpenDragonGUIFromBookMessage.class,
         OpenDragonGUIFromBookMessage::encode,
         OpenDragonGUIFromBookMessage::decode,
         OpenDragonGUIFromBookMessage::handle
      );
      INSTANCE.toClient(
         "sync_discovery_data",
         SyncDiscoveryDataMessage.class,
         SyncDiscoveryDataMessage::encode,
         SyncDiscoveryDataMessage::decode,
         SyncDiscoveryDataMessage::handle
      );
      INSTANCE.toClient(
         "discovery_level_update",
         DiscoveryLevelUpdateMessage.class,
         DiscoveryLevelUpdateMessage::encode,
         DiscoveryLevelUpdateMessage::decode,
         DiscoveryLevelUpdateMessage::handle
      );
      INSTANCE.toClient(
         "sync_tamed_dragons", SyncTamedDragonsMessage.class, SyncTamedDragonsMessage::encode, SyncTamedDragonsMessage::decode, SyncTamedDragonsMessage::handle
      );
      INSTANCE.toServer("open_dragon_gui", OpenDragonGUIMessage.class, OpenDragonGUIMessage::encode, OpenDragonGUIMessage::decode, OpenDragonGUIMessage::handle);
      INSTANCE.toServer("save_notes", SaveNotesMessage.class, SaveNotesMessage::encode, SaveNotesMessage::decode, SaveNotesMessage::handle);
      INSTANCE.toServer(
         "request_tamed_dragons",
         RequestTamedDragonsMessage.class,
         RequestTamedDragonsMessage::encode,
         RequestTamedDragonsMessage::decode,
         RequestTamedDragonsMessage::handle
      );
      INSTANCE.toServer(
         "request_dragon_page",
         RequestDragonPageMessage.class,
         RequestDragonPageMessage::encode,
         RequestDragonPageMessage::decode,
         RequestDragonPageMessage::handle
      );
      INSTANCE.toClient("dragon_page", DragonPageMessage.class, DragonPageMessage::encode, DragonPageMessage::decode, DragonPageMessage::handle);
      INSTANCE.toServer("release_dragon", ReleaseDragonMessage.class, ReleaseDragonMessage::encode, ReleaseDragonMessage::decode, ReleaseDragonMessage::handle);
      INSTANCE.toServer("give_dragon", GiveDragonMessage.class, GiveDragonMessage::encode, GiveDragonMessage::decode, GiveDragonMessage::handle);
      INSTANCE.toClient("strip_line", StripLineMessage.class, StripLineMessage::encode, StripLineMessage::decode, StripLineMessage::handle);
      INSTANCE.toClient("book_strip", BookStripMessage.class, BookStripMessage::encode, BookStripMessage::decode, BookStripMessage::handle);
   }

   public static <MSG> void sendToServer(MSG message) {
      INSTANCE.sendToServer(message);
   }

   public static <MSG> void sendToAllClients(MSG message) {
      INSTANCE.sendToAll(message);
   }

   public static <MSG> void sendToClientsTracking(MSG message, Entity entityToTrack) {
      INSTANCE.sendToTracking(message, entityToTrack);
   }

   public static <MSG> void sendToClientsTrackingAndSelf(MSG message, Entity entityToTrack) {
      INSTANCE.sendToTrackingAndSelf(message, entityToTrack);
   }

   public static <MSG> void sendToClientsNear(MSG message, ServerLevel level, double x, double y, double z, double radius) {
      INSTANCE.sendToNear(message, level, x, y, z, radius);
   }

   public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
      INSTANCE.sendToPlayer(message, player);
   }
}

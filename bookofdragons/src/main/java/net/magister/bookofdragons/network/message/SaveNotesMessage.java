package net.magister.bookofdragons.network.message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.magister.bookofdragons.datagen.data.DragonDiscoveryData;
import net.magister.bookofdragons.discovery.DiscoveryStore;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class SaveNotesMessage {
   private final SaveNotesMessage.Subject subject;
   private final String id;
   private final List<String> pages;

   private SaveNotesMessage(SaveNotesMessage.Subject subject, String id, List<String> pages) {
      this.subject = subject;
      this.id = id;
      this.pages = pages;
   }

   public static SaveNotesMessage species(DragonType type, List<String> pages) {
      return new SaveNotesMessage(SaveNotesMessage.Subject.SPECIES, type.name(), pages);
   }

   public static SaveNotesMessage variant(String variant, List<String> pages) {
      return new SaveNotesMessage(SaveNotesMessage.Subject.VARIANT, variant, pages);
   }

   public static SaveNotesMessage dragon(UUID dragon, List<String> pages) {
      return new SaveNotesMessage(SaveNotesMessage.Subject.DRAGON, dragon.toString(), pages);
   }

   public static void encode(SaveNotesMessage message, FriendlyByteBuf buffer) {
      buffer.writeEnum(message.subject);
      buffer.writeUtf(message.id, message.subject.maxId);
      List<String> pages = DragonDiscoveryData.cleanNotes(message.pages);
      buffer.writeVarInt(pages.size());

      for (String page : pages) {
         buffer.writeUtf(page, 1024);
      }
   }

   public static SaveNotesMessage decode(FriendlyByteBuf buffer) {
      SaveNotesMessage.Subject subject = (SaveNotesMessage.Subject)buffer.readEnum(SaveNotesMessage.Subject.class);
      String id = buffer.readUtf(subject.maxId);
      int count = buffer.readVarInt();
      if (count < 0 || count > DragonDiscoveryData.MAX_NOTE_PAGES) throw new IllegalArgumentException("Invalid note page count");
      List<String> pages = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         pages.add(buffer.readUtf(1024));
      }

      return new SaveNotesMessage(subject, id, pages);
   }

   public static void handle(SaveNotesMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         ServerPlayer player = context.sender();
         if (player != null) {
            switch (message.subject) {
               case SPECIES:
                  DragonType type;
                  try {
                     type = DragonType.valueOf(message.id);
                  } catch (IllegalArgumentException e) {
                     return;
                  }

                  DiscoveryStore.get(player).ifPresent(data -> data.setNotes(type, message.pages));
                  break;
               case VARIANT:
                  DiscoveryStore.get(player).ifPresent(data -> {
                     if (data.hasSeenVariant(message.id)) {
                        data.setVariantNotes(message.id, message.pages);
                     }
                  });
                  break;
               case DRAGON:
                  UUID dragon;
                  try {
                     dragon = UUID.fromString(message.id);
                  } catch (IllegalArgumentException e) {
                     return;
                  }

                  DiscoveryStore.get(player).ifPresent(data -> data.setDragonNotes(dragon, message.pages));
            }
         }
      });
   }

   public enum Subject {
      SPECIES(64),
      VARIANT(256),
      DRAGON(36);

      private final int maxId;

      Subject(int maxId) {
         this.maxId = maxId;
      }
   }
}

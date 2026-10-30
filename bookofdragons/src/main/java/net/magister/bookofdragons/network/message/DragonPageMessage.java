package net.magister.bookofdragons.network.message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.magister.bookofdragons.client.gui.book.ClientDragonPages;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class DragonPageMessage {
   private final DragonPageMessage.Page page;

   public DragonPageMessage(DragonPageMessage.Page page) {
      this.page = page;
   }

   public static void encode(DragonPageMessage message, FriendlyByteBuf buffer) {
      DragonPageMessage.Page p = message.page;
      buffer.writeUUID(p.uuid());
      buffer.writeUtf(p.name(), 128);
      buffer.writeUtf(p.type(), 64);
      buffer.writeVarInt(p.stage());
      buffer.writeNbt(p.genetics());
      buffer.writeBoolean(p.live());
      buffer.writeFloat(p.health());
      buffer.writeFloat(p.maxHealth());
      buffer.writeVarInt(p.bond());
      buffer.writeFloat(p.food());
      buffer.writeFloat(p.stamina());
      buffer.writeFloat(p.sleepDebt());
      floats(buffer, p.stats());
      floats(buffer, p.temper());
      acquaintances(buffer, p.met());
      acquaintances(buffer, p.kin());
      DragonPageMessage.Injury injury = p.injury();
      buffer.writeBoolean(injury.down());
      buffer.writeFloat(injury.weaker());
      buffer.writeVarInt(injury.broken().size());

      for (String key : injury.broken()) {
         buffer.writeUtf(key, 64);
      }

      buffer.writeVarInt(injury.mendTicks());
      DragonPageMessage.Where where = p.where();
      buffer.writeBoolean(where.known());
      buffer.writeDouble(where.x());
      buffer.writeDouble(where.y());
      buffer.writeDouble(where.z());
      buffer.writeUtf(where.dimension(), 128);
      buffer.writeBoolean(where.kept());
   }

   public static DragonPageMessage decode(FriendlyByteBuf buffer) {
      UUID uuid = buffer.readUUID();
      String name = buffer.readUtf(128);
      String type = buffer.readUtf(64);
      int stage = buffer.readVarInt();
      CompoundTag genetics = buffer.readNbt();
      boolean live = buffer.readBoolean();
      float health = buffer.readFloat();
      float maxHealth = buffer.readFloat();
      int bond = buffer.readVarInt();
      float food = buffer.readFloat();
      float stamina = buffer.readFloat();
      float sleepDebt = buffer.readFloat();
      float[] stats = floats(buffer);
      float[] temper = floats(buffer);
      List<DragonPageMessage.Acquaintance> met = acquaintances(buffer);
      List<DragonPageMessage.Acquaintance> kin = acquaintances(buffer);
      boolean down = buffer.readBoolean();
      float weaker = buffer.readFloat();
      int brokenCount = Math.min(buffer.readVarInt(), 16);
      List<String> broken = new ArrayList<>(brokenCount);

      for (int i = 0; i < brokenCount; i++) {
         broken.add(buffer.readUtf(64));
      }

      DragonPageMessage.Injury injury = new DragonPageMessage.Injury(down, weaker, broken, buffer.readVarInt());
      DragonPageMessage.Where where = new DragonPageMessage.Where(
         buffer.readBoolean(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readUtf(128), buffer.readBoolean()
      );
      return new DragonPageMessage(
         new DragonPageMessage.Page(
            uuid,
            name,
            type,
            stage,
            genetics == null ? new CompoundTag() : genetics,
            live,
            health,
            maxHealth,
            bond,
            food,
            stamina,
            sleepDebt,
            stats,
            temper,
            met,
            kin,
            injury,
            where
         )
      );
   }

   private static void floats(FriendlyByteBuf buffer, float[] values) {
      buffer.writeVarInt(values.length);

      for (float v : values) {
         buffer.writeFloat(v);
      }
   }

   private static float[] floats(FriendlyByteBuf buffer) {
      float[] out = new float[Math.min(buffer.readVarInt(), 16)];

      for (int i = 0; i < out.length; i++) {
         out[i] = buffer.readFloat();
      }

      return out;
   }

   private static void acquaintances(FriendlyByteBuf buffer, List<DragonPageMessage.Acquaintance> list) {
      buffer.writeVarInt(list.size());

      for (DragonPageMessage.Acquaintance a : list) {
         buffer.writeUUID(a.id());
         buffer.writeUtf(a.name(), 128);
         buffer.writeUtf(a.relation(), 32);
         buffer.writeFloat(a.intensity());
         buffer.writeFloat(a.like());
         buffer.writeFloat(a.fear());
         buffer.writeFloat(a.familiarity());
         buffer.writeVarInt(a.secondsAgo());
      }
   }

   private static List<DragonPageMessage.Acquaintance> acquaintances(FriendlyByteBuf buffer) {
      int count = Math.min(buffer.readVarInt(), 64);
      List<DragonPageMessage.Acquaintance> out = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         out.add(
            new DragonPageMessage.Acquaintance(
               buffer.readUUID(),
               buffer.readUtf(128),
               buffer.readUtf(32),
               buffer.readFloat(),
               buffer.readFloat(),
               buffer.readFloat(),
               buffer.readFloat(),
               buffer.readVarInt()
            )
         );
      }

      return out;
   }

   public static void handle(DragonPageMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         if (Services.platform().isClient()) {
            ClientDragonPages.put(message.page);
         }
      });
   }

   public record Acquaintance(UUID id, String name, String relation, float intensity, float like, float fear, float familiarity, int secondsAgo) {
   }

   public record Injury(boolean down, float weaker, List<String> broken, int mendTicks) {
      public static final DragonPageMessage.Injury NONE = new DragonPageMessage.Injury(false, 0.0F, List.of(), 0);

      public boolean hurt() {
         return this.down || this.weaker > 0.0F || !this.broken.isEmpty();
      }
   }

   public record Page(
      UUID uuid,
      String name,
      String type,
      int stage,
      CompoundTag genetics,
      boolean live,
      float health,
      float maxHealth,
      int bond,
      float food,
      float stamina,
      float sleepDebt,
      float[] stats,
      float[] temper,
      List<DragonPageMessage.Acquaintance> met,
      List<DragonPageMessage.Acquaintance> kin,
      DragonPageMessage.Injury injury,
      DragonPageMessage.Where where
   ) {
   }

   public record Where(boolean known, double x, double y, double z, String dimension, boolean kept) {
      public static final DragonPageMessage.Where UNKNOWN = new DragonPageMessage.Where(false, 0.0, 0.0, 0.0, "", false);
   }
}

package net.magister.liberanimalium.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.attack.AttackManager;
import net.magister.liberanimalium.attack.data.AttackDefinition;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class SyncOmniAttackDefinitionsPacket {
   private final Map<ResourceLocation, AttackDefinition> attacks;

   public SyncOmniAttackDefinitionsPacket(Map<ResourceLocation, AttackDefinition> attacks) {
      this.attacks = attacks;
   }

   public static void encode(SyncOmniAttackDefinitionsPacket message, FriendlyByteBuf buffer) {
      buffer.writeVarInt(message.attacks.size());

      for (Entry<ResourceLocation, AttackDefinition> entry : message.attacks.entrySet()) {
         buffer.writeResourceLocation(entry.getKey());
         Tag encoded = (Tag)AttackDefinition.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue()).resultOrPartial(err -> {
            throw new RuntimeException("Failed to encode AttackDefinition: " + err);
         }).orElseThrow();
         buffer.writeNbt((CompoundTag)encoded);
      }
   }

   public static SyncOmniAttackDefinitionsPacket decode(FriendlyByteBuf buffer) {
      int count = buffer.readVarInt();
      Map<ResourceLocation, AttackDefinition> decoded = new HashMap<>();

      for (int i = 0; i < count; i++) {
         ResourceLocation id = buffer.readResourceLocation();
         CompoundTag tag = buffer.readNbt();
         if (tag != null) {
            AttackDefinition def = (AttackDefinition)AttackDefinition.CODEC.parse(NbtOps.INSTANCE, tag).resultOrPartial(err -> {
               throw new RuntimeException("Failed to decode AttackDefinition: " + err);
            }).orElseThrow();
            decoded.put(id, def);
         }
      }

      return new SyncOmniAttackDefinitionsPacket(decoded);
   }

   public static void handle(SyncOmniAttackDefinitionsPacket message, PacketContext context) {
      context.enqueueWork(() -> AttackManager.getInstance().replaceDefinitions(message.attacks));
   }
}

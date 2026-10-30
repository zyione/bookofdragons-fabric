package net.magister.bookofdragons.datagen.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.compat.Saved;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class DragonRegistryData extends SavedData {
   private static final String DATA_NAME = "bookofdragons_registry";
   private final Map<UUID, List<DragonRegistryEntry>> dragonsByOwner = new ConcurrentHashMap<>();
   private final Map<UUID, DragonRegistryEntry> dragonsByUUID = new ConcurrentHashMap<>();
   private final Map<UUID, DragonRegistryData.Stored> stable = Collections.synchronizedMap(new LinkedHashMap<>());

   public static DragonRegistryData get(MinecraftServer server) {
      DimensionDataStorage storage = server.overworld().getDataStorage();
      return (DragonRegistryData)Saved.getOrCreate(storage, "bookofdragons_registry", DragonRegistryData::new, DragonRegistryData::load);
   }

   public static DragonRegistryData load(CompoundTag tag) {
      DragonRegistryData data = new DragonRegistryData();
      ListTag dragonList = tag.getList("Dragons", 10);

      for (int i = 0; i < dragonList.size(); i++) {
         CompoundTag dragonTag = dragonList.getCompound(i);
         DragonRegistryEntry entry = DragonRegistryEntry.load(dragonTag);
         data.dragonsByUUID.put(entry.getDragonUUID(), entry);
         data.dragonsByOwner.computeIfAbsent(entry.getOwnerUUID(), k -> new ArrayList<>()).add(entry);
      }

      ListTag stableList = tag.getList("Stable", 10);

      for (int i = 0; i < stableList.size(); i++) {
         CompoundTag kept = stableList.getCompound(i);
         UUID id = kept.getUUID("Dragon");
         data.stable.put(id, new DragonRegistryData.Stored(id, kept.getUUID("Owner"), kept.getString("Name"), kept.getCompound("Body")));
      }

      return data;
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag dragonList = new ListTag();

      for (DragonRegistryEntry entry : this.dragonsByUUID.values()) {
         dragonList.add(entry.save());
      }

      tag.put("Dragons", dragonList);
      tag.put("Stable", this.saveStable());
      return tag;
   }

   private ListTag saveStable() {
      ListTag list = new ListTag();
      synchronized (this.stable) {
         for (DragonRegistryData.Stored kept : this.stable.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Dragon", kept.dragon());
            tag.putUUID("Owner", kept.owner());
            tag.putString("Name", kept.name());
            tag.put("Body", kept.body());
            list.add(tag);
         }

         return list;
      }
   }

   public void store(DragonRegistryData.Stored kept) {
      this.stable.put(kept.dragon(), kept);
      this.setDirty();
   }

   @Nullable
   public DragonRegistryData.Stored take(UUID dragon) {
      DragonRegistryData.Stored kept = this.stable.remove(dragon);
      if (kept != null) {
         this.setDirty();
      }

      return kept;
   }

   public boolean isStored(UUID dragon) {
      return this.stable.containsKey(dragon);
   }

   public List<DragonRegistryData.Stored> storedOf(UUID owner) {
      synchronized (this.stable) {
         return this.stable.values().stream().filter(kept -> kept.owner().equals(owner)).toList();
      }
   }

   public void registerDragon(DragonRegistryEntry entry) {
      UUID dragonUUID = entry.getDragonUUID();
      UUID ownerUUID = entry.getOwnerUUID();
      this.dragonsByUUID.put(dragonUUID, entry);
      this.dragonsByOwner.computeIfAbsent(ownerUUID, k -> new ArrayList<>()).add(entry);
      this.setDirty();
   }

   public void updateDragon(UUID dragonUUID, DragonRegistryEntry updatedEntry) {
      DragonRegistryEntry existing = this.dragonsByUUID.get(dragonUUID);
      if (existing != null) {
         updatedEntry.setTamedAt(existing.getTamedAt());
         List<DragonRegistryEntry> ownerDragons = this.dragonsByOwner.get(existing.getOwnerUUID());
         if (ownerDragons != null) {
            ownerDragons.removeIf(e -> e.getDragonUUID().equals(dragonUUID));
         }

         this.dragonsByUUID.put(dragonUUID, updatedEntry);
         this.dragonsByOwner.computeIfAbsent(updatedEntry.getOwnerUUID(), k -> new ArrayList<>()).add(updatedEntry);
         this.setDirty();
      }
   }

   public void removeDragon(UUID dragonUUID) {
      DragonRegistryEntry entry = this.dragonsByUUID.remove(dragonUUID);
      if (entry != null) {
         List<DragonRegistryEntry> ownerDragons = this.dragonsByOwner.get(entry.getOwnerUUID());
         if (ownerDragons != null) {
            ownerDragons.removeIf(e -> e.getDragonUUID().equals(dragonUUID));
            if (ownerDragons.isEmpty()) {
               this.dragonsByOwner.remove(entry.getOwnerUUID());
            }
         }

         this.setDirty();
      }
   }

   public List<DragonRegistryEntry> getDragonsByOwner(UUID ownerUUID) {
      return new ArrayList<>(this.dragonsByOwner.getOrDefault(ownerUUID, Collections.emptyList()));
   }

   public DragonRegistryEntry getDragon(UUID dragonUUID) {
      return this.dragonsByUUID.get(dragonUUID);
   }

   public boolean hasDragon(UUID dragonUUID) {
      return this.dragonsByUUID.containsKey(dragonUUID);
   }

   public record Stored(UUID dragon, UUID owner, String name, CompoundTag body) {
   }
}

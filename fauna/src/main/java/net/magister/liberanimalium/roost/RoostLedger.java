package net.magister.liberanimalium.roost;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class RoostLedger {
   private final Map<UUID, Long> occupants = new LinkedHashMap<>();
   @Nullable
   private UUID placedBy;
   @Nullable
   private BlockPos footprintMin;
   @Nullable
   private BlockPos footprintMax;
   @Nullable
   private UUID conqueror;
   private long conqueredAt;

   public List<UUID> occupants() {
      return List.copyOf(this.occupants.keySet());
   }

   public boolean holds(UUID id) {
      return this.occupants.containsKey(id);
   }

   public int count() {
      return this.occupants.size();
   }

   void add(UUID id, long now) {
      this.occupants.put(id, now);
   }

   boolean remove(UUID id) {
      return this.occupants.remove(id) != null;
   }

   void seen(UUID id, long now) {
      this.occupants.computeIfPresent(id, (k, v) -> now);
   }

   void stampUnseen(long now) {
      this.occupants.replaceAll((id, seen) -> seen <= 0L ? now : seen);
   }

   List<UUID> unseenSince(long before) {
      List<UUID> out = new ArrayList<>();
      this.occupants.forEach((id, seen) -> {
         if (seen < before) {
            out.add(id);
         }
      });
      return out;
   }

   @Nullable
   public UUID placedBy() {
      return this.placedBy;
   }

   void setPlacedBy(@Nullable UUID player) {
      this.placedBy = player;
   }

   @Nullable
   public UUID conqueror() {
      return this.conqueror;
   }

   public long conqueredAt() {
      return this.conqueredAt;
   }

   void setConquest(UUID winner, long at) {
      this.conqueror = winner;
      this.conqueredAt = at;
   }

   @Nullable
   public BlockPos footprintMin() {
      return this.footprintMin;
   }

   @Nullable
   public BlockPos footprintMax() {
      return this.footprintMax;
   }

   void setFootprint(@Nullable BlockPos min, @Nullable BlockPos max) {
      this.footprintMin = min;
      this.footprintMax = max;
   }

   public void save(CompoundTag tag) {
      ListTag list = new ListTag();
      this.occupants.forEach((id, seen) -> {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("Id", id);
         entry.putLong("Seen", seen);
         list.add(entry);
      });
      tag.put("Occupants", list);
      if (this.placedBy != null) {
         tag.putUUID("PlacedBy", this.placedBy);
      }

      if (this.footprintMin != null && this.footprintMax != null) {
         tag.putLong("FootprintMin", this.footprintMin.asLong());
         tag.putLong("FootprintMax", this.footprintMax.asLong());
      }

      if (this.conqueror != null) {
         tag.putUUID("Conqueror", this.conqueror);
         tag.putLong("ConqueredAt", this.conqueredAt);
      }
   }

   public void load(CompoundTag tag) {
      this.occupants.clear();

      for (Tag t : tag.getList("Occupants", 10)) {
         CompoundTag entry = (CompoundTag)t;
         if (entry.hasUUID("Id")) {
            this.occupants.put(entry.getUUID("Id"), entry.getLong("Seen"));
         }
      }

      this.placedBy = tag.hasUUID("PlacedBy") ? tag.getUUID("PlacedBy") : null;
      if (tag.contains("FootprintMin") && tag.contains("FootprintMax")) {
         this.footprintMin = BlockPos.of(tag.getLong("FootprintMin"));
         this.footprintMax = BlockPos.of(tag.getLong("FootprintMax"));
      } else {
         this.footprintMin = null;
         this.footprintMax = null;
      }

      this.conqueror = tag.hasUUID("Conqueror") ? tag.getUUID("Conqueror") : null;
      this.conqueredAt = tag.getLong("ConqueredAt");
   }
}

package net.magister.liberanimalium.fauna.know;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.Relation;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class RelationMemory {
   private final Map<UUID, Relation> standings = new HashMap<>();
   private final Map<UUID, Relation> species = new HashMap<>();

   @Nullable
   public Relation of(UUID id) {
      return this.standings.get(id);
   }

   void remember(UUID id, Relation relation, @Nullable Relation speciesHolds) {
      this.standings.put(id, relation);
      if (speciesHolds != null) {
         this.species.put(id, speciesHolds);
      }

      if (this.standings.size() > 16) {
         UUID weakest = null;
         float weakestIntensity = Float.MAX_VALUE;

         for (Entry<UUID, Relation> entry : this.standings.entrySet()) {
            if (!entry.getKey().equals(id) && entry.getValue().intensity() < weakestIntensity) {
               weakestIntensity = entry.getValue().intensity();
               weakest = entry.getKey();
            }
         }

         if (weakest != null) {
            this.forget(weakest);
         }
      }
   }

   public Map<UUID, RelationMemory.Faded> fade(long elapsed, Predicate<UUID> inSight) {
      if (elapsed > 0L && !this.standings.isEmpty()) {
         double retention = Math.pow(0.85F, elapsed / 24000.0);
         Map<UUID, RelationMemory.Faded> out = new HashMap<>();
         Iterator<Entry<UUID, Relation>> it = this.standings.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, Relation> entry = it.next();
            Relation was = entry.getValue();
            Relation holds = this.species.get(entry.getKey());
            if (holds != null && Bend.isNegative(was.type()) && !inSight.test(entry.getKey())) {
               float floor = holds.type() == was.type() ? holds.intensity() : 0.0F;
               float earned = was.intensity() - floor;
               if (!(earned <= 0.0F)) {
                  float left = (float)(earned * retention);
                  if (left < 0.25F) {
                     it.remove();
                     this.species.remove(entry.getKey());
                     out.put(entry.getKey(), new RelationMemory.Faded(was, null));
                  } else if (left < earned) {
                     Relation now = new Relation(was.type(), floor + left);
                     entry.setValue(now);
                     out.put(entry.getKey(), new RelationMemory.Faded(was, now));
                  }
               }
            }
         }

         return out;
      } else {
         return Map.of();
      }
   }

   public void forget(UUID id) {
      this.standings.remove(id);
      this.species.remove(id);
   }

   public int size() {
      return this.standings.size();
   }

   public void save(CompoundTag tag) {
      ListTag list = new ListTag();

      for (Entry<UUID, Relation> entry : this.standings.entrySet()) {
         CompoundTag row = new CompoundTag();
         row.putUUID("Id", entry.getKey());
         row.putString("Type", entry.getValue().type().name());
         row.putFloat("I", entry.getValue().intensity());
         Relation holds = this.species.get(entry.getKey());
         if (holds != null) {
            row.putString("SpeciesType", holds.type().name());
            row.putFloat("SpeciesI", holds.intensity());
         }

         list.add(row);
      }

      tag.put("Standings", list);
   }

   public void load(CompoundTag tag) {
      this.standings.clear();
      this.species.clear();
      ListTag list = tag.getList("Standings", 10);

      for (int i = 0; i < list.size(); i++) {
         CompoundTag row = list.getCompound(i);
         if (row.hasUUID("Id")) {
            RelationType type = typeOf(row.getString("Type"));
            if (type != null) {
               UUID id = row.getUUID("Id");
               this.standings.put(id, new Relation(type, row.getFloat("I")));
               RelationType holds = row.contains("SpeciesType") ? typeOf(row.getString("SpeciesType")) : null;
               if (holds != null) {
                  this.species.put(id, new Relation(holds, row.getFloat("SpeciesI")));
               }
            }
         }
      }
   }

   @Nullable
   private static RelationType typeOf(String name) {
      for (RelationType candidate : RelationType.values()) {
         if (candidate.name().equals(name)) {
            return candidate;
         }
      }

      return null;
   }

   public record Faded(Relation was, @Nullable Relation now) {
   }
}

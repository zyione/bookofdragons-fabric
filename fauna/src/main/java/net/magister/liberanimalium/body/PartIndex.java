package net.magister.liberanimalium.body;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.magister.liberanimalium.platform.part.PartBody;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public final class PartIndex {
   private final List<PartIndex.Holder> holders = new ArrayList<>();

   public void add(PartIndex.Holder holder) {
      if (!this.holders.contains(holder)) {
         this.holders.add(holder);
      }
   }

   public void remove(PartIndex.Holder holder) {
      this.holders.remove(holder);
   }

   public int size() {
      return this.holders.size();
   }

   public int partCount() {
      int count = 0;

      for (int i = 0; i < this.holders.size(); i++) {
         count += this.holders.get(i).parts().length;
      }

      return count;
   }

   public <P extends Entity> Collection<P> near(AABB box, Collection<P> all) {
      if (all.size() != this.partCount()) {
         return all;
      }

      List<P> out = null;

      for (int i = 0; i < this.holders.size(); i++) {
         PartIndex.Holder holder = this.holders.get(i);
         if (holder.partsUnion().intersects(box)) {
            if (out == null) {
               out = new ArrayList<>();
            }

            for (PartBody<?> part : holder.parts()) {
               out.add((P)part);
            }
         }
      }

      return out == null ? List.of() : out;
   }

   /** Native Fabric queries have no Forge-global part collection to filter. */
   public List<PartBody<?>> nearby(AABB box) {
      List<PartBody<?>> result = new ArrayList<>();
      for (Holder holder : this.holders) {
         if (holder.partsUnion().intersects(box)) {
            for (PartBody<?> part : holder.parts()) {
               if (!part.getParent().isRemoved() && part.getBoundingBox().intersects(box)) result.add(part);
            }
         }
      }
      return result;
   }

   public Entity byId(int id) {
      for (Holder holder : this.holders) {
         for (PartBody<?> part : holder.parts()) {
            if (part.getId() == id && !part.getParent().isRemoved()) return part;
         }
      }
      return null;
   }

   public static AABB unionOf(PartBody<?>[] parts) {
      AABB union = parts[0].getBoundingBox();

      for (int i = 1; i < parts.length; i++) {
         union = union.minmax(parts[i].getBoundingBox());
      }

      return union;
   }

   public interface Holder {
      PartBody<?>[] parts();

      AABB partsUnion();
   }
}

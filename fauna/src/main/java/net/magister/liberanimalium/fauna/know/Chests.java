package net.magister.liberanimalium.fauna.know;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.magister.liberanimalium.fauna.api.ContainerOpened;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class Chests implements Faculty {
   private final List<Chests.Chest> chests = new ArrayList<>();

   @Override
   public String id() {
      return "chests";
   }

   @Override
   public void integrate(FacultyContext ctx) {
      for (ContainerOpened opened : ctx.stimuli(ContainerOpened.class)) {
         if (ctx.handle().terrain().chestTop(opened.at())) {
            this.heard(opened.opener(), opened.at().immutable(), ctx.now());
         }
      }

      this.chests.removeIf(c -> ctx.now() - c.opened() > 24000L);
   }

   @Override
   public void settle(FacultyContext ctx) {
      this.chests.removeIf(chest -> {
         for (Representation rep : ctx.known()) {
            if (rep.id().equals(chest.opener())) {
               return rep.fedSince(chest.opened());
            }
         }

         return false;
      });
   }

   private void heard(UUID opener, BlockPos at, long now) {
      for (int i = 0; i < this.chests.size(); i++) {
         if (this.chests.get(i).at().equals(at)) {
            this.chests.set(i, new Chests.Chest(at, opener, now));
            return;
         }
      }

      if (this.chests.size() >= 4) {
         this.chests.remove(this.chests.stream().min(Comparator.comparingLong(Chests.Chest::opened)).orElseThrow());
      }

      this.chests.add(new Chests.Chest(at, opener, now));
   }

   public List<Chests.Chest> all() {
      return List.copyOf(this.chests);
   }

   public void forget(BlockPos at) {
      this.chests.removeIf(c -> c.at().equals(at));
   }

   @Override
   public void save(CompoundTag tag) {
      ListTag list = new ListTag();

      for (Chests.Chest chest : this.chests) {
         CompoundTag entry = new CompoundTag();
         entry.putLong("At", chest.at().asLong());
         entry.putUUID("Opener", chest.opener());
         entry.putLong("T", chest.opened());
         list.add(entry);
      }

      tag.put("Chests", list);
   }

   @Override
   public void load(CompoundTag tag) {
      this.chests.clear();
      ListTag list = tag.getList("Chests", 10);

      for (int i = 0; i < list.size() && this.chests.size() < 4; i++) {
         CompoundTag entry = list.getCompound(i);
         if (entry.hasUUID("Opener")) {
            this.chests.add(new Chests.Chest(BlockPos.of(entry.getLong("At")), entry.getUUID("Opener"), entry.getLong("T")));
         }
      }
   }

   public record Chest(BlockPos at, UUID opener, long opened) {
      public float belief(long now) {
         double age = Math.max(0L, now - this.opened);
         return (float)Math.pow(0.5, age / 1200.0);
      }
   }
}

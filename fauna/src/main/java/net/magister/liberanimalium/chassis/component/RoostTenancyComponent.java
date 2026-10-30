package net.magister.liberanimalium.chassis.component;

import java.util.ArrayList;
import java.util.List;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.roost.RoostBlockEntity;
import net.magister.liberanimalium.roost.Roosts;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public class RoostTenancyComponent implements IFaunaComponent {
   private FaunaBase host;

   @Override
   public void onAttach(FaunaBase host) {
      this.host = host;
   }

   @Override
   public void serverTick() {
      if (this.host.level() instanceof ServerLevel level && this.host.isAlive()) {
         if (Math.floorMod(level.getGameTime() + this.host.getId(), 20) == 0) {
            BlockPos at = this.host.getRoostPosition();
            if (at != null) {
               this.keep(level, at);
            } else if (!this.host.isBaby() && this.host.species().roosts()) {
               this.search(level);
            }
         }
      }
   }

   private void keep(ServerLevel level, BlockPos at) {
      if (level.isLoaded(at)) {
         if (level.getBlockEntity(at) instanceof RoostBlockEntity block && block.holds(this.host.faunaId())) {
            RoostBlockEntity roost = block.root();
            if (!roost.getBlockPos().equals(at)) {
               this.host.setRoostPosition(roost.getBlockPos());
            }

            if (this.host.isTame() && !roost.placedByPlayer()) {
               roost.release(this.host.faunaId());
               this.host.setRoostPosition(null);
            } else {
               roost.seen(this.host.faunaId());
            }
         } else {
            this.host.setRoostPosition(null);
         }
      }
   }

   private void search(ServerLevel level) {
      for (BlockPos pos : Roosts.nearest(level, this.host.blockPosition())) {
         if (level.getBlockEntity(pos) instanceof RoostBlockEntity roost && roost.claim(this.host)) {
            return;
         }
      }
   }

   public List<String> explain(ServerLevel level) {
      List<String> out = new ArrayList<>();
      if (this.host.isBaby()) {
         out.add("young: the young never claim");
      }

      if (!this.host.species().roosts()) {
         out.add("its species file does not say \"roosts\": true");
      }

      for (BlockPos pos : Roosts.nearest(level, this.host.blockPosition())) {
         String why = level.getBlockEntity(pos) instanceof RoostBlockEntity roost ? roost.refusal(this.host) : "no roost block entity";
         out.add(pos.toShortString() + " - " + (why == null ? "may claim" : why));
      }

      if (out.isEmpty()) {
         out.add("no roost within 96 blocks");
      }

      return out;
   }

   public void searchNow(ServerLevel level) {
      if (this.host.getRoostPosition() == null) {
         this.search(level);
      }
   }

   @Override
   public void save(CompoundTag tag) {
      BlockPos at = this.host.getRoostPosition();
      if (at != null) {
         tag.putInt("RoostX", at.getX());
         tag.putInt("RoostY", at.getY());
         tag.putInt("RoostZ", at.getZ());
      }
   }

   @Override
   public void load(CompoundTag tag) {
      if (tag.contains("RoostX")) {
         this.host.setRoostPosition(new BlockPos(tag.getInt("RoostX"), tag.getInt("RoostY"), tag.getInt("RoostZ")));
      }
   }
}

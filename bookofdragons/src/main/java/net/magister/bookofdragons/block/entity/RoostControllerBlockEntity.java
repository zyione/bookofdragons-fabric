package net.magister.bookofdragons.block.entity;

import net.magister.bookofdragons.block.roost.HabitatModule;
import net.magister.bookofdragons.block.roost.ScaleDecorationModule;
import net.magister.bookofdragons.block.roost.StructureFootprintModule;
import net.magister.liberanimalium.roost.RoostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RoostControllerBlockEntity extends RoostBlockEntity {
   public RoostControllerBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.ROOST_CONTROLLER_ENTITY.get(), pos, state);
      this.addModule(new HabitatModule());
      this.addModule(new StructureFootprintModule());
      this.addModule(new ScaleDecorationModule());
   }

   public String archetype() {
      return ((HabitatModule)this.module(HabitatModule.class)).archetype();
   }

   protected void upgrade(CompoundTag tag) {
      upgradeOldSave(tag);
   }

   static void upgradeOldSave(CompoundTag tag) {
      CompoundTag modules = tag.getCompound("Modules");
      if (tag.contains("Archetype")) {
         CompoundTag habitat = modules.getCompound("habitat");
         habitat.putString("Archetype", tag.getString("Archetype"));
         modules.put("habitat", habitat);
      }

      if (tag.contains("BoundsScanDone")) {
         CompoundTag footprint = modules.getCompound("footprint");
         footprint.putBoolean("Scanned", tag.getBoolean("BoundsScanDone"));
         modules.put("footprint", footprint);
      }

      tag.put("Modules", modules);
      if (!tag.contains("Occupants") && tag.hasUUID("OccupantUUID")) {
         CompoundTag occupant = new CompoundTag();
         occupant.putUUID("Id", tag.getUUID("OccupantUUID"));
         ListTag occupants = new ListTag();
         occupants.add(occupant);
         tag.put("Occupants", occupants);
      }

      if (!tag.contains("FootprintMin") && tag.contains("BoundsMinX")) {
         tag.putLong("FootprintMin", new BlockPos(tag.getInt("BoundsMinX"), tag.getInt("BoundsMinY"), tag.getInt("BoundsMinZ")).asLong());
         tag.putLong("FootprintMax", new BlockPos(tag.getInt("BoundsMaxX"), tag.getInt("BoundsMaxY"), tag.getInt("BoundsMaxZ")).asLong());
      }
   }
}

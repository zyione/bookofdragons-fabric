package net.magister.bookofdragons.block.roost;

import net.magister.liberanimalium.roost.RoostModule;
import net.minecraft.nbt.CompoundTag;

public class HabitatModule implements RoostModule {
   public static final String DEFAULT = "cliff";
   private String archetype = "cliff";

   public String id() {
      return "habitat";
   }

   public String archetype() {
      return this.archetype;
   }

   public void setArchetype(String archetype) {
      this.archetype = archetype != null && !archetype.isEmpty() ? archetype : "cliff";
   }

   public boolean describes(String word) {
      return this.archetype.equals(word);
   }

   public void save(CompoundTag tag) {
      tag.putString("Archetype", this.archetype);
   }

   public void load(CompoundTag tag) {
      this.setArchetype(tag.getString("Archetype"));
   }
}

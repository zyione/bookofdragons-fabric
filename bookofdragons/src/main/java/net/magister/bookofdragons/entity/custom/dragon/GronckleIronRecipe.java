package net.magister.bookofdragons.entity.custom.dragon;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class GronckleIronRecipe {
   public static int REQUIRED_DRIPSTONE = 6;
   public static int REQUIRED_SANDSTONE = 4;
   public static int REQUIRED_IRON_ORE = 2;
   public static int REQUIRED_MAGMA_BLOCK = 2;
   public static int MIN_OUTPUT = 2;
   public static int MAX_OUTPUT = 4;
   public static int IRON_CREATION_COOLDOWN_TICKS = 6000;
   private final Map<Item, Integer> requiredItems = new HashMap<>();
   private final Map<Item, Integer> fedItems = new HashMap<>();

   public GronckleIronRecipe() {
      this.initializeRecipe();
   }

   private void initializeRecipe() {
      this.requiredItems.put(Items.DRIPSTONE_BLOCK, REQUIRED_DRIPSTONE);
      this.requiredItems.put(Items.SANDSTONE, REQUIRED_SANDSTONE);
      this.requiredItems.put(Items.RAW_IRON_BLOCK, REQUIRED_IRON_ORE);
      this.requiredItems.put(Items.MAGMA_BLOCK, REQUIRED_MAGMA_BLOCK);

      for (Item item : this.requiredItems.keySet()) {
         this.fedItems.put(item, 0);
      }
   }

   public boolean isRecipeItem(ItemStack stack) {
      return this.requiredItems.containsKey(stack.getItem());
   }

   public boolean feedItem(ItemStack stack) {
      Item item = stack.getItem();
      if (!this.requiredItems.containsKey(item)) {
         return false;
      }

      int currentAmount = this.fedItems.getOrDefault(item, 0);
      int requiredAmount = this.requiredItems.get(item);
      if (currentAmount >= requiredAmount) {
         return false;
      }

      this.fedItems.put(item, currentAmount + 1);
      return true;
   }

   public boolean isRecipeComplete() {
      for (Entry<Item, Integer> entry : this.requiredItems.entrySet()) {
         Item item = entry.getKey();
         int required = entry.getValue();
         int fed = this.fedItems.getOrDefault(item, 0);
         if (fed < required) {
            return false;
         }
      }

      return true;
   }

   public void reset() {
      this.fedItems.clear();

      for (Item item : this.requiredItems.keySet()) {
         this.fedItems.put(item, 0);
      }
   }

   public int getProgress(Item item) {
      return this.fedItems.getOrDefault(item, 0);
   }

   public int getRequired(Item item) {
      return this.requiredItems.getOrDefault(item, 0);
   }

   public void saveToNBT(CompoundTag tag) {
      CompoundTag recipeTag = new CompoundTag();

      for (Entry<Item, Integer> entry : this.fedItems.entrySet()) {
         String itemName = entry.getKey().getDescriptionId();
         recipeTag.putInt(itemName, entry.getValue());
      }

      tag.put("GronckleIronRecipe", recipeTag);
   }

   public void loadFromNBT(CompoundTag tag) {
      if (tag.contains("GronckleIronRecipe")) {
         CompoundTag recipeTag = tag.getCompound("GronckleIronRecipe");

         for (Item item : this.requiredItems.keySet()) {
            String itemName = item.getDescriptionId();
            if (recipeTag.contains(itemName)) {
               this.fedItems.put(item, recipeTag.getInt(itemName));
            } else {
               this.fedItems.put(item, 0);
            }
         }
      }
   }
}

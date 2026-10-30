package net.magister.bookofdragons.inventory;

import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class DragonInventoryMenu extends AbstractContainerMenu {
   private final Container dragonInventory;
   private final DragonBase dragon;

   public DragonInventoryMenu(int containerId, Inventory playerInventory, DragonBase dragon, Container dragonInventory) {
      super((MenuType)ModMenuTypes.DRAGON_INVENTORY_MENU.get(), containerId);
      this.dragonInventory = dragonInventory;
      this.dragon = dragon;
      if (dragon != null) {
         checkContainerSize(dragonInventory, getDragonInventorySize());
         dragonInventory.startOpen(playerInventory.player);
      }

      this.addDragonSlots();
      this.addPlayerInventory(playerInventory);
      this.addPlayerHotbar(playerInventory);
   }

   public DragonInventoryMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
      this(containerId, playerInventory, getDragonFromBuf(playerInventory.player.level(), extraData), new SimpleContainer(getDragonInventorySize()));
   }

   private static DragonBase getDragonFromBuf(Level level, FriendlyByteBuf buf) {
      int entityId = buf.readInt();
      Entity entity = level.getEntity(entityId);
      return entity instanceof DragonBase ? (DragonBase)entity : null;
   }

   private void addDragonSlots() {
      this.addSlot(new Slot(this.dragonInventory, 0, 4, 36) {
         public boolean mayPlace(ItemStack stack) {
            return stack.is(Items.SADDLE) && (DragonInventoryMenu.this.dragon == null || !DragonInventoryMenu.this.dragon.refusesTack());
         }
      });
      this.addSlot(new Slot(this.dragonInventory, 1, 4, 18) {
         public boolean mayPlace(ItemStack stack) {
            return stack.is(Items.CHEST);
         }

         public int getMaxStackSize() {
            return 1;
         }
      });

      for (int i = 0; i < 15; i++) {
         int x = 80 + i % 5 * 18;
         int y = 18 + i / 5 * 18;
         this.addSlot(new Slot(this.dragonInventory, i + 2, x, y) {
            public boolean isActive() {
               return DragonInventoryMenu.this.dragon != null && DragonInventoryMenu.this.dragon.hasChest();
            }
         });
      }
   }

   private void addPlayerInventory(Inventory playerInventory) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
         }
      }
   }

   private void addPlayerHotbar(Inventory playerInventory) {
      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot != null && slot.hasItem()) {
         ItemStack slotStack = slot.getItem();
         itemstack = slotStack.copy();
         int dragonInventoryEnd = getDragonInventorySize();
         if (index < dragonInventoryEnd) {
            if (!this.moveItemStackTo(slotStack, dragonInventoryEnd, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (slotStack.is(Items.SADDLE)) {
            if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (slotStack.is(Items.CHEST)) {
            if (!this.moveItemStackTo(slotStack, 1, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else {
            if (this.dragon == null || !this.dragon.hasChest()) {
               return ItemStack.EMPTY;
            }

            if (!this.moveItemStackTo(slotStack, 2, dragonInventoryEnd, false)) {
               return ItemStack.EMPTY;
            }
         }

         if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return itemstack;
   }

   public boolean stillValid(Player player) {
      return this.dragon != null && this.dragon.isAlive() && this.dragon.distanceTo(player) < 8.0F;
   }

   public void removed(Player player) {
      super.removed(player);
      this.dragonInventory.stopOpen(player);
   }

   public static int getDragonInventorySize() {
      return 17;
   }

   public DragonBase getDragon() {
      return this.dragon;
   }
}

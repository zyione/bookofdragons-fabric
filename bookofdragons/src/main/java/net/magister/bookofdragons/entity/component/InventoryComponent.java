package net.magister.bookofdragons.entity.component;

import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.inventory.DragonInventoryMenu;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.IFaunaComponent;
import net.magister.liberanimalium.compat.StackIO;
import net.magister.liberanimalium.platform.Services;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class InventoryComponent implements IFaunaComponent, ContainerListener {
   public SimpleContainer inventory;
   protected DragonBase dragon;

   public void onAttach(FaunaBase host) {
      this.dragon = (DragonBase)host;
      this.createInventory();
   }

   public void createInventory() {
      SimpleContainer oldInventory = this.inventory;
      this.inventory = new SimpleContainer(DragonInventoryMenu.getDragonInventorySize());
      if (oldInventory != null) {
         oldInventory.removeListener(this);

         for (int i = 0; i < Math.min(oldInventory.getContainerSize(), this.inventory.getContainerSize()); i++) {
            ItemStack itemstack = oldInventory.getItem(i);
            if (!itemstack.isEmpty()) {
               this.inventory.setItem(i, itemstack.copy());
            }
         }
      }

      this.inventory.addListener(this);
      this.updateEquipment();
   }

   public void updateEquipment() {
      if (!this.dragon.level().isClientSide() && this.inventory != null) {
         this.setSaddled(!this.inventory.getItem(0).isEmpty());
         this.setHasChest(!this.inventory.getItem(1).isEmpty());
      }
   }

   public SimpleContainer getInventory() {
      return this.inventory;
   }

   public void containerChanged(Container pContainer) {
      this.updateEquipment();
   }

   public MenuProvider createMenuProvider() {
      final SimpleContainer slots = this.inventory;
      return new MenuProvider() {
         public Component getDisplayName() {
            return InventoryComponent.this.dragon.getDisplayName();
         }

         public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new DragonInventoryMenu(containerId, playerInventory, InventoryComponent.this.dragon, slots);
         }
      };
   }

   public void openGui(ServerPlayer serverPlayer) {
      if (!this.dragon.level().isClientSide()) {
         Services.menus().open(serverPlayer, this.createMenuProvider(), buf -> buf.writeInt(this.dragon.getId()));
      }
   }

   public boolean hasChest() {
      return (Boolean)this.dragon.getEntityData().get(DragonBase.getHasChestData());
   }

   public void setHasChest(boolean hasChest) {
      this.dragon.getEntityData().set(DragonBase.getHasChestData(), hasChest);
   }

   public boolean isSaddled() {
      return (Boolean)this.dragon.getEntityData().get(DragonBase.getIsSaddledData());
   }

   public void setSaddled(boolean saddled) {
      this.dragon.getEntityData().set(DragonBase.getIsSaddledData(), saddled);
   }

   public void dropEquipment() {
      if (this.inventory != null) {
         for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
               this.dragon.spawnAtLocation(stack);
               this.inventory.setItem(i, ItemStack.EMPTY);
            }
         }
      }

      if (this.isSaddled()) {
         this.dragon.spawnAtLocation(Items.SADDLE);
         this.setSaddled(false);
      }

      if (this.hasChest()) {
         this.dragon.spawnAtLocation(Items.CHEST);
         this.setHasChest(false);
      }
   }

   public void dropSaddle() {
      if (this.isSaddled()) {
         if (this.inventory != null && !this.inventory.getItem(0).isEmpty()) {
            this.inventory.setItem(0, ItemStack.EMPTY);
         }

         this.dragon.spawnAtLocation(Items.SADDLE);
         this.setSaddled(false);
      }
   }

   public void save(CompoundTag tag) {
      if (this.inventory != null) {
         ListTag listtag = new ListTag();

         for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty()) {
               CompoundTag compoundtag = new CompoundTag();
               compoundtag.putByte("Slot", (byte)i);
               StackIO.save(this.dragon, itemstack, compoundtag);
               listtag.add(compoundtag);
            }
         }

         tag.put("Inventory", listtag);
      }
   }

   public void load(CompoundTag tag) {
      if (this.inventory != null) {
         ListTag listtag = tag.getList("Inventory", 10);

         for (int i = 0; i < listtag.size(); i++) {
            CompoundTag compoundtag = listtag.getCompound(i);
            int j = compoundtag.getByte("Slot") & 255;
            if (j < this.inventory.getContainerSize()) {
               this.inventory.setItem(j, StackIO.load(this.dragon, compoundtag));
            }
         }
      }

      this.updateEquipment();
   }
}

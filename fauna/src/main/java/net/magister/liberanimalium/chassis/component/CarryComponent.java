package net.magister.liberanimalium.chassis.component;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.compat.StackIO;
import net.magister.liberanimalium.fauna.api.Delivery;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class CarryComponent implements IFaunaComponent {
   private static final String KEY_CARRIED = "CarriedItem";
   private static final String KEY_VICTIM = "CarriedFrom";
   @Nullable
   private UUID victim;
   private FaunaBase body;
   public static final String KEY_HOARD = "LiberAnimaliumHoard";

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   public boolean isCarrying() {
      return !this.body.getCarriedItem().isEmpty();
   }

   public boolean take(ItemEntity item) {
      if (item != null && item.isAlive() && !this.isCarrying()) {
         ItemStack stack = item.getItem().copy();
         if (stack.isEmpty()) {
            return false;
         }

         if (item.getOwner() instanceof LivingEntity giver) {
            this.body.awardGift(giver, stack, Delivery.GIFT);
         }

         this.body.setCarriedItem(stack);
         item.discard();
         return true;
      } else {
         return false;
      }
   }

   public void stash() {
      if (this.isCarrying() && this.victim == null && !this.body.level().isClientSide()) {
         ItemStack stack = this.body.getCarriedItem();
         this.body.setCarriedItem(ItemStack.EMPTY);
         Vec3 spot = this.body.position();
         ItemEntity dropped = new ItemEntity(this.body.level(), spot.x, spot.y + 0.25, spot.z, stack);
         dropped.setDeltaMovement(Vec3.ZERO);
         dropped.setPickUpDelay(20);
         net.magister.liberanimalium.platform.fabric.EntityData.get(dropped).putUUID("LiberAnimaliumHoard", this.body.getUUID());
         this.body.level().addFreshEntity(dropped);
      }
   }

   public boolean takeFrom(ItemStack stack, UUID from) {
      if (stack != null && !stack.isEmpty() && !this.isCarrying()) {
         this.body.setCarriedItem(stack.split(1));
         this.victim = from;
         return true;
      } else {
         return false;
      }
   }

   @Nullable
   public UUID victim() {
      return this.isCarrying() ? this.victim : null;
   }

   public void swallowed() {
      this.body.setCarriedItem(ItemStack.EMPTY);
      this.victim = null;
   }

   public void dropAt(Vec3 spot) {
      ItemStack stack = this.body.getCarriedItem();
      this.body.setCarriedItem(ItemStack.EMPTY);
      this.victim = null;
      if (!stack.isEmpty() && !this.body.level().isClientSide()) {
         ItemEntity dropped = new ItemEntity(this.body.level(), spot.x, spot.y + 0.25, spot.z, stack);
         dropped.setDeltaMovement(Vec3.ZERO);
         dropped.setPickUpDelay(20);
         this.body.level().addFreshEntity(dropped);
      }
   }

   public void dropHere() {
      if (this.isCarrying()) {
         this.dropAt(this.body.position());
      }
   }

   @Override
   public void save(CompoundTag tag) {
      ItemStack stack = this.body.getCarriedItem();
      if (!stack.isEmpty()) {
         tag.put("CarriedItem", StackIO.save(this.body, stack, new CompoundTag()));
      }

      if (!stack.isEmpty() && this.victim != null) {
         tag.putUUID("CarriedFrom", this.victim);
      }
   }

   @Override
   public void load(CompoundTag tag) {
      this.body.setCarriedItem(tag.contains("CarriedItem") ? StackIO.load(this.body, tag.getCompound("CarriedItem")) : ItemStack.EMPTY);
      this.victim = tag.hasUUID("CarriedFrom") ? tag.getUUID("CarriedFrom") : null;
   }
}

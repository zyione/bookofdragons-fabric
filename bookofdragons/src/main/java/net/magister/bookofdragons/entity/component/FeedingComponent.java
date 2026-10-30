package net.magister.bookofdragons.entity.component;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.CarryComponent;
import net.magister.liberanimalium.chassis.component.IFaunaComponent;
import net.magister.liberanimalium.fauna.api.Delivery;
import net.magister.liberanimalium.fauna.api.ExpressionKey;
import net.magister.liberanimalium.fauna.api.Kindness;
import net.magister.liberanimalium.fauna.api.ExpressionKey.Body;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class FeedingComponent implements IFaunaComponent {
   private ItemEntity eatingItem = null;
   private int eatingTicks = 0;
   private static final int SWALLOW_TICKS = 6;
   private int swallowTicks = 0;
   private boolean eatingCarried;
   protected FaunaBase dragon;

   public void onAttach(FaunaBase host) {
      this.dragon = host;
   }

   public void serverTick() {
      this.tickEating();
   }

   public void save(CompoundTag tag) {
      tag.putInt("EatingTicks", this.eatingTicks);
   }

   public void load(CompoundTag tag) {
      if (tag.contains("EatingTicks")) {
         this.eatingTicks = tag.getInt("EatingTicks");
      }
   }

   public void startEating(ItemEntity foodItem) {
      if (foodItem != null && foodItem.isAlive() && !this.isEating() && this.swallowTicks <= 0) {
         this.eatingItem = foodItem;
         this.eatingTicks = EatWindow.ticks(this.dragon.speciesId());
         this.dragon.express(ExpressionKey.body(Body.EAT_BITE));
      }
   }

   public void startEatingCarried() {
      if (!this.isEating() && this.swallowTicks <= 0 && !this.dragon.getCarriedItem().isEmpty()) {
         this.eatingItem = null;
         this.eatingCarried = true;
         this.eatingTicks = EatWindow.ticks(this.dragon.speciesId());
         this.dragon.express(ExpressionKey.body(Body.EAT_BITE));
      }
   }

   public void tickEating() {
      if (this.eatingTicks <= 0) {
         if (this.swallowTicks > 0) {
            this.swallowTicks--;
         }
      } else if (this.eatingCarried) {
         if (this.dragon.getCarriedItem().isEmpty()) {
            this.eatingCarried = false;
            this.eatingTicks = 0;
         } else {
            if (--this.eatingTicks <= 0) {
               this.consumeCarried();
               this.swallowTicks = 6;
            }
         }
      } else if (this.eatingItem != null && this.eatingItem.isAlive()) {
         if (--this.eatingTicks <= 0) {
            this.consumeFood();
            this.swallowTicks = 6;
         }
      } else {
         this.eatingItem = null;
         this.eatingTicks = 0;
      }
   }

   public void consumeFood() {
      if (this.eatingItem != null && this.eatingItem.isAlive()) {
         ItemStack stack = this.eatingItem.getItem();
         if (this.eatingItem.getOwner() instanceof LivingEntity giver) {
            this.dragon.awardGift(giver, stack, Delivery.DROPPED);
            FaunaMind mind = this.dragon.faunaMind();
            if (mind != null && !this.dragon.level().isClientSide()) {
               mind.perception()
                  .deliver(
                     this.dragon,
                     new Kindness(
                        giver.getUUID(),
                        this.dragon.kindOfAttacker(giver),
                        giver.position(),
                        mind.age(),
                        Delivery.DROPPED,
                        this.dragon.cravingFor(stack),
                        this.dragon.relishFor(stack)
                     )
                  );
            }

            if (!this.dragon.level().isClientSide()) {
               int currentAffection = this.dragon.getBond(giver.getUUID());
               int particleCount = Math.max(1, currentAffection / 40);
               if (this.dragon.level() instanceof ServerLevel serverLevel) {
                  Vec3 at = this.dragon.moodPoint();
                  double spread = FaunaBase.moodSpread();
                  serverLevel.sendParticles(ParticleTypes.HEART, at.x, at.y, at.z, particleCount, spread, spread, spread, 0.1);
               }
            }
         }

         if (this.dragon.getNeedsSystem() != null) {
            this.dragon.getNeedsSystem().eat(stack);
         }

         this.dragon.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
         stack.shrink(1);
         if (stack.isEmpty()) {
            this.eatingItem.discard();
         }

         this.eatingItem = null;
      } else {
         this.eatingItem = null;
      }
   }

   private void consumeCarried() {
      this.eatingCarried = false;
      ItemStack stack = this.dragon.getCarriedItem();
      if (!stack.isEmpty()) {
         if (this.dragon.getNeedsSystem() != null) {
            this.dragon.getNeedsSystem().eat(stack);
         }

         this.dragon.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
         CarryComponent carry = (CarryComponent)this.dragon.componentRegistry.get(CarryComponent.class);
         if (carry != null) {
            carry.swallowed();
         } else {
            this.dragon.setCarriedItem(ItemStack.EMPTY);
         }
      }
   }

   public boolean isEating() {
      return this.eatingTicks > 0;
   }
}

package net.magister.bookofdragons.entity.custom.dragon;

import java.util.UUID;
import mod.azure.azurelib.animation.AzAnimator;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import net.magister.bookofdragons.entity.ability.StingAbility;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.item.ModItems;
import net.magister.liberanimalium.anim.event.EventRow;
import net.magister.liberanimalium.body.stats.FaunaProfile;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.FaunaDispatcher;
import net.magister.liberanimalium.chassis.component.FaunaComponentRegistry;
import net.magister.liberanimalium.chassis.component.melee.OmniMeleeAttackHandler;
import net.magister.liberanimalium.fauna.api.ExpressionKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class SpeedStingerEntity extends DragonBase {
   private static final String CALL_LAYER = "call";
   private static final String POUNCE_LAYER = "pounce";
   private ExpressionKey heldCall;

   public SpeedStingerEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public static Builder setAttributes() {
      return DragonBase.createFaunaAttributes();
   }

   protected FaunaProfile buildProfile() {
      return dragonProfile().canFly(false).canBeRidden(false).canJump(true).opensDoors(true).build();
   }

   protected void registerSpeciesComponents(FaunaComponentRegistry registry) {
      registry.register(OmniMeleeAttackHandler.fromGameplay());
      this.setSecondaryAbility(new StingAbility());
   }

   public void express(ExpressionKey key) {
      if (!this.level().isClientSide() && this.hopping() && this.playsOn(key, "call")) {
         this.heldCall = key;
      } else {
         super.express(key);
      }
   }

   public void playClip(String layer, String fullClip) {
      if ("pounce".equals(layer)) {
         this.clearSlot("call");
      }

      super.playClip(layer, fullClip);
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         this.clearIfDone("call");
         this.clearIfDone("pounce");
      } else if (this.heldCall != null && !this.hopping()) {
         ExpressionKey key = this.heldCall;
         this.heldCall = null;
         super.express(key);
      }
   }

   private boolean playsOn(ExpressionKey key, String layer) {
      EventRow row = this.animEvents().row(key.key());
      return row != null && layer.equals(row.layer());
   }

   private void clearIfDone(String layer) {
      AzAnimator<UUID, FaunaBase> animator = this.getAnimatorOrNull();
      AzAnimationControllerContainer<FaunaBase> container = animator == null ? null : animator.getAnimationControllerContainer();
      AzAnimationController<FaunaBase> controller = container == null ? null : container.getOrNull(layer);
      if (controller != null && controller.currentAnimation() != null && controller.stateMachine().isStopped()) {
         this.clearSlot(layer);
      }
   }

   private void clearSlot(String layer) {
      FaunaDispatcher dispatcher = this.getDispatcher();
      if (dispatcher != null && dispatcher.ready()) {
         dispatcher.cancelSlot(layer);
      }
   }

   @Override
   public DragonType getDragonType() {
      return DragonType.SPEED_STINGER;
   }

   @Override
   public ItemStack getScaleItem() {
      return new ItemStack((ItemLike)ModItems.SPEED_STINGER_SCALES.get());
   }
}

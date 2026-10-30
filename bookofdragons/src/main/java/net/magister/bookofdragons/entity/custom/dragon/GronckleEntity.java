package net.magister.bookofdragons.entity.custom.dragon;

import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.item.ModItems;
import net.magister.bookofdragons.network.Strip;
import net.magister.liberanimalium.body.stats.FaunaProfile;
import net.magister.liberanimalium.chassis.component.FaunaComponentRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GronckleEntity extends DragonBase {
   private GronckleIronRecipe ironRecipe;
   private int ironCreationTimer = -1;

   public GronckleEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.ironRecipe = new GronckleIronRecipe();
   }

   public static Builder setAttributes() {
      return DragonBase.createFaunaAttributes();
   }

   protected FaunaProfile buildProfile() {
      return dragonProfile().build();
   }

   protected void registerSpeciesComponents(FaunaComponentRegistry registry) {
      this.registerJawsAndBreath(registry);
   }

   @Override
   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (!this.level().isClientSide() && this.isTame() && this.ironRecipe.isRecipeItem(itemStack)) {
         boolean consumed = this.ironRecipe.feedItem(itemStack);
         if (consumed) {
            itemStack.shrink(1);
            this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
            if (this.level() instanceof ServerLevel serverLevel) {
               Vec3 at = this.moodPoint();
               serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z, 5, moodSpread(), moodSpread(), moodSpread(), 0.1);
            }

            Strip.say(
               player,
               Component.translatable(
                  "gronckle.recipe.fed",
                  new Object[]{itemStack.getDisplayName(), this.ironRecipe.getProgress(itemStack.getItem()), this.ironRecipe.getRequired(itemStack.getItem())}
               )
            );
            if (this.ironRecipe.isRecipeComplete() && this.ironCreationTimer == -1) {
               this.ironCreationTimer = GronckleIronRecipe.IRON_CREATION_COOLDOWN_TICKS;
               Strip.say(player, Component.translatable("gronckle.recipe.complete.timer"));
            }

            return InteractionResult.SUCCESS;
         } else {
            Strip.say(player, Component.translatable("gronckle.recipe.enough", new Object[]{itemStack.getDisplayName()}));
            return InteractionResult.FAIL;
         }
      } else {
         return super.mobInteract(player, hand);
      }
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.ironCreationTimer >= 0) {
         this.ironCreationTimer--;
         if (this.ironCreationTimer % 100 == 0 && this.level() instanceof ServerLevel serverLevel) {
            Vec3 at = this.moodPoint();
            serverLevel.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 3, moodSpread(), moodSpread(), moodSpread(), 0.02);
         }

         if (this.ironCreationTimer == 0) {
            this.createGronckleIron();
            this.ironCreationTimer = -1;
         }
      }
   }

   private void createGronckleIron() {
      if (!this.level().isClientSide()) {
         int outputAmount = GronckleIronRecipe.MIN_OUTPUT + this.random.nextInt(GronckleIronRecipe.MAX_OUTPUT - GronckleIronRecipe.MIN_OUTPUT + 1);

         for (int i = 0; i < outputAmount; i++) {
            ItemStack ironStack = new ItemStack((ItemLike)ModItems.GRONCKLE_IRON.get(), 1);
            ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), ironStack);
            itemEntity.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.3, 0.2, (this.random.nextDouble() - 0.5) * 0.3);
            this.level().addFreshEntity(itemEntity);
         }

         this.ironRecipe.reset();
         this.playSound(SoundEvents.PLAYER_BURP, 1.5F, 0.3F);
         this.playSound(SoundEvents.LAVA_POP, 2.0F, 1.2F);
         if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.5, this.getZ(), 15, 0.5, 0.3, 0.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.5, this.getZ(), 10, 0.5, 0.3, 0.5, 0.1);
         }

         this.level()
            .players()
            .stream()
            .filter(player -> player.distanceToSqr(this) < 256.0)
            .forEach(player -> Strip.say(player, Component.translatable("gronckle.timer.complete", new Object[]{outputAmount})));
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.ironRecipe != null) {
         this.ironRecipe.saveToNBT(tag);
      }

      tag.putInt("IronCreationTimer", this.ironCreationTimer);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (this.ironRecipe == null) {
         this.ironRecipe = new GronckleIronRecipe();
      }

      this.ironRecipe.loadFromNBT(tag);
      this.ironCreationTimer = tag.getInt("IronCreationTimer");
   }

   @Override
   public DragonType getDragonType() {
      return DragonType.GRONCKLE;
   }

   @Override
   public ItemStack getScaleItem() {
      return new ItemStack((ItemLike)ModItems.GRONCKLE_SCALES.get());
   }
}

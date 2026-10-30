package net.magister.bookofdragons.entity.ability;

import net.magister.bookofdragons.network.Strip;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.ability.AbilityMoment;
import net.magister.liberanimalium.chassis.ability.AbstractFaunaAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class LightningStrikeAbility extends AbstractFaunaAbility {
   private static final int COOLDOWN = 6000;
   private static final int INVISIBILITY_DURATION = 100;
   private static final int LIGHTNING_BOLTS = 1;
   private static final double BOLT_SPREAD = 2.0;
   private static final float BOLT_HEALTH_FRACTION = 0.3F;

   public String getId() {
      return "lightning_strike";
   }

   public int getCooldownTicks() {
      return 6000;
   }

   public float getExhaustionCost() {
      return 4.0F;
   }

   public void onActivate(FaunaBase dragon, @Nullable Player activator) {
      super.onActivate(dragon, activator);
      if (!dragon.level().isClientSide() && dragon.level() instanceof ServerLevel serverLevel) {
         LightningBolt directStrike = (LightningBolt)EntityType.LIGHTNING_BOLT.create(serverLevel);
         if (directStrike != null) {
            directStrike.setPos(dragon.getX(), dragon.getY(), dragon.getZ());
            directStrike.setVisualOnly(true);
            serverLevel.addFreshEntity(directStrike);
         }

         for (int i = 0; i < 0; i++) {
            LightningBolt surroundingStrike = (LightningBolt)EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (surroundingStrike != null) {
               double offsetX = (dragon.getRandom().nextDouble() - 0.5) * 2.0 * 2.0;
               double offsetZ = (dragon.getRandom().nextDouble() - 0.5) * 2.0 * 2.0;
               surroundingStrike.setPos(dragon.getX() + offsetX, dragon.getY(), dragon.getZ() + offsetZ);
               surroundingStrike.setVisualOnly(true);
               serverLevel.addFreshEntity(surroundingStrike);
            }
         }

         for (LivingEntity entity : dragon.level()
            .getEntitiesOfClass(LivingEntity.class, dragon.getBoundingBox().inflate(2.0), entityx -> entityx != dragon && entityx.isAlive())) {
            if (!(entity instanceof Player player && player.isCreative()) && !dragon.getPassengers().contains(entity) && dragon.sees(entity)) {
               entity.hurt(dragon.damageSources().lightningBolt(), 8.0F);
            }
         }

         dragon.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100));
         if (dragon.getControllingPassenger() instanceof Player rider) {
            rider.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100));
         }

         BlockPos at = dragon.blockPosition();
         dragon.level().playSound(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, SoundEvents.TRIDENT_THUNDER, dragon.getSoundSource(), 2.0F, 1.0F);
         if (activator != null) {
            Strip.say(activator, Component.translatable("dragon.ability.lightning.activated", new Object[]{dragon.getDisplayName()}));
         }

         this.setRemainingCooldown(this.getCooldownTicks());
      }
   }

   public boolean shouldAIActivate(FaunaBase dragon, AbilityMoment moment) {
      if (!moment.fleeing()) {
         return false;
      }

      float max = dragon.getMaxHealth();
      return max > 0.0F && dragon.getHealth() / max < 0.3F;
   }
}

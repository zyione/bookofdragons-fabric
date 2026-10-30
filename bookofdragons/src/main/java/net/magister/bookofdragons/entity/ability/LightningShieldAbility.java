package net.magister.bookofdragons.entity.ability;

import java.util.List;
import net.magister.bookofdragons.entity.custom.dragon.SkrillEntity;
import net.magister.bookofdragons.network.Strip;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.ability.AbilityMoment;
import net.magister.liberanimalium.chassis.ability.AbstractFaunaAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class LightningShieldAbility extends AbstractFaunaAbility {
   private static final int COOLDOWN = 2400;
   private static final int DURATION = 200;
   private static final int LIGHTNING_BOLTS = 5;
   private static final double BOLT_SPREAD = 6.0;
   private static final float INITIAL_DAMAGE = 8.0F;
   private static final double DAMAGE_RADIUS = 8.0;
   private static final float THORNS_DAMAGE = 4.0F;
   private static final float DAMAGE_REDUCTION = 0.85F;
   private int remainingDuration = 0;
   private static final int RECENT_VIOLENCE = 100;

   public String getId() {
      return "lightning_shield";
   }

   public int getCooldownTicks() {
      return 2400;
   }

   public float getExhaustionCost() {
      return 4.0F;
   }

   public String getActivationAnimation() {
      return "threaten";
   }

   public int getAnimationLengthTicks() {
      return 200;
   }

   public boolean canActivate(FaunaBase dragon) {
      return !super.canActivate(dragon) ? false : !this.isActive();
   }

   protected boolean worksWet() {
      return false;
   }

   public void onActivate(FaunaBase dragon, @Nullable Player activator) {
      super.onActivate(dragon, activator);
      if (!dragon.level().isClientSide() && dragon.level() instanceof ServerLevel serverLevel) {
         this.active = true;
         this.remainingDuration = 200;
         if (dragon instanceof SkrillEntity skrill) {
            skrill.setLightningShieldActive(true);
         }

         this.spawnLightningStorm(dragon, serverLevel);
         this.dealAreaDamage(dragon);
         BlockPos at = dragon.blockPosition();
         dragon.level().playSound(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, SoundEvents.TRIDENT_THUNDER, dragon.getSoundSource(), 2.0F, 0.8F);
         if (activator != null) {
            Strip.say(activator, Component.translatable("dragon.ability.lightning_shield.activated", new Object[]{dragon.getDisplayName()}));
         }
      }
   }

   private void spawnLightningStorm(FaunaBase dragon, ServerLevel serverLevel) {
      LightningBolt centralBolt = (LightningBolt)EntityType.LIGHTNING_BOLT.create(serverLevel);
      if (centralBolt != null) {
         centralBolt.setPos(dragon.getX(), dragon.getY(), dragon.getZ());
         centralBolt.setVisualOnly(true);
         serverLevel.addFreshEntity(centralBolt);
      }

      for (int i = 0; i < 5; i++) {
         LightningBolt bolt = (LightningBolt)EntityType.LIGHTNING_BOLT.create(serverLevel);
         if (bolt != null) {
            double angle = (Math.PI * 2) * i / 5.0;
            double offsetX = Math.cos(angle) * 6.0;
            double offsetZ = Math.sin(angle) * 6.0;
            bolt.setPos(dragon.getX() + offsetX, dragon.getY(), dragon.getZ() + offsetZ);
            bolt.setVisualOnly(true);
            serverLevel.addFreshEntity(bolt);
         }
      }
   }

   private void dealAreaDamage(FaunaBase dragon) {
      List<LivingEntity> entities = dragon.level()
         .getEntitiesOfClass(LivingEntity.class, dragon.getBoundingBox().inflate(8.0), entityx -> entityx != dragon && entityx.isAlive());
      DamageSource source = dragon.damageSources().lightningBolt();

      for (LivingEntity entity : entities) {
         if (!(entity instanceof Player player && player.isCreative()) && !dragon.getPassengers().contains(entity)) {
            entity.hurt(source, 8.0F);
         }
      }
   }

   public void onTick(FaunaBase dragon) {
      if (this.active) {
         if (!dragon.level().isClientSide()) {
            if (dragon.isInWaterRainOrBubble()) {
               this.onDeactivate(dragon);
               return;
            }

            this.remainingDuration--;
            if (dragon.tickCount % 5 == 0 && dragon.level() instanceof ServerLevel serverLevel) {
               for (int i = 0; i < 3; i++) {
                  double offsetX = (dragon.getRandom().nextDouble() - 0.5) * dragon.getBbWidth() * 2.0;
                  double offsetY = dragon.getRandom().nextDouble() * dragon.getBbHeight();
                  double offsetZ = (dragon.getRandom().nextDouble() - 0.5) * dragon.getBbWidth() * 2.0;
                  serverLevel.sendParticles(
                     ParticleTypes.ELECTRIC_SPARK, dragon.getX() + offsetX, dragon.getY() + offsetY, dragon.getZ() + offsetZ, 1, 0.0, 0.0, 0.0, 0.05
                  );
               }
            }

            if (dragon.tickCount % 20 == 0) {
               dragon.level().playSound(null, dragon.blockPosition(), SoundEvents.BEACON_AMBIENT, dragon.getSoundSource(), 1.0F, 2.0F);
            }

            if (this.remainingDuration <= 0) {
               this.onDeactivate(dragon);
            }
         } else {
            for (int i = 0; i < 2; i++) {
               dragon.level()
                  .addParticle(
                     ParticleTypes.ELECTRIC_SPARK,
                     dragon.getRandomX(1.5),
                     dragon.getRandomY() + 0.5,
                     dragon.getRandomZ(1.5),
                     (dragon.getRandom().nextDouble() - 0.5) * 0.5,
                     dragon.getRandom().nextDouble() * 0.5,
                     (dragon.getRandom().nextDouble() - 0.5) * 0.5
                  );
            }
         }
      }
   }

   public float onDamaged(FaunaBase dragon, DamageSource source, @Nullable LivingEntity attacker, float damage) {
      if (!this.active) {
         return damage;
      }

      if (attacker != null && attacker != dragon && attacker.isAlive() && !source.is(DamageTypes.THORNS)) {
         attacker.hurt(dragon.damageSources().thorns(dragon), 4.0F);
         if (dragon.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
               ParticleTypes.ELECTRIC_SPARK, attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.5, attacker.getZ(), 10, 0.3, 0.3, 0.3, 0.1
            );
         }
      }

      return damage * 0.14999998F;
   }

   public void onDeactivate(FaunaBase dragon) {
      boolean wasActive = this.active;
      this.active = false;
      this.remainingDuration = 0;
      if (dragon instanceof SkrillEntity skrill) {
         skrill.setLightningShieldActive(false);
      }

      if (wasActive) {
         this.setRemainingCooldown(this.getCooldownTicks());
         if (dragon.getControllingPassenger() instanceof Player player) {
            Strip.say(player, Component.translatable("dragon.ability.lightning_shield.deactivated", new Object[]{dragon.getDisplayName()}));
         }

         if (!dragon.level().isClientSide()) {
            dragon.level().playSound(null, dragon.blockPosition(), SoundEvents.BEACON_DEACTIVATE, dragon.getSoundSource(), 1.0F, 1.5F);
         }
      }
   }

   public boolean shouldAIActivate(FaunaBase dragon, AbilityMoment moment) {
      LivingEntity target = moment.target();
      return target != null && !(dragon.distanceTo(target) > 8.0) ? dragon.tickCount - dragon.getLastHurtByMobTimestamp() < 100 : false;
   }

   public boolean shouldAIDeactivate(FaunaBase dragon, AbilityMoment moment) {
      if (this.remainingDuration <= 0) {
         return true;
      }

      AABB checkArea = dragon.getBoundingBox().inflate(12.0);
      List<LivingEntity> nearbyHostiles = dragon.level()
         .getEntitiesOfClass(LivingEntity.class, checkArea, e -> e != dragon && e.isAlive() && !dragon.getPassengers().contains(e));
      return nearbyHostiles.isEmpty();
   }
}

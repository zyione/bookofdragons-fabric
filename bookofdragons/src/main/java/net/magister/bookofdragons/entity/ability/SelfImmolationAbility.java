package net.magister.bookofdragons.entity.ability;

import net.magister.bookofdragons.entity.custom.dragon.MonstrousnightmareEntity;
import net.magister.bookofdragons.network.Strip;
import net.magister.liberanimalium.attack.data.LightConfig;
import net.magister.liberanimalium.attack.light.LightScribe;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.ability.AbilityMoment;
import net.magister.liberanimalium.chassis.ability.AbstractFaunaAbility;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.magister.liberanimalium.compat.Entities;
import net.magister.liberanimalium.config.WorldRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SelfImmolationAbility extends AbstractFaunaAbility {
   private static final int COOLDOWN = 100;
   private static final int MIN_FUEL_TO_ACTIVATE = 0;
   private static final float DAMAGE_PER_TICK = 2.0F;
   private static final int FIRE_SECONDS = 5;
   private static final double EFFECT_RADIUS = 3.0;
   private static final LightConfig COAT_LIGHT = new LightConfig(14, true, false, 2, 4, 2.0F, 6);
   private static final int IMMOLATE_WINDUP = 60;

   public String getId() {
      return "self_immolation";
   }

   public int getCooldownTicks() {
      return 100;
   }

   public float getExhaustionCost() {
      return 0.0F;
   }

   public boolean canActivate(FaunaBase dragon) {
      if (!super.canActivate(dragon)) {
         return false;
      }

      OmniAttackHandler handler = (OmniAttackHandler)dragon.componentRegistry.get(OmniAttackHandler.class);
      return handler == null ? false : handler.getCurrentFuel() >= 0;
   }

   protected boolean worksWet() {
      return false;
   }

   public void onActivate(FaunaBase dragon, @Nullable Player activator) {
      this.active = true;
      if (dragon instanceof MonstrousnightmareEntity nightmare) {
         nightmare.setSelfImmolating(true);
      }

      if (activator != null) {
         Strip.say(activator, Component.translatable("dragon.ability.immolation.activated", new Object[]{dragon.getDisplayName()}));
      }
   }

   public void onTick(FaunaBase dragon) {
      if (this.active) {
         if (dragon.componentRegistry.get(OmniAttackHandler.class) == null) {
            this.onDeactivate(dragon);
         } else {
            if (!dragon.level().isClientSide()) {
               if (dragon.isInWaterRainOrBubble()) {
                  this.onDeactivate(dragon);
                  return;
               }

               if (dragon.getControllingPassenger() == null && dragon.engagedTarget() != null && !dragon.engagedTarget().isAlive()) {
                  this.onDeactivate(dragon);
                  return;
               }

               for (LivingEntity entity : dragon.level().getEntitiesOfClass(LivingEntity.class, dragon.getBoundingBox().inflate(3.0))) {
                  if (entity != dragon && !(entity instanceof Player p && p.isCreative())) {
                     entity.hurt(dragon.damageSources().lava(), 2.0F);
                     Entities.ignite(entity, 5);
                  }
               }

               if (dragon.onGround() && dragon.getRandom().nextFloat() < 0.2F && WorldRules.maySetFire(dragon.level())) {
                  BlockPos firePos = dragon.blockPosition();
                  if (BaseFireBlock.canBePlacedAt(dragon.level(), firePos, dragon.getDirection())) {
                     dragon.level().setBlockAndUpdate(firePos, BaseFireBlock.getState(dragon.level(), firePos));
                  }
               }

               lightTheWorld(dragon);
               if (dragon.getNeedsSystem() != null) {
                  dragon.getNeedsSystem().addExhaustion(0.04F);
               }
            } else {
               for (int i = 0; i < 2; i++) {
                  dragon.level().addParticle(ParticleTypes.FLAME, dragon.getRandomX(1.5), dragon.getRandomY() + 1.0, dragon.getRandomZ(1.5), 0.0, 0.1, 0.0);
                  dragon.level().addParticle(ParticleTypes.LAVA, dragon.getRandomX(1.5), dragon.getRandomY() + 1.0, dragon.getRandomZ(1.5), 0.0, 0.0, 0.0);
               }
            }
         }
      }
   }

   private static void lightTheWorld(FaunaBase dragon) {
      if (dragon.tickCount % COAT_LIGHT.everyTicks() == 0 && dragon.level() instanceof ServerLevel server) {
         int var6 = coatLightKey(dragon);
         Vec3 middle = dragon.getBoundingBox().getCenter();
         Vec3 half = Vec3.directionFromRotation(0.0F, dragon.yBodyRot).scale(dragon.getBbWidth() * 0.5);
         LightScribe scribe = LightScribe.of(server);
         scribe.illuminate(server, middle, var6, var6, COAT_LIGHT, 1.0F);
         scribe.illuminate(server, middle.add(half), var6, var6, COAT_LIGHT, 1.0F);
         scribe.illuminate(server, middle.subtract(half), var6, var6, COAT_LIGHT, 1.0F);
      }
   }

   private static int coatLightKey(FaunaBase dragon) {
      return ~dragon.getId();
   }

   public void onDeactivate(FaunaBase dragon) {
      boolean wasActive = this.active;
      super.onDeactivate(dragon);
      if (dragon.level() instanceof ServerLevel server) {
         LightScribe.of(server).release(server, coatLightKey(dragon));
      }

      if (dragon instanceof MonstrousnightmareEntity nightmare) {
         nightmare.setSelfImmolating(false);
      }

      if (wasActive && dragon.getControllingPassenger() instanceof Player player) {
         Strip.say(player, Component.translatable("dragon.ability.immolation.deactivated", new Object[]{dragon.getDisplayName()}));
      }
   }

   public boolean shouldAIActivate(FaunaBase dragon, AbilityMoment moment) {
      return dragon.componentRegistry.get(OmniAttackHandler.class) == null
         ? false
         : moment.target() != null && moment.target().isAlive() && moment.committedFor(60) && !moment.spent();
   }

   public boolean shouldAIDeactivate(FaunaBase dragon, AbilityMoment moment) {
      return dragon.componentRegistry.get(OmniAttackHandler.class) == null ? true : moment.target() == null || !moment.target().isAlive();
   }
}

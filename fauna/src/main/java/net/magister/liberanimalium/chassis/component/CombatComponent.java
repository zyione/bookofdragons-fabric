package net.magister.liberanimalium.chassis.component;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.data.CrippledFeature;
import net.magister.liberanimalium.body.stats.BondLevel;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.Notices;
import net.magister.liberanimalium.chassis.ability.AbilityMoment;
import net.magister.liberanimalium.chassis.ability.FaunaAbility;
import net.magister.liberanimalium.chassis.component.melee.CombatReach;
import net.magister.liberanimalium.compat.Melee;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.progression.FaunaInteractionEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class CombatComponent implements IFaunaComponent {
   public FaunaAbility secondaryAbility;
   protected FaunaBase body;
   private boolean heldByMind;

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   @Override
   public void serverTick() {
      if (!this.body.isIncapacitated()) {
         if (this.secondaryAbility != null) {
            this.secondaryAbility.tickCooldown();
            if (this.secondaryAbility.isActive()) {
               this.secondaryAbility.onTick(this.body);
            }
         }
      }
   }

   @Override
   public void clientTick() {
      if (this.secondaryAbility != null && this.secondaryAbility.isActive()) {
         this.secondaryAbility.onTick(this.body);
      }
   }

   @Override
   public void save(CompoundTag tag) {
      if (this.secondaryAbility != null) {
         tag.putString("SecondaryAbility", this.secondaryAbility.getId());
      }
   }

   @Override
   public void load(CompoundTag tag) {
   }

   public boolean doHurtTarget(Entity pEntity) {
      if (this.body.isTame() && this.body.isFriendlyTarget(pEntity)) {
         return false;
      }

      if (!CombatReach.unblocked(this.body, this.body.eyePosition(), pEntity)) {
         return false;
      }

      float attackDamage = (float)this.body.getAttributeValue(Attributes.ATTACK_DAMAGE);
      float knockback = (float)this.body.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
      DamageSource source = this.body.damageSources().mobAttack(this.body);
      attackDamage = Melee.damage(this.body, pEntity, source, attackDamage);
      knockback = Melee.knockback(this.body, pEntity, source, knockback);
      boolean flag = pEntity.hurt(source, attackDamage);
      if (flag) {
         if (knockback > 0.0F && pEntity instanceof LivingEntity living) {
            living.knockback(
               knockback * 0.5F,
               Mth.sin(this.body.getYRot() * (float) (Math.PI / 180.0)),
               -Mth.cos(this.body.getYRot() * (float) (Math.PI / 180.0))
            );
            this.body.setDeltaMovement(this.body.getDeltaMovement().multiply(0.6, 1.0, 0.6));
         }

         Melee.postAttack(this.body, pEntity, source);
         if (this.body.getNeedsSystem() != null) {
            this.body.getNeedsSystem().onAttack();
         }
      }

      return flag;
   }

   public boolean killedEntity(ServerLevel pLevel, LivingEntity pEntity) {
      if (this.body.getNeedsSystem().isHunting() && this.body.species().relationTo(this.body.kindOf(pEntity)).type() == RelationType.EATS) {
         this.body.getNeedsSystem().feed(10);
         AABB searchBox = pEntity.getBoundingBox().inflate(3.0);

         for (ItemEntity itemEntity : pLevel.getEntitiesOfClass(ItemEntity.class, searchBox)) {
            if (this.body.getNeedsSystem().isEdibleKillDrop(itemEntity.getItem())) {
               this.body.getNeedsSystem().eat(itemEntity.getItem());
               itemEntity.discard();
               int currentAffection = 0;
               if (this.body.isTame() && this.body.getOwnerUUID() != null) {
                  currentAffection = this.body.getBond(this.body.getOwnerUUID());
               }

               int particleCount = Math.max(1, currentAffection / 40);
               pLevel.sendParticles(ParticleTypes.HEART, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), particleCount, 0.5, 0.5, 0.5, 0.1);
            }
         }
      }

      return true;
   }

   public boolean elect(AbilityMoment moment) {
      if (this.secondaryAbility == null || !this.secondaryAbility.isAIEnabled()) {
         return false;
      } else if (this.secondaryAbility.isActive()) {
         return this.heldByMind && this.secondaryAbility.shouldAIDeactivate(this.body, moment)
            ? this.activate(null) == CombatComponent.Activation.DEACTIVATED
            : false;
      } else {
         return this.secondaryAbility.shouldAIActivate(this.body, moment) && this.activate(null) == CombatComponent.Activation.ACTIVATED;
      }
   }

   public void activateSecondaryAbility(Player player) {
      if (this.secondaryAbility != null) {
         if (!this.secondaryAbility.bypassesAffectionRequirement() && this.body.getBond(player.getUUID()) < BondLevel.CURIOUS.getMinAffection()) {
            this.say(player, "liberanimalium.ability.not_bonded");
         } else if (!IncapacitationComponent.refuses(this.body, player, CrippledFeature.SECONDARY_ABILITY)) {
            switch (this.activate(player)) {
               case COOLDOWN:
                  this.say(player, "liberanimalium.ability.cooldown");
                  break;
               case REFUSED:
                  this.say(player, "liberanimalium.ability.cannot_use");
                  break;
               case NO_STAMINA:
                  this.say(player, "liberanimalium.ability.no_stamina");
            }
         }
      }
   }

   public CombatComponent.Activation activate(@Nullable Player activator) {
      if (this.secondaryAbility == null) {
         return CombatComponent.Activation.NONE;
      }

      if (this.secondaryAbility.isActive()) {
         this.secondaryAbility.onDeactivate(this.body);
         this.heldByMind = false;
         return CombatComponent.Activation.DEACTIVATED;
      }

      if (!this.secondaryAbility.canActivate(this.body)) {
         return this.secondaryAbility.getRemainingCooldown() > 0 ? CombatComponent.Activation.COOLDOWN : CombatComponent.Activation.REFUSED;
      }

      if (this.body.getStaminaSystem() != null && !this.body.getStaminaSystem().tryConsume(15.0F)) {
         return CombatComponent.Activation.NO_STAMINA;
      }

      this.secondaryAbility.onActivate(this.body, activator);
      this.heldByMind = activator == null && this.secondaryAbility.isActive();
      FaunaInteractionEvents.post(FaunaInteractionEvents.Type.WITNESSED_ABILITY, activator, this.body, this.secondaryAbility.getId());
      return CombatComponent.Activation.ACTIVATED;
   }

   private void say(Player player, String key) {
      Notices.tell(player, Component.translatable(key, new Object[]{this.body.getDisplayName()}));
   }

   public FaunaAbility getSecondaryAbility() {
      return this.secondaryAbility;
   }

   public void setSecondaryAbility(FaunaAbility ability) {
      this.secondaryAbility = ability;
   }

   public int getSecondaryAbilityRemainingCooldown() {
      return this.secondaryAbility != null ? this.secondaryAbility.getRemainingCooldown() : 0;
   }

   public int getSecondaryAbilityMaxCooldown() {
      return this.secondaryAbility != null ? this.secondaryAbility.getCooldownTicks() : 0;
   }

   public enum Activation {
      ACTIVATED,
      DEACTIVATED,
      NONE,
      COOLDOWN,
      REFUSED,
      NO_STAMINA;
   }
}

package net.magister.bookofdragons.entity.ability;

import net.magister.bookofdragons.entity.projectile.SpikeProjectile;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.ability.AbilityMoment;
import net.magister.liberanimalium.chassis.ability.AbstractFaunaAbility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SpikeBarrageAbility extends AbstractFaunaAbility {
   private static final int COOLDOWN = 200;
   private static final int SPIKE_MIN = 3;
   private static final int SPIKE_MAX = 7;
   private static final float SPIKE_SPEED = 3.0F;
   private static final float SPIKE_SPREAD = 8.0F;
   private static final double SPIKE_DAMAGE = 6.0;
   private static final int BARRAGE_WINDUP = 30;

   public String getId() {
      return "spike_barrage";
   }

   public int getCooldownTicks() {
      return 200;
   }

   public float getExhaustionCost() {
      return 2.0F;
   }

   public String getActivationAnimation() {
      return "animation.deadlynadder.tailshot";
   }

   public int getAnimationLengthTicks() {
      return 25;
   }

   public void onActivate(FaunaBase dragon, @Nullable Player activator) {
      super.onActivate(dragon, activator);
      if (!dragon.level().isClientSide()) {
         Vec3 tailPos = dragon.position().subtract(dragon.getViewVector(1.0F).scale(dragon.getBbWidth())).add(0.0, dragon.getBbHeight() * 0.7, 0.0);
         Vec3 aimDirection;
         if (activator != null) {
            aimDirection = activator.getViewVector(1.0F);
         } else if (dragon.engagedTarget() != null) {
            LivingEntity target = dragon.engagedTarget();
            aimDirection = target.getEyePosition().subtract(tailPos).normalize();
         } else {
            aimDirection = dragon.getViewVector(1.0F);
         }

         if (dragon.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, tailPos.x, tailPos.y, tailPos.z, 10, 0.35, 0.25, 0.35, 0.02);
         }

         int spikeCount = 3 + dragon.getRandom().nextInt(5);

         for (int i = 0; i < spikeCount; i++) {
            SpikeProjectile spike = activator != null ? new SpikeProjectile(dragon.level(), activator) : new SpikeProjectile(dragon.level(), dragon);
            spike.setPos(tailPos);
            spike.shoot(aimDirection.x, aimDirection.y, aimDirection.z, 3.0F, 8.0F);
            spike.setBaseDamage(6.0);
            dragon.level().addFreshEntity(spike);
            dragon.level().playSound(null, dragon.blockPosition(), SoundEvents.SCAFFOLDING_BREAK, dragon.getSoundSource(), 1.5F, 0.5F + dragon.getRandom().nextFloat() * 0.2F);
         }

         this.setRemainingCooldown(this.getCooldownTicks());
      }
   }

   public boolean shouldAIActivate(FaunaBase dragon, AbilityMoment moment) {
      LivingEntity target = moment.target();
      if (target == null) {
         return false;
      } else if (moment.committedFor(30) && !moment.fleeing()) {
         double distance = dragon.distanceTo(target);
         return distance > 5.0 && distance < 20.0 && dragon.hasLineOfSight(target);
      } else {
         return false;
      }
   }
}

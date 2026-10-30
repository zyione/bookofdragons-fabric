package net.magister.bookofdragons.entity.ability;

import java.util.ArrayList;
import java.util.List;
import net.magister.liberanimalium.attack.AttackManager;
import net.magister.liberanimalium.attack.data.AttackDefinition;
import net.magister.liberanimalium.attack.data.ImpactConfig;
import net.magister.liberanimalium.attack.entity.AttackProjectileEntity;
import net.magister.liberanimalium.attack.impact.ImpactEffect;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.ability.AbilityMoment;
import net.magister.liberanimalium.chassis.ability.AbstractFaunaAbility;
import net.magister.liberanimalium.network.ClearAttackParticlesMessage;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.sound.FaunaSoundHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class GasIgniteAbility extends AbstractFaunaAbility {
   private static final int COOLDOWN = 40;
   private static final int IGNITE_WINDOW = 30;
   private static final double IGNITE_RANGE = 16.0;
   private static final String GAS_BREATH_ID = "bookofdragons:gas_breath";
   private static final double BLAST_SPACING = 3.0;
   private static final ResourceLocation GAS_IGNITION = new ResourceLocation("bookofdragons", "gas_ignition");

   public String getId() {
      return "gas_ignite";
   }

   public int getCooldownTicks() {
      return 40;
   }

   public float getExhaustionCost() {
      return 1.0F;
   }

   public String getActivationAnimation() {
      return "";
   }

   public int getAnimationLengthTicks() {
      return 12;
   }

   public boolean bypassesAffectionRequirement() {
      return true;
   }

   public void onActivate(FaunaBase dragon, @Nullable Player activator) {
      super.onActivate(dragon, activator);
      Vec3 dragonPos = dragon.position();
      FaunaSoundHelper.play(dragon, "ignition");
      if (dragon.level() instanceof ServerLevel serverLevel) {
         Vec3 headPos = dragonPos.add(0.0, 1.0, 0.0);

         for (int i = 0; i < 15; i++) {
            double xOffset = (dragon.getRandom().nextDouble() - 0.5) * 2.0;
            double yOffset = (dragon.getRandom().nextDouble() - 0.5) * 1.0;
            double zOffset = (dragon.getRandom().nextDouble() - 0.5) * 2.0;
            serverLevel.sendParticles(
               ParticleTypes.CRIT, headPos.x + xOffset, headPos.y + yOffset, headPos.z + zOffset, 1, 0.0, 0.0, -4.0, 0.02
            );
         }
      }

      if (!dragon.level().isClientSide()) {
         AABB searchArea = new AABB(
            dragonPos.x - 16.0,
            dragonPos.y - 8.0,
            dragonPos.z - 16.0,
            dragonPos.x + 16.0,
            dragonPos.y + 8.0,
            dragonPos.z + 16.0
         );
         List<AttackProjectileEntity> nearbyGasProjectiles = dragon.level().getEntitiesOfClass(AttackProjectileEntity.class, searchArea);
         AttackDefinition igniteConfig = AttackManager.getInstance().getAttack(GAS_IGNITION);
         boolean lit = false;
         List<Vec3> blasts = new ArrayList<>();

         for (AttackProjectileEntity gasProjectile : nearbyGasProjectiles) {
            if ("bookofdragons:gas_breath".equals(gasProjectile.getAttackId()) && !gasProjectile.isRemoved()) {
               if (igniteConfig != null) {
                  detonate(gasProjectile, igniteConfig);
                  showBlast(gasProjectile, igniteConfig, blasts);
               } else {
                  gasProjectile.discard();
               }

               lit = true;
            }
         }

         if (lit && dragon.level() instanceof ServerLevel serverLevel) {
            FaunaNetwork.sendToClientsNear(
               new ClearAttackParticlesMessage(
                  new ResourceLocation("bookofdragons:gas_breath"), dragonPos.x, dragonPos.y, dragonPos.z, 16.0F
               ),
               serverLevel,
               dragonPos.x,
               dragonPos.y,
               dragonPos.z,
               80.0
            );
         }
      }

      this.setRemainingCooldown(this.getCooldownTicks());
   }

   public boolean shouldAIActivate(FaunaBase dragon, AbilityMoment moment) {
      if (!moment.breathJustStopped(30)) {
         return false;
      }

      Vec3 dragonPos = dragon.position();
      AABB searchArea = new AABB(
         dragonPos.x - 16.0,
         dragonPos.y - 8.0,
         dragonPos.z - 16.0,
         dragonPos.x + 16.0,
         dragonPos.y + 8.0,
         dragonPos.z + 16.0
      );

      for (AttackProjectileEntity gasProjectile : dragon.level().getEntitiesOfClass(AttackProjectileEntity.class, searchArea)) {
         if ("bookofdragons:gas_breath".equals(gasProjectile.getAttackId()) && !gasProjectile.isRemoved()) {
            return true;
         }
      }

      return false;
   }

   private static void showBlast(AttackProjectileEntity gasProjectile, AttackDefinition igniteConfig, List<Vec3> blasts) {
      if (gasProjectile.level() instanceof ServerLevel serverLevel) {
         Vec3 var7 = gasProjectile.position();

         for (Vec3 shown : blasts) {
            if (shown.distanceToSqr(var7) < 9.0) {
               return;
            }
         }

         blasts.add(var7);
         gasProjectile.showImpact(serverLevel, igniteConfig, GAS_IGNITION);
      }
   }

   private static void detonate(AttackProjectileEntity gasProjectile, AttackDefinition igniteConfig) {
      boolean consumed = false;

      for (ImpactConfig impact : igniteConfig.impact()) {
         ImpactEffect effect = ImpactEffect.byType(impact.type());
         if (effect != null && effect.apply(gasProjectile, null, impact)) {
            consumed = true;
            break;
         }
      }

      if (!consumed) {
         gasProjectile.discard();
      }
   }
}

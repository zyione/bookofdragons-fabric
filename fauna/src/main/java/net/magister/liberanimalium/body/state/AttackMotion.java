package net.magister.liberanimalium.body.state;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.melee.IMeleeAttackHandler;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;

public enum AttackMotion {
   NONE,
   CHARGE_START,
   CHARGE_LOOP,
   FIRING_PROJ,
   WINDUP,
   BREATHING,
   BITING;

   public static AttackMotion fromOrdinal(int ordinal) {
      return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : NONE;
   }

   public static AttackMotion of(FaunaBase body) {
      OmniAttackHandler handler = body.componentRegistry.get(OmniAttackHandler.class);
      if (handler != null && handler.hasAttack()) {
         if (handler.isStream()) {
            if (handler.isWindingUp()) {
               return WINDUP;
            }

            if (handler.isBreathing()) {
               return BREATHING;
            }
         } else {
            if (handler.isFiring()) {
               return FIRING_PROJ;
            }

            if (handler.isCharging()) {
               if (handler.getAimingTicks() < handler.getChargeBuildupTicks()) {
                  return CHARGE_START;
               }

               return CHARGE_LOOP;
            }
         }
      }

      IMeleeAttackHandler melee = body.componentRegistry.get(IMeleeAttackHandler.class);
      return melee != null && melee.isAttacking() ? BITING : NONE;
   }

   public static void publish(FaunaBase body) {
      body.setLayerAttackOrdinal(of(body).ordinal());
   }
}

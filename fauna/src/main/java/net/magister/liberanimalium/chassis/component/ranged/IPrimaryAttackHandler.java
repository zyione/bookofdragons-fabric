package net.magister.liberanimalium.chassis.component.ranged;

import net.magister.liberanimalium.chassis.component.IFaunaComponent;

public interface IPrimaryAttackHandler extends IFaunaComponent {
   boolean isAttacking();

   void handlePlayerAttack(boolean var1);

   boolean canAttack();
}

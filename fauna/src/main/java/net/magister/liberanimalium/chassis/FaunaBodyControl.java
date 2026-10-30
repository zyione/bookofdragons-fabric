package net.magister.liberanimalium.chassis;

import net.magister.liberanimalium.body.locomotion.WalkEngine;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;

public class FaunaBodyControl extends BodyRotationControl {
   private final Mob mob;

   public FaunaBodyControl(Mob mob) {
      super(mob);
      this.mob = mob;
   }

   public void clientTick() {
      this.mob.yBodyRot = WalkEngine.travelFacing(this.mob, this.mob.getControllingPassenger() != null);
      if (this.isMoving()) {
         this.mob.yHeadRot = Mth.rotateIfNecessary(this.mob.yHeadRot, this.mob.yBodyRot, this.mob.getMaxHeadYRot());
      }
   }

   private boolean isMoving() {
      return this.mob.getX() != this.mob.xOld || this.mob.getZ() != this.mob.zOld;
   }
}

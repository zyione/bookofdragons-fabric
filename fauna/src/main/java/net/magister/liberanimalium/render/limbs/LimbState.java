package net.magister.liberanimalium.render.limbs;

import java.util.function.UnaryOperator;
import net.magister.liberanimalium.anim.rig.FootPlant;
import net.magister.liberanimalium.anim.rig.FootStep;
import net.magister.liberanimalium.anim.rig.GatherCache;
import net.minecraft.world.phys.Vec3;

public final class LimbState {
   public int tick = Integer.MIN_VALUE;
   public double groundY = Double.NaN;
   public Vec3 correction = Vec3.ZERO;
   public Vec3 footTarget;
   public Vec3 footTargetO;
   public boolean clipRegime;
   public Vec3 offset = Vec3.ZERO;
   public Vec3 offsetO = Vec3.ZERO;
   public boolean reaching;
   public final FootPlant plant = new FootPlant();
   public boolean planted;
   public boolean down = true;
   public int landedTick = Integer.MIN_VALUE;
   public final FootStep step = new FootStep();
   public double swingGround = Double.NaN;
   public double pinAbove;
   public int clipTicks;
   public Vec3 homePrev;
   public Vec3 releasePivot;
   public int releaseTick;
   public double liftedAbove = Double.NaN;
   public final GatherCache gathers = new GatherCache(1);
   public final LimbTrace trace = new LimbTrace();

   public void ride(UnaryOperator<Vec3> ground) {
      if (this.footTarget != null) {
         Vec3 moved = ground.apply(this.footTarget);
         if (!Double.isNaN(this.groundY)) {
            this.groundY = this.groundY + (moved.y - this.footTarget.y);
         }

         this.footTarget = moved;
      }

      if (this.homePrev != null) {
         this.homePrev = ground.apply(this.homePrev);
      }

      if (this.releasePivot != null) {
         this.releasePivot = ground.apply(this.releasePivot);
      }

      if (this.plant.pin != null) {
         this.plant.pin = ground.apply(this.plant.pin);
      }

      if (this.step.pin != null) {
         this.step.pin = ground.apply(this.step.pin);
      }

      if (this.step.sole != null) {
         this.step.sole = ground.apply(this.step.sole);
      }

      if (this.step.start != null) {
         this.step.start = ground.apply(this.step.start);
      }

      if (this.step.target != null) {
         this.step.target = ground.apply(this.step.target);
      }
   }
}

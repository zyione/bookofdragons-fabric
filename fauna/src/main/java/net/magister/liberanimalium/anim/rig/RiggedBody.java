package net.magister.liberanimalium.anim.rig;

import java.util.List;
import net.magister.liberanimalium.anim.api.AnimatedBody;
import net.minecraft.world.entity.Entity;

public interface RiggedBody extends AnimatedBody {
   RigSpec animRig();

   default Footing animFooting() {
      Entity e = this.animEntity();
      return e == null ? null : Footing.of(e);
   }

   default StepsSpec animSteps() {
      return StepsSpec.EMPTY;
   }

   default List<String> animPartBones() {
      return List.of();
   }

   default Entity animEntity() {
      return this instanceof Entity e ? e : null;
   }

   default boolean animStepsAllowed() {
      return true;
   }

   default boolean animStepsMoving() {
      return false;
   }

   default boolean animGrounded() {
      Entity e = this.animEntity();
      return e != null && e.onGround();
   }

   default Object animAttitude() {
      return null;
   }

   default Object animLimbReach() {
      return null;
   }

   int animTick();
}

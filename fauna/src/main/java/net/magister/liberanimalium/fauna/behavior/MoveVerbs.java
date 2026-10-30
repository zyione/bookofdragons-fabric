package net.magister.liberanimalium.fauna.behavior;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public interface MoveVerbs {
   void goTo(BlockPos var1, float var2);

   default void cruise(BlockPos destination, float urgency) {
      this.goTo(destination, urgency);
   }

   void bolt(BlockPos var1, float var2);

   void flee(List<Vec3> var1, float var2);

   default void fleeing(UUID from, float fear) {
   }

   void engage(UUID var1, double var2, double var4, float var6);

   void chase(UUID var1, double var2, double var4, float var6);

   void stalk(UUID var1, double var2, double var4, float var6);

   void display(UUID var1, double var2, double var4, float var6);

   void back(UUID var1, double var2, double var4, float var6);

   void escort(UUID var1, double var2, double var4, float var6);

   void alight(float var1);

   void standBy(UUID var1, double var2);

   void unstick();

   void changeMode();

   void stop();

   boolean idle();

   boolean failed();

   double remaining();

   default boolean doorAhead() {
      return false;
   }
}

package net.magister.liberanimalium.fauna.maneuver;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public interface Waypoints {
   boolean goTo(BlockPos var1, float var2);

   void pace(float var1);

   void gait(boolean var1);

   boolean fly(List<Vec3> var1, float var2);

   void loiter(Vec3 var1, double var2, int var4, float var5);

   void stop();

   boolean idle();

   boolean failed();

   boolean solving();
}

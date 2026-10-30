package net.magister.liberanimalium.fauna.know;

import net.minecraft.world.phys.Vec3;

public interface PositionEstimator {
   Vec3 estimate(Representation var1, long var2, float var4);
}

package net.magister.liberanimalium.body.state;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class RiderMotion {
   private RiderMotion() {
   }

   public static Vec3 of(FaunaBase body) {
      if (!body.isControlledByLocalInstance() && body.getControllingPassenger() instanceof Player) {
         Vector3f sent = (Vector3f)body.getEntityData().get(FaunaBase.DATA_RIDER_MOTION);
         return new Vec3(sent.x(), sent.y(), sent.z());
      } else {
         return body.getDeltaMovement();
      }
   }

   public static void set(FaunaBase body, float x, float y, float z) {
      body.getEntityData().set(FaunaBase.DATA_RIDER_MOTION, new Vector3f(x, y, z));
   }
}

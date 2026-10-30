package net.magister.liberanimalium.anim.rig;

import java.util.function.UnaryOperator;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public interface Footing {
   double x();

   double y();

   double z();

   double yo();

   Vec3 groundOld();

   float yaw();

   float yawO();

   boolean onGround();

   boolean held(double var1);

   double height();

   GroundOracle oracle();

   default boolean reaches() {
      return true;
   }

   @Nullable
   UnaryOperator<Vec3> ride();

   @Nullable
   default UnaryOperator<Vec3> rideBetween(float partial) {
      return null;
   }

   Vec3 drawn(Vec3 var1, float var2);

   Matrix4f modelToStand(Matrix4f var1, float var2);

   Vec3 toWorld(Vec3 var1);

   default Vec3 position() {
      return new Vec3(this.x(), this.y(), this.z());
   }

   static Footing of(Entity entity) {
      return new EntityFooting(entity);
   }
}

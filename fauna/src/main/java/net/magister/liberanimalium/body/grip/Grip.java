package net.magister.liberanimalium.body.grip;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Grip {
   private Grip() {
   }

   @Nullable
   public static FaunaBase holderOf(Entity entity) {
      return entity instanceof Held held ? held.liberanimalium$holder() : null;
   }

   public static void thrown(Entity entity, float drag) {
      if (entity instanceof Held held) {
         Held.Pin pin = held.liberanimalium$pin();
         pin.thrown = 100;
         pin.drag = drag;
         pin.flyX = entity.getDeltaMovement().x;
         pin.flyZ = entity.getDeltaMovement().z;
      }
   }

   private static void fly(Entity entity, Held.Pin pin) {
      if (pin.thrown > 0) {
         if (entity instanceof LivingEntity living
            && !entity.onGround()
            && !entity.horizontalCollision
            && !entity.isInWater()
            && !entity.isInLava()
            && !entity.isPassenger()
            && !living.isFallFlying()) {
            pin.thrown--;
            Vec3 kept = thrownVelocity(entity.getDeltaMovement(), new Vec3(pin.flyX, 0.0, pin.flyZ), pin.drag);
            entity.setDeltaMovement(kept);
            pin.flyX = kept.x;
            pin.flyZ = kept.z;
         } else {
            pin.thrown = 0;
         }
      }
   }

   static Vec3 thrownVelocity(Vec3 now, Vec3 before, double drag) {
      double wanted = Math.sqrt(before.x * before.x + before.z * before.z) * drag;
      double level = Math.sqrt(now.x * now.x + now.z * now.z);
      if (!(level >= wanted) && !(level < 1.0E-9)) {
         double back = wanted / level;
         return new Vec3(now.x * back, now.y, now.z * back);
      } else {
         return now;
      }
   }

   public static boolean held(Entity entity) {
      return holderOf(entity) != null;
   }

   public static void pin(Entity entity) {
      if (entity instanceof Held held) {
         if (held.liberanimalium$holder() == null) {
            fly(entity, held.liberanimalium$pin());
         } else {
            Held.Pin pin = held.liberanimalium$pin();
            if (pin.placed) {
               boolean placesItself = entity instanceof Player && !entity.level().isClientSide();
               if (!placesItself) {
                  entity.setPos(pin.x, pin.y, pin.z);
                  entity.xo = entity.xOld = pin.xO;
                  entity.yo = entity.yOld = pin.yO;
                  entity.zo = entity.zOld = pin.zO;
               }

               entity.setOnGround(false);
               entity.fallDistance = 0.0F;
               entity.setDeltaMovement(pin.vx, pin.vy, pin.vz);
               entity.noPhysics = true;
               if (entity instanceof LivingEntity living) {
                  living.yBodyRot = pin.yaw;
                  living.yBodyRotO = pin.yawO;
                  if (!(entity instanceof Player)) {
                     entity.setYRot(pin.yaw);
                     entity.yRotO = pin.yawO;
                  }

                  living.walkAnimation.setSpeed(0.0F);
                  living.walkAnimation.update(0.0F, 1.0F);
               }
            }
         }
      }
   }
}

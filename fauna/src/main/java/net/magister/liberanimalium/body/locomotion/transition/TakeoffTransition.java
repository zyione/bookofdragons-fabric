package net.magister.liberanimalium.body.locomotion.transition;

import net.magister.liberanimalium.body.locomotion.LocomotionMode;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class TakeoffTransition implements TransitionPrimitive {
   private static final double HEAD_MARGIN = 0.1;
   private int ticksRemaining;
   private int leapTicks;
   private double launchY;

   public static int headroom(FaunaBase body) {
      return PhysicsSheet.of(body).takeoff().headroom();
   }

   public static int headroom(String host) {
      return PhysicsSheet.forHost(host).takeoff().headroom();
   }

   public static int headroomTop(double bodyHeight, int headroom) {
      return Mth.floor(bodyHeight + 0.1) + headroom - 1;
   }

   public static int launchWindowTicks(FaunaBase body) {
      return PhysicsSheet.of(body).takeoff().launchWindowTicks();
   }

   @Override
   public boolean precondition(FaunaBase body) {
      return hasHeadroomAbove(body, headroom(body));
   }

   @Override
   public boolean ownsPhysics() {
      return this.leapTicks > 0;
   }

   @Override
   public void begin(FaunaBase body) {
      this.ticksRemaining = launchWindowTicks(body);
      this.leapTicks = PhysicsSheet.of(body).takeoff().leapTicks();
      this.launchY = body.getY();
      body.setOnGround(false);
   }

   @Override
   public boolean tick(FaunaBase body) {
      if (this.leapTicks > 0) {
         this.leapTicks--;
         PhysicsSheet.Takeoff takeoff = PhysicsSheet.of(body).takeoff();
         if (body.getY() - this.launchY < takeoff.leapHeight()) {
            Vec3 velocity = body.getDeltaMovement();
            body.setDeltaMovement(velocity.x, takeoff.leapSpeed(), velocity.z);
            body.move(MoverType.SELF, body.getDeltaMovement());
            return false;
         }

         this.leapTicks = 0;
      }

      return !body.onGround() || --this.ticksRemaining <= 0;
   }

   @Override
   public void interrupt(FaunaBase body) {
      this.ticksRemaining = 0;
   }

   @Override
   public String animSignal() {
      return "TAKEOFF";
   }

   @Override
   public LocomotionMode fallbackMode() {
      return LocomotionMode.WALK;
   }

   public static boolean hasHeadroomAbove(FaunaBase body, int blocks) {
      BlockPos head = BlockPos.containing(body.getX(), body.getBoundingBox().maxY + 0.1, body.getZ());

      for (int i = 0; i < blocks; i++) {
         BlockPos check = head.above(i);
         if (!body.level().getBlockState(check).getCollisionShape(body.level(), check).isEmpty()) {
            return false;
         }
      }

      return true;
   }
}

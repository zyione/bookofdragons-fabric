package net.magister.liberanimalium.chassis.component;

import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DiveBreakComponent implements IFaunaComponent {
   protected FaunaBase body;

   public static PhysicsSheet.DiveBreak tuning(FaunaBase body) {
      return PhysicsSheet.of(body).diveBreak();
   }

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   @Override
   public void serverTick() {
   }

   @Override
   public void save(CompoundTag tag) {
   }

   @Override
   public void load(CompoundTag tag) {
   }

   public float calculateGroundDistance() {
      Vec3 velocity = this.body.getDeltaMovement();
      double speed = velocity.length();
      if (speed < 0.01) {
         return -1.0F;
      }

      Vec3 direction = velocity.normalize();
      Vec3 start = this.body.position();
      float stepSize = tuning(this.body).raycastStep();
      int maxSteps = (int)(tuning(this.body).raycastRange() / stepSize);

      for (int step = 0; step < maxSteps; step++) {
         float distance = step * stepSize;
         Vec3 checkPos = start.add(direction.scale(distance));
         BlockPos blockPos = new BlockPos((int)Math.floor(checkPos.x), (int)Math.floor(checkPos.y), (int)Math.floor(checkPos.z));
         BlockState blockState = this.body.level().getBlockState(blockPos);
         if (!blockState.isAir() && !blockState.getCollisionShape(this.body.level(), blockPos).isEmpty()) {
            return distance;
         }
      }

      return -1.0F;
   }

   public float getDiveBreakSpeedMultiplier(float distanceToGround) {
      if (distanceToGround >= tuning(this.body).range()) {
         return 1.0F;
      }

      float progress = distanceToGround / tuning(this.body).range();
      float curvedProgress = (float)Math.pow(progress, tuning(this.body).curve());
      return Mth.lerp(curvedProgress, tuning(this.body).minSpeed(), 1.0F);
   }
}

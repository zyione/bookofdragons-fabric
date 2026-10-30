package net.magister.liberanimalium.fauna.behavior;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class Loaf implements Behavior {
   @Nullable
   private BlockPos chest;
   @Nullable
   private Vec3 opener;
   private boolean asked;
   private boolean gaveUp;

   public void sitOn(@Nullable BlockPos chest, @Nullable Vec3 opener) {
      if (chest == null || !chest.equals(this.chest)) {
         this.asked = false;
      }

      this.chest = chest;
      this.opener = opener;
   }

   public boolean takeGaveUp() {
      boolean was = this.gaveUp;
      this.gaveUp = false;
      return was;
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.asked = false;
      this.gaveUp = false;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.chest == null) {
         handle.setPose(PoseFlag.LOAFING, false);
      } else if (!on(handle, this.chest)) {
         handle.setPose(PoseFlag.LOAFING, false);
         if (handle.currentMode() == MoveMode.AIR && over(handle, this.chest)) {
            if (move.idle()) {
               move.alight(1.0F);
            }
         } else if (!move.idle() || !handle.settleOn(this.chest)) {
            if (this.asked && move.idle() && near(handle, this.chest)) {
               this.gaveUp = true;
               move.stop();
            } else {
               move.goTo(this.chest.above(), 1.0F);
               this.asked = true;
            }
         }
      } else {
         move.stop();
         handle.setPose(PoseFlag.LOAFING, true);
         handle.applyLook(this.opener);
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      handle.setPose(PoseFlag.LOAFING, false);
      handle.applyLook(null);
      this.chest = null;
      this.opener = null;
      this.asked = false;
   }

   private static boolean on(AnimalHandle handle, BlockPos chest) {
      if (handle.currentMode() == MoveMode.AIR) {
         return false;
      }

      Vec3 at = handle.bodyPosition();
      double dx = at.x - (chest.getX() + 0.5);
      double dz = at.z - (chest.getZ() + 0.5);
      return at.y >= chest.getY() + 0.5 && at.y < chest.getY() + 2 && dx * dx + dz * dz <= 0.0625;
   }

   private static boolean near(AnimalHandle handle, BlockPos chest) {
      Vec3 at = handle.bodyPosition();
      double dx = at.x - (chest.getX() + 0.5);
      double dz = at.z - (chest.getZ() + 0.5);
      return dx * dx + dz * dz <= 4.0;
   }

   private static boolean over(AnimalHandle handle, BlockPos chest) {
      Vec3 at = handle.bodyPosition();
      double reach = 0.5 + handle.bounds().getXsize() * 0.5;
      return Math.abs(at.x - (chest.getX() + 0.5)) < reach && Math.abs(at.z - (chest.getZ() + 0.5)) < reach;
   }
}

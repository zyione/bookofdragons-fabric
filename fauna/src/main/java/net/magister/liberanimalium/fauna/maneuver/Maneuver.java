package net.magister.liberanimalium.fauna.maneuver;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public abstract class Maneuver {
   protected int ticks;
   @Nullable
   private Vec3 issued;
   private boolean everIssued;
   private int blockedTicks;

   public String name() {
      return this.getClass().getSimpleName().replace("Maneuver", "").toUpperCase(Locale.ROOT);
   }

   public String phase() {
      return "";
   }

   public boolean running(AnimalHandle handle) {
      return false;
   }

   public void begin(AnimalHandle handle, Waypoints move) {
   }

   public abstract Maneuver.Status tick(AnimalHandle var1, Waypoints var2);

   public void stop(AnimalHandle handle, Waypoints move) {
   }

   public boolean premiseHolds(AnimalHandle handle) {
      return true;
   }

   public final int ticksRun() {
      return this.ticks;
   }

   protected boolean steer(Waypoints move, @Nullable Vec3 point, float speed) {
      if (point == null) {
         return false;
      }

      if (this.issued != null && point.distanceToSqr(this.issued) < 1.0) {
         return false;
      }

      if (this.solvePending(move)) {
         return false;
      }

      boolean taken = move.goTo(BlockPos.containing(point), speed);
      if (!taken) {
         return false;
      }

      this.issued = point;
      this.everIssued = true;
      return true;
   }

   private boolean solvePending(Waypoints move) {
      return this.everIssued && move.solving();
   }

   protected void releaseSteering() {
      this.issued = null;
   }

   protected boolean blocked(AnimalHandle handle) {
      return handle.currentMode() == MoveMode.AIR ? true : ++this.blockedTicks >= 20;
   }

   protected void unblocked() {
      this.blockedTicks = 0;
   }

   public final void advance() {
      this.ticks++;
   }

   public enum Status {
      RUNNING,
      SUCCEEDED,
      FAILED;
   }
}

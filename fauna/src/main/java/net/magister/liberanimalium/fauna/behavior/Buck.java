package net.magister.liberanimalium.fauna.behavior;

import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public final class Buck implements Behavior, Laddered {
   private Buck.Rung rung = Buck.Rung.BOLT;
   private long nextRearAt;
   private long nextLegAt;
   private float energy = 0.5F;
   private boolean flier;

   @Override
   public String rungName() {
      return this.rung.name();
   }

   public Buck.Rung rung() {
      return this.rung;
   }

   public void ridden(float energy, boolean flier) {
      this.energy = Mth.clamp(energy, 0.0F, 1.0F);
      this.flier = flier;
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.enterBolt(handle, now);
      this.nextLegAt = now;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      if (this.rung == Buck.Rung.REAR) {
         if (Halter.of(handle).struggling()) {
            move.stop();
            return;
         }

         this.enterBolt(handle, now);
      }

      if (now >= this.nextRearAt) {
         this.enterRear(handle, move, now);
      } else if (move.idle() && now >= this.nextLegAt) {
         BlockPos destination = this.pick(handle, now);
         if (destination == null) {
            this.nextLegAt = now + 40L;
         } else {
            this.nextLegAt = now + 40L;
            move.bolt(destination, 1.0F);
         }
      }
   }

   private BlockPos pick(AnimalHandle handle, long now) {
      BlockPos ground = Roam.pick(handle, now, 10, 20);
      if (ground != null && this.flier) {
         boolean lift = now / Math.max(1, 40) % 2L == 0L;
         return lift ? ground.above(12) : ground;
      } else {
         return ground;
      }
   }

   private void enterRear(AnimalHandle handle, MoveVerbs move, long now) {
      this.rung = Buck.Rung.REAR;
      move.stop();
      handle.setPose(PoseFlag.BUCKING, true);
   }

   private void enterBolt(AnimalHandle handle, long now) {
      this.rung = Buck.Rung.BOLT;
      handle.setPose(PoseFlag.BUCKING, false);
      this.nextRearAt = now + rearInterval(this.energy);
   }

   public static int rearInterval(float energy) {
      float t = Mth.clamp(energy, 0.0F, 1.0F);
      return Math.round(Mth.lerp(t, 200.0F, 80.0F));
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      handle.setPose(PoseFlag.BUCKING, false);
      this.rung = Buck.Rung.BOLT;
      this.nextRearAt = Long.MIN_VALUE;
      this.nextLegAt = Long.MIN_VALUE;
   }

   public enum Rung {
      BOLT,
      REAR;
   }
}

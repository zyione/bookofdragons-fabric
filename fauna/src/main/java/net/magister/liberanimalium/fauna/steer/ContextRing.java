package net.magister.liberanimalium.fauna.steer;

import java.util.Arrays;
import net.minecraft.world.phys.Vec3;

public final class ContextRing {
   private final int slots;
   private final float[] interest;
   private final float[] danger;
   private final float[] previousInterest;
   private float peakInterest;
   private float dangerUp;
   private float dangerDown;
   private final float[] score;
   private float verticalBias;
   private int verticalChoice;

   public ContextRing(int slots) {
      this.slots = slots;
      this.interest = new float[slots];
      this.danger = new float[slots];
      this.previousInterest = new float[slots];
      this.score = new float[slots];
   }

   public int slots() {
      return this.slots;
   }

   public void begin() {
      System.arraycopy(this.interest, 0, this.previousInterest, 0, this.slots);
      Arrays.fill(this.interest, 0.0F);
      Arrays.fill(this.danger, 0.0F);
      this.peakInterest = 0.0F;
      this.dangerUp = 0.0F;
      this.dangerDown = 0.0F;
   }

   public Vec3 direction(int slot) {
      double angle = (Math.PI * 2) * slot / this.slots;
      return new Vec3(Math.sin(angle), 0.0, Math.cos(angle));
   }

   public int slotOf(Vec3 dir) {
      double angle = Math.atan2(dir.x, dir.z);
      if (angle < 0.0) {
         angle += Math.PI * 2;
      }

      return (int)Math.round(angle / (Math.PI * 2) * this.slots) % this.slots;
   }

   public void addInterest(Vec3 dir, float weight, int spread) {
      this.peakInterest = Math.max(this.peakInterest, weight);
      this.writeFalloff(this.interest, this.slotOf(dir), weight, spread);
   }

   public void addDanger(Vec3 dir, float weight, int spread) {
      this.writeFalloff(this.danger, this.slotOf(dir), weight, spread);
   }

   public void addDangerAt(int slot, float weight, int spread) {
      this.writeFalloff(this.danger, Math.floorMod(slot, this.slots), weight, spread);
   }

   public void addVerticalDanger(boolean upward, float weight) {
      if (upward) {
         this.dangerUp = Math.max(this.dangerUp, weight);
      } else {
         this.dangerDown = Math.max(this.dangerDown, weight);
      }
   }

   public float verticalBias() {
      float raw = this.dangerDown - this.dangerUp;
      this.verticalBias = raw * 0.65F + this.verticalBias * 0.35F;
      if (Math.abs(this.verticalBias) < 0.25F) {
         this.verticalChoice = 0;
      } else if (this.verticalChoice == 0) {
         this.verticalChoice = this.verticalBias > 0.0F ? 1 : -1;
      } else if (Math.signum(this.verticalBias) != this.verticalChoice && Math.abs(this.verticalBias) >= 0.5F) {
         this.verticalChoice = this.verticalBias > 0.0F ? 1 : -1;
      }

      return this.verticalChoice == 0 ? this.verticalBias : Math.copySign(Math.abs(this.verticalBias), this.verticalChoice);
   }

   public int verticalChoice() {
      return this.verticalChoice;
   }

   private void writeFalloff(float[] map, int centre, float weight, int spread) {
      for (int offset = -spread; offset <= spread; offset++) {
         int slot = Math.floorMod(centre + offset, this.slots);
         float falloff = spread == 0 ? 1.0F : 1.0F - (float)Math.abs(offset) / (spread + 1);
         float value = weight * falloff;
         if (value > map[slot]) {
            map[slot] = value;
         }
      }
   }

   public Vec3 decide() {
      double slot = this.decideSlot();
      return Double.isNaN(slot) ? null : this.directionAt(slot);
   }

   public double decideSlot() {
      this.blur(this.interest);
      this.blur(this.danger);

      for (int i = 0; i < this.slots; i++) {
         this.interest[i] = this.interest[i] * 0.65F + this.previousInterest[i] * 0.35F;
      }

      float wanted = 0.0F;

      for (float v : this.interest) {
         wanted = Math.max(wanted, v);
      }

      if (wanted <= 0.0F) {
         return Double.NaN;
      }

      int best = -1;
      float bestScore = 0.0F;

      for (int i = 0; i < this.slots; i++) {
         this.score[i] = this.interest[i] <= 0.0F ? 0.0F : this.interest[i] * (1.0F - Math.min(1.0F, this.danger[i] / 1.0F));
         if (this.score[i] > bestScore) {
            bestScore = this.score[i];
            best = i;
         }
      }

      return this.leaned(best >= 0 ? best : this.leastDangerousWanted(), bestScore > 0.0F);
   }

   public Vec3 directionAt(double slot) {
      double angle = (Math.PI * 2) * slot / this.slots;
      return new Vec3(Math.sin(angle), 0.0, Math.cos(angle)).normalize();
   }

   public double deflectionFrom(double slot, double reference) {
      double delta = slot - reference;
      double half = this.slots * 0.5;

      while (delta > half) {
         delta -= this.slots;
      }

      while (delta < -half) {
         delta += this.slots;
      }

      return delta;
   }

   public double slotAngle() {
      return (Math.PI * 2) / this.slots;
   }

   private int leastDangerousWanted() {
      int best = 0;
      float least = Float.MAX_VALUE;

      for (int i = 0; i < this.slots; i++) {
         if (!(this.interest[i] <= 0.0F) && this.danger[i] < least) {
            least = this.danger[i];
            best = i;
         }
      }

      return best;
   }

   public float confidence() {
      return this.peakInterest;
   }

   private double leaned(int best, boolean scored) {
      int left = Math.floorMod(best - 1, this.slots);
      int right = Math.floorMod(best + 1, this.slots);
      float lean;
      float span;
      if (scored) {
         lean = this.score[right] - this.score[left];
         span = Math.max(1.0E-4F, this.score[best]);
      } else {
         lean = this.danger[left] - this.danger[right];
         span = 1.0F;
      }

      double bias = Math.max(-1.0, Math.min(1.0, lean / span)) * 0.5;
      return best + bias;
   }

   private void blur(float[] map) {
      float[] copy = (float[])map.clone();

      for (int i = 0; i < this.slots; i++) {
         float left = copy[Math.floorMod(i - 1, this.slots)];
         float right = copy[Math.floorMod(i + 1, this.slots)];
         map[i] = copy[i] * 0.75F + (left + right) * 0.125F;
      }
   }

   public float dangerAt(int slot) {
      return this.danger[Math.floorMod(slot, this.slots)];
   }

   public float interestAt(int slot) {
      return this.interest[Math.floorMod(slot, this.slots)];
   }
}

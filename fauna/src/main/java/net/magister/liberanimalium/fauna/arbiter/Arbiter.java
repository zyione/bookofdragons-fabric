package net.magister.liberanimalium.fauna.arbiter;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.drive.Drive;
import net.magister.liberanimalium.fauna.drive.DriveContext;
import net.minecraft.util.Mth;

public final class Arbiter {
   private final List<Drive> drives;
   private final float[] smoothed;
   private final float[] raw;
   @Nullable
   private Drive holder;
   private int holderIndex = -1;
   private long lastScoreAge = Long.MIN_VALUE;
   private float entryLevel;
   private float releaseLevel = -1.0F;
   private boolean released;
   private static final float EMPTY_ENTRY = 0.001F;

   public Arbiter(List<Drive> drives) {
      this.drives = List.copyOf(drives);
      this.smoothed = new float[this.drives.size()];
      this.raw = new float[drives.size()];
   }

   @Nullable
   public Drive holder() {
      return this.holder;
   }

   public float scoreOf(int index) {
      return this.smoothed[index];
   }

   public float rawOf(int index) {
      return this.raw[index];
   }

   public List<Drive> drives() {
      return this.drives;
   }

   public float releaseLevel() {
      return this.releaseLevel;
   }

   public boolean released() {
      return this.released;
   }

   public void score(DriveContext context) {
      long now = context.age();
      long elapsed = this.lastScoreAge == Long.MIN_VALUE ? 1L : Math.max(1L, now - this.lastScoreAge);
      this.lastScoreAge = now;

      for (int i = 0; i < this.drives.size(); i++) {
         Drive drive = this.drives.get(i);
         float target = Mth.clamp(drive.score(context) * context.speciesWeight(drive.id()), 0.0F, 1.0F);
         this.raw[i] = target;
         SpeciesProfile.Smoothing shape = smoothingFor(context, drive);
         if (target > this.smoothed[i]) {
            this.smoothed[i] = this.smoothed[i] + (target - this.smoothed[i]) * rise(shape);
         } else {
            this.smoothed[i] = Math.max(target, this.smoothed[i] - decay(shape) * (float)elapsed * drive.settles(context));
         }
      }
   }

   @Nullable
   public Drive pick(DriveContext context) {
      boolean wasReleased = this.released;
      this.released = this.hasHadEnough(context);
      if (this.released && !wasReleased && this.holderIndex >= 0) {
         this.smoothed[this.holderIndex] = this.raw[this.holderIndex];
      }

      if (this.holderIndex >= 0
         && this.holder != null
         && this.holder.needsASubject()
         && this.raw[this.holderIndex] <= 0.02F
         && this.smoothed[this.holderIndex] > 0.0F) {
         this.smoothed[this.holderIndex] = 0.0F;
      }

      int bestIndex = -1;
      float bestScore = 0.02F;

      for (int i = 0; i < this.drives.size(); i++) {
         float score = this.effective(i);
         if (score > bestScore) {
            bestScore = score;
            bestIndex = i;
         }
      }

      if (bestIndex >= 0) {
         Drive winner = this.drives.get(bestIndex);
         if (this.holder != null && winner != this.holder) {
            if (bestScore < preemptBarrier(this.effective(this.holderIndex))) {
               this.holder.onTick(context);
               return this.holder;
            }

            this.holder.onStop(context);
            this.holder = null;
            this.holderIndex = -1;
         }

         if (this.holder == null) {
            this.holder = winner;
            this.holderIndex = bestIndex;
            this.reseat(context, bestIndex);
            this.holder.onStart(context);
         }

         this.holder.onTick(context);
         return this.holder;
      } else if (this.holder != null && this.released) {
         this.reseat(context, this.holderIndex);
         this.holder.onTick(context);
         return this.holder;
      } else {
         this.release(context);
         return null;
      }
   }

   public static float preemptBarrier(float incumbent) {
      return Math.max(incumbent, Math.min(1.3F * incumbent, 0.95F));
   }

   private float effective(int index) {
      if (index < 0) {
         return 0.0F;
      } else {
         return index == this.holderIndex && this.released ? 0.0F : this.smoothed[index];
      }
   }

   private boolean hasHadEnough(DriveContext context) {
      if (this.holder == null || this.releaseLevel < 0.0F) {
         return false;
      }

      if (this.entryLevel <= 0.001F) {
         return false;
      }

      String store = this.holder.store();
      return store == null ? false : Mth.clamp(context.reservoir(store), 0.0F, 1.0F) <= this.releaseLevel;
   }

   private void reseat(DriveContext context, int index) {
      Drive drive = this.drives.get(index);
      this.released = false;
      String store = drive.store();
      if (store != null && drive.satiates()) {
         this.entryLevel = Mth.clamp(context.reservoir(store), 0.0F, 1.0F);
         float draw = context.draw(98691678L + context.age());
         this.releaseLevel = this.entryLevel * Mth.lerp(draw, 0.35F, 0.75F);
      } else {
         this.entryLevel = 0.0F;
         this.releaseLevel = -1.0F;
      }
   }

   private void release(DriveContext context) {
      if (this.holder != null) {
         this.holder.onStop(context);
         this.holder = null;
         this.holderIndex = -1;
      }

      this.released = false;
      this.releaseLevel = -1.0F;
      this.entryLevel = 0.0F;
   }

   @Nullable
   private static SpeciesProfile.Smoothing smoothingFor(DriveContext context, Drive drive) {
      return context.smoothing(drive.id());
   }

   private static float rise(@Nullable SpeciesProfile.Smoothing shape) {
      return shape != null && !Float.isNaN(shape.rise()) ? Mth.clamp(shape.rise(), 0.0F, 1.0F) : 0.5F;
   }

   private static float decay(@Nullable SpeciesProfile.Smoothing shape) {
      return shape != null && !Float.isNaN(shape.decay()) ? Math.max(0.0F, shape.decay()) : 0.005F;
   }
}

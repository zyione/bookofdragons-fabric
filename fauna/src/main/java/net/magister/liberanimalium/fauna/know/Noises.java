package net.magister.liberanimalium.fauna.know;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.HeardSound;
import net.minecraft.world.phys.Vec3;

public final class Noises implements Faculty {
   private float count;
   private float meals;
   @Nullable
   private Vec3 where;
   private long lastHeard = Long.MIN_VALUE;
   private long lastAged = Long.MIN_VALUE;

   @Override
   public String id() {
      return "noises";
   }

   @Override
   public void integrate(FacultyContext ctx) {
      for (HeardSound sound : ctx.mysteries()) {
         this.note(sound.position(), sound.tick(), sound.kind().meal());
      }

      for (HeardSound sound : ctx.stimuli(HeardSound.class)) {
         if (sound.kind().happening() && sound.sourceId() != null) {
            for (Representation rep : ctx.known()) {
               if (rep.id().equals(sound.sourceId()) && !rep.visible()) {
                  this.note(sound.position(), sound.tick(), false);
               }
            }
         }
      }

      this.age(ctx.now());
   }

   public void note(Vec3 position, long now) {
      this.note(position, now, false);
   }

   public void note(Vec3 position, long now, boolean meal) {
      this.count++;
      if (meal) {
         this.meals++;
      }

      this.where = position;
      this.lastHeard = now;
   }

   public void age(long now) {
      if (this.lastAged == Long.MIN_VALUE) {
         this.lastAged = now;
      } else {
         long elapsed = now - this.lastAged;
         this.lastAged = now;
         if (elapsed > 0L && !(this.count <= 0.0F)) {
            if (this.lastHeard == Long.MIN_VALUE || now - this.lastHeard >= 100L) {
               float before = this.count;
               this.count = Math.max(0.0F, this.count - (float)elapsed * 0.025F);
               this.meals = before <= 0.0F ? 0.0F : this.meals * (this.count / before);
               if (this.count <= 0.0F) {
                  this.where = null;
               }
            }
         }
      }
   }

   public void cleared() {
      this.count = 0.0F;
      this.meals = 0.0F;
      this.where = null;
   }

   public float count() {
      return this.count;
   }

   public float meals() {
      return this.meals;
   }

   @Nullable
   public Vec3 point() {
      return this.where;
   }
}

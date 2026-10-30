package net.magister.liberanimalium.fauna.know;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.HeardSound;
import net.magister.liberanimalium.fauna.api.SoundKind;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.sense.HearingSense;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Places implements Faculty {
   private final List<Places.Mark> marks = new ArrayList<>();
   @Nullable
   private AnimalHandle body;
   @Nullable
   private Vec3 fenceCentre;
   @Nullable
   private Vec3 fenceNow;
   private double fenceRadius;
   private long fenceTick;

   @Override
   public String id() {
      return "places";
   }

   private Vec3 keep(Vec3 world) {
      return this.body == null ? world : this.body.keepPlace(world);
   }

   @Override
   public void integrate(FacultyContext ctx) {
      this.body = ctx.handle();
      float nervous = ctx.temper().get(Trait.NERVOUSNESS);
      float perBlow = Mth.lerp(nervous, 0.03F, 0.15F);
      double speed = ctx.handle().velocity().length();

      for (HeardSound sound : ctx.stimuli(HeardSound.class)) {
         if (sound.kind() == SoundKind.COMBAT) {
            float strength = HearingSense.strength(ctx.species().senses(), speed, sound);
            if (strength > 0.0F) {
               this.mark(sound.position(), perBlow * strength, ctx.now());
            }
         }
      }
   }

   public void mark(Vec3 at, float severity, long now) {
      if (!(severity <= 0.0F)) {
         Vec3 world = at;
         at = this.keep(at);

         for (Places.Mark mark : this.marks) {
            if (mark.at.distanceToSqr(at) <= 9.0) {
               mark.severity = Math.min(1.0F, mark.severity + severity);
               mark.tick = now;
               return;
            }
         }

         Places.Mark made = new Places.Mark(at, Math.min(1.0F, severity), now);
         Vec3 seen = this.body == null ? null : this.body.placeNow(at);
         made.now = seen != null ? seen : world;
         this.marks.add(made);
         if (this.marks.size() > 8) {
            int weakest = 0;

            for (int i = 1; i < this.marks.size(); i++) {
               if (this.marks.get(i).severity < this.marks.get(weakest).severity) {
                  weakest = i;
               }
            }

            this.marks.remove(weakest);
         }
      }
   }

   public float dangerAt(Vec3 point, long now) {
      float worst = 0.0F;

      for (Places.Mark mark : this.marks) {
         double distance = mark.distanceTo(point);
         if (!(distance > 6.0)) {
            float fade = fade(mark, now);
            if (!(fade <= 0.0F)) {
               float reach = (float)(1.0 - distance / 6.0);
               worst = Math.max(worst, mark.severity * fade * reach);
            }
         }
      }

      return worst;
   }

   public void forget(long now) {
      Iterator<Places.Mark> it = this.marks.iterator();

      while (it.hasNext()) {
         Places.Mark mark = it.next();
         Vec3 world = this.body == null ? mark.at : this.body.placeNow(mark.at);
         if (!(fade(mark, now) <= 0.0F) && world != null) {
            mark.now = world;
         } else {
            it.remove();
         }
      }

      if (this.fenceCentre != null) {
         Vec3 world = this.body == null ? this.fenceCentre : this.body.placeNow(this.fenceCentre);
         if (world == null) {
            this.fenceCentre = null;
         } else {
            this.fenceNow = world;
         }
      }
   }

   private static float fade(Places.Mark mark, long now) {
      long age = now - mark.tick;
      if (age <= 0L) {
         return 1.0F;
      } else {
         return age >= 6000L ? 0.0F : 1.0F - (float)age / 6000.0F;
      }
   }

   public int markCount() {
      return this.marks.size();
   }

   public void fence(Vec3 centre, double radius, long now) {
      if (!(radius <= 0.0) && Double.isFinite(radius)) {
         this.fenceCentre = centre;
         Vec3 world = this.body == null ? centre : this.body.placeNow(centre);
         this.fenceNow = world == null ? centre : world;
         this.fenceRadius = radius;
         this.fenceTick = now;
      }
   }

   public boolean fenced(long now) {
      return this.fenceCentre != null && now - this.fenceTick < 3000L;
   }

   public boolean insideFence(Vec3 point, long now) {
      return !this.fenced(now) ? true : Math.min(this.fenceCentre.distanceTo(point), this.fenceNow.distanceTo(point)) <= this.fenceRadius;
   }

   public void leaveFence(Vec3 at, long now) {
      if (this.fenceCentre != null) {
         if (!this.fenced(now) || this.fenceNow.distanceTo(at) > this.fenceRadius + 6.0) {
            this.fenceCentre = null;
         }
      }
   }

   public double fenceRadius(long now) {
      return this.fenced(now) ? this.fenceRadius : 0.0;
   }

   public long fenceRemaining(long now) {
      return this.fenced(now) ? 3000L - (now - this.fenceTick) : 0L;
   }

   @Override
   public void save(CompoundTag tag) {
      ListTag list = new ListTag();

      for (Places.Mark mark : this.marks) {
         CompoundTag entry = new CompoundTag();
         entry.putDouble("X", mark.at.x);
         entry.putDouble("Y", mark.at.y);
         entry.putDouble("Z", mark.at.z);
         entry.putFloat("S", mark.severity);
         entry.putLong("T", mark.tick);
         list.add(entry);
      }

      tag.put("Marks", list);
   }

   @Override
   public void load(CompoundTag tag) {
      this.marks.clear();
      ListTag list = tag.getList("Marks", 10);

      for (int i = 0; i < list.size(); i++) {
         CompoundTag entry = list.getCompound(i);
         this.marks
            .add(new Places.Mark(new Vec3(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z")), entry.getFloat("S"), entry.getLong("T")));
      }
   }

   private static final class Mark {
      Vec3 at;
      Vec3 now;
      float severity;
      long tick;

      Mark(Vec3 at, float severity, long tick) {
         this.at = at;
         this.now = at;
         this.severity = severity;
         this.tick = tick;
      }

      double distanceTo(Vec3 point) {
         return Math.min(this.at.distanceTo(point), this.now.distanceTo(point));
      }
   }
}

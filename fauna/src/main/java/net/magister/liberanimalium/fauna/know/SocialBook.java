package net.magister.liberanimalium.fauna.know;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;

public final class SocialBook {
   private final Map<UUID, SocialBook.Opinion> book = new HashMap<>();
   private long lastAged = Long.MIN_VALUE;

   public SocialBook.Opinion of(UUID id) {
      return this.book.getOrDefault(id, SocialBook.Opinion.NONE);
   }

   public boolean knows(UUID id) {
      return this.book.containsKey(id);
   }

   public void note(UUID id, float likeDelta, float fearDelta) {
      SocialBook.Opinion was = this.of(id);
      float absorb = 1.0F - 0.9F * was.familiarity();
      SocialBook.Opinion now = new SocialBook.Opinion(
         Mth.clamp(was.like() + likeDelta * absorb, -1.0F, 1.0F),
         Mth.clamp(was.fear() + fearDelta * absorb, -1.0F, 1.0F),
         Mth.clamp(was.familiarity() + 0.05F, 0.0F, 1.0F)
      );
      this.book.put(id, now);
      this.trim(id);
   }

   public void fed(UUID id, float amount, float fearShare) {
      SocialBook.Opinion was = this.of(id);
      float earned = Mth.clamp(was.like(), 0.0F, 1.0F);
      float share = Mth.lerp(earned, 1.0F, 0.25F);
      float gain = amount * share;
      this.book
         .put(
            id,
            new SocialBook.Opinion(
               Mth.clamp(was.like() + gain, -1.0F, 1.0F),
               Mth.clamp(was.fear() - gain * fearShare, -1.0F, 1.0F),
               Mth.clamp(was.familiarity() + 0.05F, 0.0F, 1.0F)
            )
         );
      this.trim(id);
   }

   public void courted(UUID id, float like, float familiarity) {
      SocialBook.Opinion was = this.of(id);
      float paid = Math.max(0.0F, like) + Math.max(0.0F, familiarity);
      this.book
         .put(
            id,
            new SocialBook.Opinion(
               Mth.clamp(was.like() + like, -1.0F, 1.0F),
               Math.max(Math.min(was.fear(), 0.0F), was.fear() - paid * 1.0F),
               Mth.clamp(was.familiarity() + Math.max(0.0F, familiarity), 0.0F, 1.0F)
            )
         );
      this.trim(id);
   }

   public void introduce(UUID id, float fear) {
      if (!this.book.containsKey(id)) {
         this.book.put(id, new SocialBook.Opinion(0.0F, Mth.clamp(fear, 0.0F, 1.0F), 0.0F));
         this.trim(id);
      }
   }

   public void betrayed(UUID id, float cost) {
      SocialBook.Opinion was = this.of(id);
      this.book
         .put(
            id,
            new SocialBook.Opinion(
               Mth.clamp(was.like() - Math.max(0.0F, cost), -1.0F, 1.0F), was.fear(), Mth.clamp(was.familiarity() + 0.05F, 0.0F, 1.0F)
            )
         );
      this.trim(id);
   }

   public void rite(UUID id, float like) {
      SocialBook.Opinion was = this.of(id);
      this.book
         .put(
            id,
            new SocialBook.Opinion(
               Mth.clamp(was.like() + Math.max(0.0F, like), -1.0F, 1.0F), was.fear(), Mth.clamp(was.familiarity() + 0.05F, 0.0F, 1.0F)
            )
         );
      this.trim(id);
   }

   public void atLeast(UUID id, float floor) {
      SocialBook.Opinion was = this.of(id);
      if (was.like() >= floor) {
         this.habituate(id);
      } else {
         this.book.put(id, new SocialBook.Opinion(Mth.clamp(floor, -1.0F, 1.0F), was.fear(), Mth.clamp(was.familiarity() + 0.05F, 0.0F, 1.0F)));
         this.trim(id);
      }
   }

   public void stage(UUID id, float like) {
      SocialBook.Opinion was = this.of(id);
      this.book.put(id, new SocialBook.Opinion(Mth.clamp(like, -1.0F, 1.0F), was.fear(), was.familiarity()));
      this.trim(id);
   }

   public void habituate(UUID id) {
      SocialBook.Opinion was = this.book.get(id);
      if (was != null) {
         this.book.put(id, new SocialBook.Opinion(was.like(), was.fear(), Mth.clamp(was.familiarity() + 0.05F, 0.0F, 1.0F)));
      }
   }

   public void age(long now) {
      this.age(now, null);
   }

   public void age(long now, @Nullable UUID exempt) {
      if (this.lastAged == Long.MIN_VALUE) {
         this.lastAged = now;
      } else {
         long elapsed = now - this.lastAged;
         if (elapsed >= 24000L) {
            this.lastAged = now;
            int days = (int)(elapsed / 24000L);
            float retention = (float)Math.pow(0.85F, days);
            this.book
               .replaceAll(
                  (id, was) -> new SocialBook.Opinion(
                     id.equals(exempt) ? was.like() : fade(was.like(), retention), fade(was.fear(), retention), was.familiarity()
                  )
               );
            this.book
               .entrySet()
               .removeIf(
                  e -> !e.getKey().equals(exempt)
                     && Math.abs(e.getValue().like()) < 0.25F
                     && Math.abs(e.getValue().fear()) < 0.25F
                     && e.getValue().familiarity() < 0.1F
               );
         }
      }
   }

   private static float fade(float value, float retention) {
      return Math.abs(value) >= 0.5F ? value : value * retention;
   }

   public void forgive(UUID id, float ceiling, long elapsed) {
      if (id != null && elapsed > 0L) {
         SocialBook.Opinion was = this.of(id);
         if (!(was.like() >= ceiling)) {
            float raised = Math.min(ceiling, was.like() + 3.3333334E-5F * (float)elapsed);
            this.book.put(id, new SocialBook.Opinion(raised, was.fear(), was.familiarity()));
         }
      }
   }

   public void warm(UUID id, float amount, float ceiling) {
      if (!(amount <= 0.0F)) {
         SocialBook.Opinion was = this.of(id);
         float top = Math.min(ceiling, 1.0F);
         if (!(was.like() >= top)) {
            float raised = Math.min(top, was.like() + amount);
            this.book.put(id, new SocialBook.Opinion(raised, was.fear(), was.familiarity()));
            this.trim(id);
         }
      }
   }

   public void forget(UUID id) {
      this.book.remove(id);
   }

   public int size() {
      return this.book.size();
   }

   private void trim(UUID justWritten) {
      if (this.book.size() > 16) {
         UUID weakest = null;
         float least = Float.MAX_VALUE;

         for (Entry<UUID, SocialBook.Opinion> entry : this.book.entrySet()) {
            if (!entry.getKey().equals(justWritten)) {
               float weight = entry.getValue().weight();
               if (weight < least) {
                  least = weight;
                  weakest = entry.getKey();
               }
            }
         }

         if (weakest != null) {
            this.book.remove(weakest);
         }
      }
   }

   public void save(CompoundTag tag) {
      ListTag list = new ListTag();

      for (Entry<UUID, SocialBook.Opinion> entry : this.book.entrySet()) {
         CompoundTag row = new CompoundTag();
         row.putUUID("Id", entry.getKey());
         row.putFloat("L", entry.getValue().like());
         row.putFloat("F", entry.getValue().fear());
         row.putFloat("K", entry.getValue().familiarity());
         list.add(row);
      }

      tag.put("Social", list);
      tag.putLong("SocialAged", this.lastAged);
   }

   public void load(CompoundTag tag) {
      this.book.clear();
      ListTag list = tag.getList("Social", 10);

      for (int i = 0; i < list.size(); i++) {
         CompoundTag row = list.getCompound(i);
         if (row.hasUUID("Id")) {
            this.book.put(row.getUUID("Id"), new SocialBook.Opinion(row.getFloat("L"), row.getFloat("F"), row.getFloat("K")));
         }
      }

      this.lastAged = tag.contains("SocialAged") ? tag.getLong("SocialAged") : Long.MIN_VALUE;
   }

   public record Opinion(float like, float fear, float familiarity) {
      public static final SocialBook.Opinion NONE = new SocialBook.Opinion(0.0F, 0.0F, 0.0F);

      public float weight() {
         return Math.abs(this.like) + Math.abs(this.fear);
      }
   }
}

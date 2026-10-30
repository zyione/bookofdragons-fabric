package net.magister.liberanimalium.fauna.know;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.api.SensedItem;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Larder implements Faculty {
   private final Map<UUID, Long> snubbed = new LinkedHashMap<>();
   private List<Larder.Morsel> morsels = List.of();
   private List<Larder.Offer> offers = List.of();
   @Nullable
   private UUID offerPin;
   private float offerPinCraving;
   private long offerPinUntil;

   @Override
   public String id() {
      return "larder";
   }

   @Override
   public void settle(FacultyContext ctx) {
      this.integrate(
         ctx.items(),
         ctx.reach(),
         ctx.space(),
         heed(Crowding.guardedness(ctx.temper().get(Trait.SYMPATHY), ctx.temper().get(Trait.NERVOUSNESS))),
         ctx.known(),
         ctx.now()
      );
   }

   public List<Larder.Morsel> morsels() {
      return this.morsels;
   }

   public List<Larder.Offer> offers() {
      return this.offers;
   }

   public boolean isSnubbed(UUID id, long now) {
      Long until = this.snubbed.get(id);
      return until != null && now < until;
   }

   public void snub(UUID id, long now) {
      this.snubbed.remove(id);
      this.snubbed.put(id, now + 6000L);

      while (this.snubbed.size() > 8) {
         this.snubbed.remove(this.snubbed.keySet().iterator().next());
      }
   }

   public void forceOffer(UUID holder, float craving, long until) {
      this.offerPin = holder;
      this.offerPinCraving = Mth.clamp(craving, 0.0F, 1.0F);
      this.offerPinUntil = until;
   }

   public void integrate(List<SensedItem> items, double reach, float space, float heed, Iterable<Representation> reps, long now) {
      this.expire(now);
      List<SensedItem> ground = new ArrayList<>();
      Map<UUID, Larder.Offer> held = new LinkedHashMap<>();

      for (SensedItem item : items) {
         if (!(item.craving() <= 0.0F)) {
            if (item.offered()) {
               Larder.Offer was = held.get(item.holder());
               if (was == null || item.craving() > was.craving()) {
                  held.put(item.holder(), new Larder.Offer(item.holder(), item.craving(), item.edible() || was != null && was.edible()));
               } else if (item.edible() && !was.edible()) {
                  held.put(item.holder(), new Larder.Offer(was.holder(), was.craving(), true));
               }
            } else if (!this.isSnubbed(item.id(), now)) {
               ground.add(item);
            }
         }
      }

      if (this.offerPin != null) {
         if (now >= this.offerPinUntil) {
            this.offerPin = null;
         } else {
            Larder.Offer was = held.get(this.offerPin);
            if (was == null || this.offerPinCraving > was.craving()) {
               held.put(this.offerPin, new Larder.Offer(this.offerPin, this.offerPinCraving));
            }
         }
      }

      this.offers = List.copyOf(held.values());
      this.morsels = piles(ground, reach, space, heed, reps);
   }

   private static List<Larder.Morsel> piles(List<SensedItem> ground, double reach, float space, float heed, Iterable<Representation> reps) {
      if (ground.isEmpty()) {
         return List.of();
      }

      List<Larder.Morsel> out = new ArrayList<>(ground.size());
      Map<UUID, Float> looms = new HashMap<>();

      for (SensedItem item : ground) {
         looms.put(item.id(), loom(item.position(), space, reps, item.giver()));
      }

      for (SensedItem item : ground) {
         double x = 0.0;
         double y = 0.0;
         double z = 0.0;
         int members = 0;

         for (SensedItem other : ground) {
            if (!(other.position().distanceToSqr(item.position()) > reach * reach)) {
               x += other.position().x;
               y += other.position().y;
               z += other.position().z;
               members++;
            }
         }

         Vec3 centre = new Vec3(x / members, y / members, z / members);
         out.add(
            new Larder.Morsel(item.id(), item.position(), item.edible(), item.craving(), Mth.clamp(looms.get(item.id()) * heed, 0.0F, 1.0F), members, centre)
         );
      }

      return List.copyOf(out);
   }

   public static float loom(Vec3 at, float space, Iterable<Representation> reps, @Nullable UUID exempt) {
      float worst = 0.0F;

      for (Representation rep : reps) {
         if ((exempt == null || !rep.id().equals(exempt)) && rep.relation().type() != RelationType.EATS) {
            float depth = Crowding.within(rep.lastKnownPos().distanceTo(at), space);
            if (!(depth <= 0.0F)) {
               float press = Mth.clamp(depth * depth * regard(rep) * Mth.clamp(rep.presence(), 0.0F, 1.0F), 0.0F, 1.0F);
               if (press > worst) {
                  worst = press;
               }
            }
         }
      }

      return worst;
   }

   private static float regard(Representation rep) {
      float above = Mth.clamp((rep.manner() - Manner.NEUTRAL) / (1.0F - Manner.NEUTRAL), 0.0F, 1.0F);
      float presence = Mth.lerp(above, 0.85F, 1.0F);
      return Math.max(presence, Mth.clamp(rep.alarm(), 0.0F, 1.0F));
   }

   public static float heed(float guardedness) {
      return Mth.lerp(Mth.clamp(guardedness, 0.0F, 1.0F), 0.45F, 0.9F);
   }

   private void expire(long now) {
      this.snubbed.entrySet().removeIf(entry -> now >= entry.getValue());
   }

   public int snubCount() {
      return this.snubbed.size();
   }

   public record Morsel(UUID id, Vec3 at, boolean edible, float craving, float loom, int pileSize, Vec3 pileAt) {
   }

   public record Offer(UUID holder, float craving, boolean edible) {
      public Offer(UUID holder, float craving) {
         this(holder, craving, true);
      }
   }
}

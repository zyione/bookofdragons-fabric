package net.magister.liberanimalium.fauna.know;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.Order;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Obedience {
   private float compliance;
   private boolean settled;
   private long lastIntegrated = Long.MIN_VALUE;
   private float equilibrium;
   private float bondNorm;
   private float ownerFind;
   private float worstAlarm;
   private float ownerDanger;
   private float fightClaim;
   private final float[] demand = new float[Order.values().length];
   private boolean estranged;
   private float cap = 0.98F;
   @Nullable
   private Vec3 post;
   private static final String TAG = "Comply";
   private static final String TAG_POST = "ComplyPost";

   public void integrate(
      long now,
      float bondNorm,
      float steadiness,
      float sympathy,
      float bravery,
      float ownerFind,
      float worstAlarm,
      float ownerDanger,
      float fightClaim,
      float needPressure
   ) {
      this.integrate(now, bondNorm, steadiness, sympathy, bravery, ownerFind, worstAlarm, ownerDanger, fightClaim, needPressure, false, 0.0F);
   }

   public void integrate(
      long now,
      float bondNorm,
      float steadiness,
      float sympathy,
      float bravery,
      float ownerFind,
      float worstAlarm,
      float ownerDanger,
      float fightClaim,
      float needPressure,
      boolean estranged,
      float estrangement
   ) {
      this.bondNorm = bondNorm;
      this.ownerFind = ownerFind;
      this.worstAlarm = worstAlarm;
      this.ownerDanger = ownerDanger;
      this.fightClaim = fightClaim;
      this.estranged = estranged;
      this.cap = Compliance.cap(estranged, estrangement);
      this.equilibrium = Math.min(this.cap, Compliance.equilibrium(bondNorm, steadiness, sympathy, ownerFind));
      boolean firstPass = this.lastIntegrated == Long.MIN_VALUE;
      if (!this.settled) {
         this.compliance = this.equilibrium;
         this.settled = true;
      } else if (!firstPass) {
         int elapsed = (int)Math.min(Math.max(0L, now - this.lastIntegrated), 8L);
         this.compliance = Compliance.drift(this.compliance, this.equilibrium, steadiness, elapsed);
         this.compliance = Compliance.afterEvent(this.compliance, -0.001F * needPressure * elapsed);
      }

      this.compliance = Math.min(this.compliance, this.cap);
      this.lastIntegrated = now;

      for (Order order : Order.values()) {
         this.demand[order.ordinal()] = Demand.of(order, worstAlarm, ownerDanger, bravery);
      }
   }

   public boolean obeys(Order order) {
      return !this.settled || Demand.obeys(this.compliance, this.demand[order.ordinal()]);
   }

   public float demand(Order order) {
      return this.demand[order.ordinal()];
   }

   public boolean estranged() {
      return this.estranged;
   }

   public float cap() {
      return this.cap;
   }

   public float compliance() {
      return this.compliance;
   }

   public float equilibrium() {
      return this.equilibrium;
   }

   public float bondNorm() {
      return this.bondNorm;
   }

   public float ownerFind() {
      return this.ownerFind;
   }

   public float worstAlarm() {
      return this.worstAlarm;
   }

   public float ownerDanger() {
      return this.ownerDanger;
   }

   public float fightClaim() {
      return this.fightClaim;
   }

   public float share() {
      return !this.settled ? 0.0F : Compliance.share(this.compliance);
   }

   public void ordered(Vec3 at) {
      this.post = at;
   }

   @Nullable
   public Vec3 post() {
      return this.post;
   }

   public boolean settled() {
      return this.settled;
   }

   public void fed(float share) {
      this.compliance = Compliance.afterEvent(this.compliance, 0.15F * Mth.clamp(share, 0.0F, 1.0F));
      this.settled = true;
   }

   public void obeyed() {
      this.compliance = Compliance.afterEvent(this.compliance, 0.05F);
      this.settled = true;
   }

   public void struckByOwner() {
      this.compliance = Compliance.afterEvent(this.compliance, -0.4F);
      this.settled = true;
   }

   public float force(float level) {
      this.compliance = Mth.clamp(level, 0.05F, 0.98F);
      this.settled = true;
      return this.compliance;
   }

   public void save(CompoundTag tag) {
      if (this.settled) {
         tag.putFloat("Comply", this.compliance);
      }

      if (this.post != null) {
         tag.putDouble("ComplyPostX", this.post.x);
         tag.putDouble("ComplyPostY", this.post.y);
         tag.putDouble("ComplyPostZ", this.post.z);
      }
   }

   public void load(CompoundTag tag) {
      if (tag.contains("ComplyPostX")) {
         this.post = new Vec3(tag.getDouble("ComplyPostX"), tag.getDouble("ComplyPostY"), tag.getDouble("ComplyPostZ"));
      }

      if (tag.contains("Comply")) {
         this.compliance = Mth.clamp(tag.getFloat("Comply"), 0.05F, 0.98F);
         this.settled = true;
      }
   }
}

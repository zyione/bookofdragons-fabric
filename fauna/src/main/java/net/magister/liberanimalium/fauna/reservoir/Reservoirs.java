package net.magister.liberanimalium.fauna.reservoir;

import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.minecraft.nbt.CompoundTag;

public final class Reservoirs {
   public static final String HUNGER = "hunger";
   public static final String ENERGY = "energy";
   public static final String ANNOYANCE = "annoyance";
   public static final String SLEEP = "sleep";
   public static final String COMPANY = "company";
   public static final String BOREDOM = "boredom";
   private float hunger;
   private float fatigue;
   private float annoyance;
   private float sleep;
   private float company;
   private float boredom;
   private float bother;
   private static final String SLEEP_KEY = "Sleep";
   private static final String BOREDOM_KEY = "Boredom";

   public void refresh(AnimalHandle handle, boolean inCompany) {
      this.refresh(handle, inCompany, Reservoirs.Pastime.IDLE);
   }

   public void refresh(AnimalHandle handle, boolean inCompany, Reservoirs.Pastime pastime) {
      this.refresh(handle, inCompany, pastime, 0.0F);
   }

   public void refresh(AnimalHandle handle, boolean inCompany, Reservoirs.Pastime pastime, float playAppetite) {
      this.hunger = clamp(handle.hungerLevel());
      this.fatigue = clamp(handle.fatigueLevel());
      this.annoyance = Math.max(0.0F, this.annoyance - 0.0033333334F);
      this.bother = Math.max(0.0F, this.bother - 0.0033333334F);
      this.sleep = clamp(handle.asleep() ? this.sleep - 0.0011111111F : this.sleep + 8.3333335E-4F);
      this.company = clamp(inCompany ? this.company - 0.016666668F : this.company + 0.0033333334F);
      if (!handle.asleep()) {
         this.boredom = clamp(switch (pastime) {
            case IDLE -> this.boredom + 0.0016666667F;
            case OCCUPIED -> this.boredom - 0.0016666667F;
            case ROMPING -> this.boredom - ReservoirTuning.boredomRompDrainPerRefresh(playAppetite);
         });
      }
   }

   public void startle() {
      this.bother = clamp(this.bother + 0.34F);
   }

   public void startleTo(float level) {
      this.annoyance = clamp(Math.max(this.annoyance, level));
   }

   public void sleepTo(float level) {
      this.sleep = clamp(Math.max(this.sleep, level));
   }

   public float sleepDebt() {
      return this.sleep;
   }

   public void boredTo(float level) {
      this.boredom = clamp(Math.max(this.boredom, level));
   }

   public void save(CompoundTag tag) {
      tag.putFloat("Sleep", this.sleep);
      tag.putFloat("Boredom", this.boredom);
   }

   public void load(CompoundTag tag) {
      this.sleep = clamp(tag.getFloat("Sleep"));
      this.boredom = clamp(tag.getFloat("Boredom"));
   }

   public void chafe(float pressure) {
      float target = clamp(pressure);
      float rate = target > this.annoyance ? 0.025F : 0.0125F;
      this.annoyance = clamp(this.annoyance + (target - this.annoyance) * rate);
   }

   public float level(String name) {
      if ("hunger".equals(name)) {
         return this.hunger;
      } else if ("energy".equals(name)) {
         return this.fatigue;
      } else if ("sleep".equals(name)) {
         return this.sleep;
      } else if ("company".equals(name)) {
         return this.company;
      } else if ("boredom".equals(name)) {
         return this.boredom;
      } else {
         return "annoyance".equals(name) ? Math.max(this.annoyance, this.bother) : 0.0F;
      }
   }

   public float annoyance() {
      return this.annoyance;
   }

   private static float clamp(float value) {
      return Float.isNaN(value) ? 0.0F : Math.max(0.0F, Math.min(1.0F, value));
   }

   public enum Pastime {
      IDLE,
      OCCUPIED,
      ROMPING;
   }
}

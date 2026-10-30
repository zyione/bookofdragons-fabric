package net.magister.liberanimalium.fauna.api;

import java.util.UUID;
import net.magister.liberanimalium.fauna.core.Seeds;
import net.minecraft.util.Mth;

public final class DayCycle {
   public static final long DAY_TICKS = 24000L;
   public static final int LEAD_TICKS = 2000;
   public static final long SALT_BEDTIME = 1542287678L;

   private DayCycle() {
   }

   public static float fraction(long dayTime) {
      long wrapped = Math.floorMod(dayTime, 24000L);
      return (float)wrapped / 24000.0F;
   }

   public static boolean inactive(float fraction, DayCycle.Habit habit) {
      boolean daylight = fraction < 0.5F;
      return habit == DayCycle.Habit.DAY ? !daylight : daylight;
   }

   public static float phase(long dayTime, DayCycle.Habit habit, int offset) {
      float fraction = fraction(dayTime - offset);
      if (inactive(fraction, habit)) {
         return 1.0F;
      }

      float lead = 0.083333336F;
      float toStart = ring(fraction, habit == DayCycle.Habit.DAY ? 0.5F : 0.0F);
      float toEnd = ring(habit == DayCycle.Habit.DAY ? 0.0F : 0.5F, fraction);
      float nearest = Math.min(toStart, toEnd);
      return nearest >= lead ? 0.0F : Mth.clamp(1.0F - nearest / lead, 0.0F, 1.0F);
   }

   private static float ring(float from, float to) {
      float delta = to - from;
      return delta < 0.0F ? delta + 1.0F : delta;
   }

   public static int offset(UUID id) {
      float unit = Seeds.unit(id, 1542287678L);
      return Math.round((unit - 0.5F) * 2000.0F);
   }

   public enum Habit {
      DAY,
      NIGHT;

      public static final DayCycle.Habit DEFAULT = DAY;
   }
}

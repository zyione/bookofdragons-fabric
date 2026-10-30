package net.magister.liberanimalium.chassis.ability;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.LivingEntity;

public record AbilityMoment(@Nullable LivingEntity target, boolean spent, boolean fleeing, int sinceBreath, int committedTicks) {
   public static final int NEVER = Integer.MAX_VALUE;

   public static AbilityMoment idle() {
      return new AbilityMoment(null, false, false, Integer.MAX_VALUE, 0);
   }

   public boolean breathJustStopped(int ticks) {
      return this.sinceBreath <= ticks;
   }

   public boolean committed() {
      return this.committedTicks > 0;
   }

   public boolean committedFor(int ticks) {
      return this.committedTicks >= ticks;
   }
}

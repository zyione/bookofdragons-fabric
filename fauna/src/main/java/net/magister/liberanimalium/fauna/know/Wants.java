package net.magister.liberanimalium.fauna.know;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public record Wants(
   float company, float boredom, float annoyance, Wants.Want want, @Nullable UUID warnedAt, float tired, @Nullable UUID hunting, float playWant, boolean rough
) {
   public static final Wants EAGER = new Wants(1.0F, 1.0F, 0.0F, Wants.Want.NONE, null);

   public Wants(float company, float boredom, float annoyance, Wants.Want want, @Nullable UUID warnedAt, float tired, @Nullable UUID hunting) {
      this(company, boredom, annoyance, want, warnedAt, tired, hunting, 0.0F, false);
   }

   public Wants(float company, float boredom, float annoyance, Wants.Want want, @Nullable UUID warnedAt) {
      this(company, boredom, annoyance, want, warnedAt, 0.0F, null);
   }

   public enum Want {
      NONE,
      HUNGRY,
      LONELY,
      PLAYFUL,
      RESTLESS,
      TIRED,
      THREATENED,
      CURIOUS;
   }
}

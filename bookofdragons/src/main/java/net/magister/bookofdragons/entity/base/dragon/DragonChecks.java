package net.magister.bookofdragons.entity.base.dragon;

import net.magister.liberanimalium.fauna.api.Check;
import net.magister.liberanimalium.fauna.api.Checks;
import net.magister.liberanimalium.fauna.api.Delivery;
import net.magister.liberanimalium.fauna.api.Check.Engagement;

public final class DragonChecks {
   public static final Check HUNT = Checks.register(Check.of("bookofdragons:hunt", Engagement.FEEDING).fedBy(new Delivery[]{Delivery.PREY}));
   public static final Check SPAR = Checks.register(Check.of("bookofdragons:spar", Engagement.SPAR));
   public static final Check FOLLOW = Checks.register(Check.of("bookofdragons:follow", Engagement.TAG));
   public static final Check DEFEND = Checks.register(Check.of("bookofdragons:defend", Engagement.DEFEND));
   public static final Check REST = Checks.register(Check.of("bookofdragons:rest", Engagement.REST));
   public static final Check WALK = Checks.register(Check.of("bookofdragons:walk", Engagement.WALK));
   public static final Check HUNT_TOGETHER = Checks.register(Check.of("bookofdragons:hunt_together", Engagement.HUNT_TOGETHER));
   public static final Check GIFT = Checks.register(Check.of("bookofdragons:gift", Engagement.GIFT));

   private DragonChecks() {
   }

   public static void register() {
   }
}

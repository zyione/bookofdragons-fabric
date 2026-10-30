package net.magister.liberanimalium.render;

import java.util.Locale;
import java.util.Map;

public record ChainStatus(
   double muscle,
   double flow,
   double airspeedShare,
   int contacts,
   int stuck,
   double worstOffset,
   boolean anchored,
   double baseDeviation,
   double baseCap,
   double worstBend,
   int inside,
   double stepMicros,
   double frameMicros
) {
   public static Map<String, ChainStatus> of(int entityId) {
      return ChainPass.snapshot(entityId);
   }

   public String line() {
      return String.format(
         Locale.ROOT,
         "muscle %.2f flow %.2f @ %.0f%% | contacts %d stuck %d worst %.2f b | base %.1f deg of cap %.0f | worst bend %.1f deg | inside body %d | %s | step %.0f us/tick pass %.0f us/frame",
         this.muscle,
         this.flow,
         this.airspeedShare * 100.0,
         this.contacts,
         this.stuck,
         this.worstOffset,
         this.baseDeviation,
         this.baseCap,
         this.worstBend,
         this.inside,
         this.anchored ? "anchored" : "free",
         this.stepMicros,
         this.frameMicros
      );
   }
}

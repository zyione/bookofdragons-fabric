package net.magister.liberanimalium.body.anim.binding;

import java.util.Locale;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehavior;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;

public enum PlayBehavior {
   LOOP(AzPlayBehaviors.LOOP),
   PLAY_ONCE(AzPlayBehaviors.PLAY_ONCE),
   HOLD_ON_LAST_FRAME(AzPlayBehaviors.HOLD_ON_LAST_FRAME);

   public final AzPlayBehavior az;

   PlayBehavior(AzPlayBehavior az) {
      this.az = az;
   }

   public static PlayBehavior parse(String s, PlayBehavior defaultVal) {
      if (s == null) {
         return defaultVal;
      }

      return switch (s.toUpperCase(Locale.ROOT)) {
         case "PLAY_ONCE" -> PLAY_ONCE;
         case "HOLD_ON_LAST_FRAME" -> HOLD_ON_LAST_FRAME;
         default -> LOOP;
      };
   }
}

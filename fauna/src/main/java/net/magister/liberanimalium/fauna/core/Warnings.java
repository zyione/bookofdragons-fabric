package net.magister.liberanimalium.fauna.core;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.ExpressionKey;
import net.magister.liberanimalium.fauna.behavior.Behavior;
import net.magister.liberanimalium.fauna.behavior.Contest;
import net.magister.liberanimalium.fauna.behavior.Shoo;
import net.magister.liberanimalium.fauna.drive.WarnDrive;
import net.magister.liberanimalium.fauna.know.Representation;

final class Warnings {
   private static final String WARN = ExpressionKey.drive("warn", ExpressionKey.Edge.BEGIN).key();
   private static final String DISPLAY = ExpressionKey.rung("rival", Contest.Rung.DISPLAY.name(), ExpressionKey.Edge.ENTER).key();
   private static final String SHOO = ExpressionKey.rung("space", Shoo.Rung.WARN.name(), ExpressionKey.Edge.ENTER).key();

   private Warnings() {
   }

   @Nullable
   static UUID subjectOf(FaunaMind mind, ExpressionKey key) {
      String word = key.key();
      if (word.equals(WARN)) {
         Representation subject = WarnDrive.subject(mind.context());
         return subject == null ? null : subject.id();
      } else {
         Behavior running = mind.runningBehavior();
         if (word.equals(DISPLAY) && running instanceof Contest contest) {
            return contest.rival();
         } else {
            return word.equals(SHOO) && running instanceof Shoo shoo ? shoo.crowder() : null;
         }
      }
   }
}

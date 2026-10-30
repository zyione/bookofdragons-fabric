package net.magister.liberanimalium.fauna.api;

import java.util.Locale;

public record ExpressionKey(ExpressionKey.Family family, String key) {
   public static ExpressionKey drive(String driveId, ExpressionKey.Edge edge) {
      return new ExpressionKey(ExpressionKey.Family.DRIVE, ExpressionKey.Family.DRIVE.word() + "." + driveId + "." + edge.word());
   }

   public static ExpressionKey rung(String driveId, String rung, ExpressionKey.Edge edge) {
      return new ExpressionKey(
         ExpressionKey.Family.BEHAVIOR, ExpressionKey.Family.BEHAVIOR.word() + "." + driveId + ".rung." + rung.toLowerCase(Locale.ROOT) + "." + edge.word()
      );
   }

   public static ExpressionKey beat(String primitive, ExpressionKey.Edge edge) {
      return new ExpressionKey(ExpressionKey.Family.BEAT, ExpressionKey.Family.BEAT.word() + "." + primitive.toLowerCase(Locale.ROOT) + "." + edge.word());
   }

   public static ExpressionKey mode(String from, String to) {
      return new ExpressionKey(
         ExpressionKey.Family.MODE, ExpressionKey.Family.MODE.word() + "." + from.toLowerCase(Locale.ROOT) + "." + to.toLowerCase(Locale.ROOT)
      );
   }

   public static ExpressionKey pose(PoseFlag flag, ExpressionKey.Edge edge) {
      return new ExpressionKey(ExpressionKey.Family.POSE, ExpressionKey.Family.POSE.word() + "." + flag.name().toLowerCase(Locale.ROOT) + "." + edge.word());
   }

   public static ExpressionKey body(ExpressionKey.Body what) {
      return new ExpressionKey(ExpressionKey.Family.BODY, ExpressionKey.Family.BODY.word() + "." + what.word());
   }

   public static ExpressionKey.Family familyOf(String key) {
      int dot = key.indexOf(46);
      String head = dot < 0 ? key : key.substring(0, dot);

      for (ExpressionKey.Family family : ExpressionKey.Family.values()) {
         if (family.word().equals(head)) {
            return family;
         }
      }

      return null;
   }

   @Override
   public String toString() {
      return this.key;
   }

   public enum Body {
      FALL_IMPACT("fall.impact"),
      HURT("hurt"),
      EAT_BITE("eat.bite"),
      STRIKE("strike"),
      TRIGGER_PRESS("trigger.press"),
      TRIGGER_RELEASE("trigger.release"),
      BREATH_BEGIN("breath.begin"),
      BREATH_END("breath.end"),
      HOP_BEGIN("hop.begin"),
      HOP_FALL("hop.fall"),
      HOP_END("hop.end"),
      BLINK("blink"),
      ABILITY("ability"),
      COURTSHIP_RUNG("courtship.rung"),
      TOUCH_REBUFF("touch.rebuff"),
      TOUCH_DECLINE("touch.decline"),
      TOUCH_ACCEPT("touch.accept"),
      PLAY_INVITE("play.invite"),
      PLAY_CAUGHT("play.caught"),
      PLAY_WON("play.won"),
      COURTSHIP_TAMED("courtship.tamed"),
      COURTSHIP_RESET("courtship.reset"),
      MOUNT_THROWN("mount.thrown"),
      HUNT_NOTICE("hunt.notice");

      private final String word;

      Body(String word) {
         this.word = word;
      }

      public String word() {
         return this.word;
      }
   }

   public enum Edge {
      BEGIN("begin"),
      END("end"),
      ENTER("enter"),
      EXIT("exit"),
      RAISE("raise"),
      DROP("drop");

      private final String word;

      Edge(String word) {
         this.word = word;
      }

      public String word() {
         return this.word;
      }
   }

   public enum Family {
      DRIVE("drive"),
      BEHAVIOR("behavior"),
      BEAT("beat"),
      MODE("mode"),
      POSE("pose"),
      BODY("body");

      private final String word;

      Family(String word) {
         this.word = word;
      }

      public String word() {
         return this.word;
      }
   }
}

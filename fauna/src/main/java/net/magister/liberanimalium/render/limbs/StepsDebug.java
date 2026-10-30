package net.magister.liberanimalium.render.limbs;

import java.util.Locale;
import net.magister.liberanimalium.anim.rig.Smoothing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StepsDebug {
   static final Logger LOGGER = LoggerFactory.getLogger("STEPS");
   public static volatile boolean log = false;
   public static volatile float forceDelta = 0.0F;
   public static volatile boolean noWrite = false;
   public static volatile float poseShare = Float.NaN;
   public static volatile boolean shift = true;
   public static volatile StepsDebug.Plant plant = StepsDebug.Plant.TERRAIN;

   private StepsDebug() {
   }

   public static void reset() {
      log = false;
      forceDelta = 0.0F;
      noWrite = false;
      poseShare = Float.NaN;
      shift = true;
      plant = StepsDebug.Plant.TERRAIN;
      Smoothing.on = true;
   }

   static void say(String format, Object... args) {
      LOGGER.debug(String.format(Locale.ROOT, format.replace("%n", ""), args));
   }

   public enum Plant {
      OFF,
      TERRAIN,
      ALWAYS;
   }
}

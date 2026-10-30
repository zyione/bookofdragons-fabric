package net.magister.liberanimalium.render.waves;

public class WaveConstants {
   public static final int BLOCKS_PER_CHUNK = 16;
   public static final int PIXELS_PER_BLOCK = 4;
   public static final int SIMULATION_RESOLUTION = 64;
   public static final float DAMPING = 0.935F;
   public static final float WAVE_SPEED = 0.42F;
   public static final float CREST = 0.6F;
   public static final float SWELL = 0.04F;
   public static final float TROUGH = -0.04F;
   public static final float LIFT_MOST = 0.6F;
   public static final float LIFT_GAIN = 0.26F;
   public static final float LIFT_HEIGHT = 0.25F;
   public static final float LIFT_LEVEL = 0.2F;
   public static final float LIFT_SLOPE = 0.5F;
   public static final float LIFT_SKEW = 0.35F;
   public static final float LIFT_RISE = 0.12F;
   public static final float WATER_HEIGHT_OFFSET = 0.89088887F;
   public static final float MIN_ENERGY_THRESHOLD = 0.0015F;
   public static final int IDLE_TICKS_BEFORE_SLEEP = 25;
   public static final int IDLE_TICKS_BEFORE_REMOVAL = 100;
}

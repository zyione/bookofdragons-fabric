package net.magister.liberanimalium.spawn;

public final class SpawnTuning {
   public static final int PERIOD_TICKS = 600;
   public static final int ROOSTLESS_CAP = 30;
   public static final int PER_EXTRA_PLAYER = 4;
   public static final int DENSITY_CAP = 10;
   public static final double DENSITY_RADIUS = 128.0;
   public static final double SPECIES_SHARE = 0.35;
   public static final int ABUNDANCE_REGION = 512;
   public static final double ABUNDANCE_DAYS = 3.0;
   public static final double ABUNDANCE_FLOOR = 0.0;
   public static final double TIDE_FLOOR = 0.4;
   public static final double ABUNDANCE_LOW = 0.3;
   public static final double ABUNDANCE_HIGH = 0.7;
   public static final double DESPAWN_SAFE_RADIUS = 64.0;
   public static final int DESPAWN_UNWATCHED_TICKS = 6000;
   public static final double DESPAWN_CHANCE = 5.0E-4;
   public static final int MIN_LIFE_TICKS = 600;
   public static final int CHUNK_REACH = 8;
   public static final double MIN_PLAYER_DISTANCE = 24.0;
   public static final double SIGHT_MARGIN = 8.0;
   public static final double HOME_RADIUS = 64.0;
   public static final double VIEW_HALF_ANGLE = 70.0;
   public static final double VIEW_RANGE = 128.0;
   public static final int AIR_HEIGHT_MIN = 16;
   public static final int AIR_HEIGHT_MAX = 48;
   public static final int BEACON_REACH = 64;
   public static final int FIT_REACH = 2;
   public static final int GROUP_TRIES = 3;
   public static final int GROUP_HOPS = 4;
   public static final int SCAN_REACH = 8;
   public static final int CAVE_FLOORS_TRIED = 4;
   public static final int CAVE_FLOOR_REACH = 16;
   public static final int CAVE_MIN_DEPTH = 12;
   public static final int CAVE_FILL_LIMIT = 512;
   public static final int LOG_SIZE = 20;

   private SpawnTuning() {
   }
}

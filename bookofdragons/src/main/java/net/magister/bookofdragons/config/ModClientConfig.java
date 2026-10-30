package net.magister.bookofdragons.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import org.apache.commons.lang3.tuple.Pair;

public class ModClientConfig {
   public static final ForgeConfigSpec SPEC;
   public static final ModClientConfig INSTANCE;
   public final EnumValue<ModClientConfig.SprintMode> sprintMode;
   public final BooleanValue readableBookFont;
   public final BooleanValue cameraRollEnabled;
   public final DoubleValue thirdPersonDistance;
   public final DoubleValue firstPersonFovOffset;
   public final BooleanValue barrelRollEnabled;
   public final BooleanValue speedFovEffectEnabled;
   public final DoubleValue speedFovEffectStrength;
   public final DoubleValue dragonMouseSensitivity;
   public final EnumValue<ModClientConfig.CameraPerspective> ridingCameraPerspective;
   public final BooleanValue rememberLastCameraPerspective;
   public final BooleanValue dragonCameraLooseFollow;
   public final DoubleValue cinematicRotStiffness;
   public final DoubleValue cinematicRotDamping;
   public final DoubleValue cinematicPosStiffness;
   public final DoubleValue cinematicPosDamping;
   public final DoubleValue cinematicRotLooseness;
   public final DoubleValue cinematicPosLoosenessLateral;
   public final DoubleValue cinematicPosLoosenessForward;
   public final BooleanValue proceduralRigEnabled;
   public final BooleanValue turbulenceEnabled;
   public final DoubleValue turbulenceStrength;
   public final BooleanValue speedLinesEnabled;
   public final BooleanValue plushSoundsEnabled;

   private ModClientConfig(Builder builder) {
      builder.comment("Book of Dragons Client Settings").push("client");
      this.sprintMode = builder.comment(
            new String[]{
               "Sprint mode while riding dragons",
               "HOLD: Sprint while holding the sprint key (like vanilla)",
               "TOGGLE: Press to start sprinting, press again to stop"
            }
         )
         .translation("bookofdragons.config.sprintMode")
         .defineEnum("sprintMode", ModClientConfig.SprintMode.TOGGLE);
      this.cameraRollEnabled = builder.comment(
            new String[]{"Enable camera roll while riding dragons", "Set to false to disable for motion sickness accessibility"}
         )
         .translation("bookofdragons.config.cameraRollEnabled")
         .define("cameraRollEnabled", true);
      this.thirdPersonDistance = builder.comment("Third person camera distance when riding dragons")
         .translation("bookofdragons.config.thirdPersonDistance")
         .defineInRange("thirdPersonDistance", 8.0, 4.0, 100.0);
      this.firstPersonFovOffset = builder.comment("FOV offset for first-person view while riding")
         .translation("bookofdragons.config.firstPersonFovOffset")
         .defineInRange("firstPersonFovOffset", 0.0, -50.0, 50.0);
      this.barrelRollEnabled = builder.comment(
            new String[]{
               "Enable barrel roll with A/D keys while flying",
               "When true: A/D causes the dragon to roll left/right",
               "When false: A/D applies yaw (turn left/right) with natural banking"
            }
         )
         .translation("bookofdragons.config.barrelRollEnabled")
         .define("barrelRollEnabled", true);
      this.speedFovEffectEnabled = builder.comment("Enable speed-based FOV effects while flying")
         .translation("bookofdragons.config.speedFovEffectEnabled")
         .define("speedFovEffectEnabled", true);
      this.speedFovEffectStrength = builder.comment("Multiplier for the speed-based FOV effect")
         .translation("bookofdragons.config.speedFovEffectStrength")
         .defineInRange("speedFovEffectStrength", 1.0, 0.0, 10.0);
      this.dragonMouseSensitivity = builder.comment(
            new String[]{
               "Dragon flight mouse sensitivity multiplier",
               "Applied on top of vanilla camera sensitivity after normalisation.",
               "Increase if dragon controls feel too heavy (e.g. Raw Input disabled,",
               "remote desktop, or low-DPI mouse with non-default pointer speed).",
               "Default 1.0 = same feel as before for players with Raw Input enabled."
            }
         )
         .translation("bookofdragons.config.dragonMouseSensitivity")
         .defineInRange("dragonMouseSensitivity", 1.0, 0.1, 10.0);
      this.ridingCameraPerspective = builder.comment(
            new String[]{
               "Preferred camera perspective when mounting a dragon", "FIRST_PERSON: First-person view", "THIRD_PERSON: Third-person view (behind the dragon)"
            }
         )
         .translation("bookofdragons.config.ridingCameraPerspective")
         .defineEnum("ridingCameraPerspective", ModClientConfig.CameraPerspective.THIRD_PERSON);
      this.rememberLastCameraPerspective = builder.comment(
            new String[]{
               "When true, remembers the last camera perspective used while riding",
               "and restores it on the next mount instead of always switching to the preferred one"
            }
         )
         .translation("bookofdragons.config.rememberLastCameraPerspective")
         .define("rememberLastCameraPerspective", false);
      this.dragonCameraLooseFollow = builder.comment(
            new String[]{
               "Enable loose-follow camera for 3D barrel-roll flight (third-person only).",
               "When true, the third-person camera lags slightly behind the dragon's rotation",
               "with a small overshoot on settle, giving a cinematic feel.",
               "Has no effect in first-person or 2D flight mode."
            }
         )
         .translation("bookofdragons.config.dragonCameraLooseFollow")
         .define("dragonCameraLooseFollow", true);
      this.cinematicRotStiffness = builder.comment("Cinematic Camera: Rotation Stiffness")
         .translation("bookofdragons.config.cinematicRotStiffness")
         .defineInRange("cinematicRotStiffness", 30.0, 0.0, 100.0);
      this.cinematicRotDamping = builder.comment("Cinematic Camera: Rotation Damping")
         .translation("bookofdragons.config.cinematicRotDamping")
         .defineInRange("cinematicRotDamping", 15.0, 0.0, 100.0);
      this.cinematicPosStiffness = builder.comment("Cinematic Camera: Position Stiffness")
         .translation("bookofdragons.config.cinematicPosStiffness")
         .defineInRange("cinematicPosStiffness", 30.0, 0.0, 100.0);
      this.cinematicPosDamping = builder.comment("Cinematic Camera: Position Damping")
         .translation("bookofdragons.config.cinematicPosDamping")
         .defineInRange("cinematicPosDamping", 15.0, 0.0, 100.0);
      this.cinematicRotLooseness = builder.comment("Cinematic Camera: Rotation Looseness")
         .translation("bookofdragons.config.cinematicRotLooseness")
         .defineInRange("cinematicRotLooseness", 0.5, 0.0, 1.0);
      this.cinematicPosLoosenessLateral = builder.comment("Cinematic Camera: Position Looseness (Lateral)")
         .translation("bookofdragons.config.cinematicPosLoosenessLateral")
         .defineInRange("cinematicPosLoosenessLateral", 0.5, 0.0, 1.0);
      this.cinematicPosLoosenessForward = builder.comment("Cinematic Camera: Position Looseness (Forward)")
         .translation("bookofdragons.config.cinematicPosLoosenessForward")
         .defineInRange("cinematicPosLoosenessForward", 0.125, 0.0, 1.0);
      this.readableBookFont = builder.comment("Write the Book of Dragons in upright letters instead of the slanted hand")
         .translation("bookofdragons.config.readableBookFont")
         .define("readableBookFont", false);
      this.proceduralRigEnabled = builder.comment(
            new String[]{
               "Procedural animation on dragons: tail and neck physics, the body tilting to",
               "the ground and the legs reaching for it. Off, dragons play their animations",
               "as a distant dragon does"
            }
         )
         .translation("bookofdragons.config.proceduralRigEnabled")
         .define("proceduralRigEnabled", true);
      this.turbulenceEnabled = builder.comment("Shake the camera in the buffeting of fast flight and dives")
         .translation("bookofdragons.config.turbulenceEnabled")
         .define("turbulenceEnabled", true);
      this.turbulenceStrength = builder.comment("Multiplier for the turbulence shake")
         .translation("bookofdragons.config.turbulenceStrength")
         .defineInRange("turbulenceStrength", 1.0, 0.0, 3.0);
      this.speedLinesEnabled = builder.comment("Draw streaks of air rushing past the rider at speed")
         .translation("bookofdragons.config.speedLinesEnabled")
         .define("speedLinesEnabled", true);
      this.plushSoundsEnabled = builder.comment("Stuffed toys squeak when squeezed, hit or bounced")
         .translation("bookofdragons.config.plushSoundsEnabled")
         .define("plushSoundsEnabled", true);
      builder.pop();
   }

   public static boolean isSprintToggleMode() {
      return getSafe(INSTANCE.sprintMode) == ModClientConfig.SprintMode.TOGGLE;
   }

   public static boolean isCameraRollEnabled() {
      return getSafe(INSTANCE.cameraRollEnabled);
   }

   public static double getThirdPersonDistance() {
      return getSafe(INSTANCE.thirdPersonDistance);
   }

   public static double getFirstPersonFovOffset() {
      return getSafe(INSTANCE.firstPersonFovOffset);
   }

   public static boolean isBarrelRollEnabled() {
      return getSafe(INSTANCE.barrelRollEnabled);
   }

   public static boolean isSpeedFovEffectEnabled() {
      return getSafe(INSTANCE.speedFovEffectEnabled);
   }

   public static double getSpeedFovEffectStrength() {
      return getSafe(INSTANCE.speedFovEffectStrength);
   }

   public static double getDragonMouseSensitivity() {
      return getSafe(INSTANCE.dragonMouseSensitivity);
   }

   public static ModClientConfig.CameraPerspective getRidingCameraPerspective() {
      return getSafe(INSTANCE.ridingCameraPerspective);
   }

   public static boolean isRememberLastCameraPerspective() {
      return getSafe(INSTANCE.rememberLastCameraPerspective);
   }

   public static boolean isCameraLooseFollowEnabled() {
      return getSafe(INSTANCE.dragonCameraLooseFollow);
   }

   public static double getCinematicRotStiffness() {
      return getSafe(INSTANCE.cinematicRotStiffness);
   }

   public static double getCinematicRotDamping() {
      return getSafe(INSTANCE.cinematicRotDamping);
   }

   public static double getCinematicPosStiffness() {
      return getSafe(INSTANCE.cinematicPosStiffness);
   }

   public static double getCinematicPosDamping() {
      return getSafe(INSTANCE.cinematicPosDamping);
   }

   public static double getCinematicRotLooseness() {
      return getSafe(INSTANCE.cinematicRotLooseness);
   }

   public static double getCinematicPosLoosenessLateral() {
      return getSafe(INSTANCE.cinematicPosLoosenessLateral);
   }

   public static double getCinematicPosLoosenessForward() {
      return getSafe(INSTANCE.cinematicPosLoosenessForward);
   }

   public static boolean isReadableBookFont() {
      return getSafe(INSTANCE.readableBookFont);
   }

   public static boolean isProceduralRigEnabled() {
      return getSafe(INSTANCE.proceduralRigEnabled);
   }

   public static boolean isTurbulenceEnabled() {
      return getSafe(INSTANCE.turbulenceEnabled);
   }

   public static double getTurbulenceStrength() {
      return getSafe(INSTANCE.turbulenceStrength);
   }

   public static boolean isSpeedLinesEnabled() {
      return getSafe(INSTANCE.speedLinesEnabled);
   }

   public static boolean isPlushSoundsEnabled() {
      return getSafe(INSTANCE.plushSoundsEnabled);
   }

   public static void setReadableBookFont(boolean readable) {
      if (SPEC.isLoaded()) {
         INSTANCE.readableBookFont.set(readable);
         SPEC.save();
      }
   }

   private static <T> T getSafe(ConfigValue<T> value) {
      return (T)(SPEC.isLoaded() ? value.get() : value.getDefault());
   }

   static {
      Pair<ModClientConfig, ForgeConfigSpec> pair = new Builder().configure(ModClientConfig::new);
      INSTANCE = (ModClientConfig)pair.getLeft();
      SPEC = (ForgeConfigSpec)pair.getRight();
   }

   public enum CameraPerspective {
      FIRST_PERSON,
      THIRD_PERSON;
   }

   public enum SprintMode {
      HOLD,
      TOGGLE;
   }
}

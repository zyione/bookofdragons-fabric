package net.magister.bookofdragons.config;

import net.magister.liberanimalium.config.RideSettings;
import net.magister.liberanimalium.config.WorldRules;
import net.magister.liberanimalium.config.RideSettings.Source;

public final class DragonSettings {
   private DragonSettings() {
   }

   public static void install() {
      RideSettings.install("bookofdragons", new DragonSettings.Ride());
      WorldRules.install(new DragonSettings.World());
   }

   private static final class Ride implements Source {
      public boolean sprintToggles() {
         return ModClientConfig.isSprintToggleMode();
      }

      public boolean cameraRoll() {
         return ModClientConfig.isCameraRollEnabled();
      }

      public double thirdPersonDistance() {
         return ModClientConfig.getThirdPersonDistance();
      }

      public double firstPersonFovOffset() {
         return ModClientConfig.getFirstPersonFovOffset();
      }

      public boolean barrelRoll() {
         return ModClientConfig.isBarrelRollEnabled();
      }

      public boolean speedFov() {
         return ModClientConfig.isSpeedFovEffectEnabled();
      }

      public double speedFovStrength() {
         return ModClientConfig.getSpeedFovEffectStrength();
      }

      public double mouseSensitivity() {
         return ModClientConfig.getDragonMouseSensitivity();
      }

      public boolean ridesInFirstPerson() {
         return ModClientConfig.getRidingCameraPerspective() == ModClientConfig.CameraPerspective.FIRST_PERSON;
      }

      public boolean remembersLastPerspective() {
         return ModClientConfig.isRememberLastCameraPerspective();
      }

      public boolean cameraLooseFollow() {
         return ModClientConfig.isCameraLooseFollowEnabled();
      }

      public double cinematicRotStiffness() {
         return ModClientConfig.getCinematicRotStiffness();
      }

      public double cinematicRotDamping() {
         return ModClientConfig.getCinematicRotDamping();
      }

      public double cinematicPosStiffness() {
         return ModClientConfig.getCinematicPosStiffness();
      }

      public double cinematicPosDamping() {
         return ModClientConfig.getCinematicPosDamping();
      }

      public double cinematicRotLooseness() {
         return ModClientConfig.getCinematicRotLooseness();
      }

      public double cinematicPosLoosenessLateral() {
         return ModClientConfig.getCinematicPosLoosenessLateral();
      }

      public double cinematicPosLoosenessForward() {
         return ModClientConfig.getCinematicPosLoosenessForward();
      }

      public boolean proceduralRig() {
         return ModClientConfig.isProceduralRigEnabled();
      }

      public boolean turbulence() {
         return ModClientConfig.isTurbulenceEnabled();
      }

      public double turbulenceStrength() {
         return ModClientConfig.getTurbulenceStrength();
      }

      public boolean speedLines() {
         return ModClientConfig.isSpeedLinesEnabled();
      }
   }

   private static final class World implements net.magister.liberanimalium.config.WorldRules.Source {
      public boolean maySetFire() {
         return ModCommonConfig.dragonsMaySetFire();
      }

      public boolean mayBreakBlocks() {
         return ModCommonConfig.dragonsMayBreakBlocks();
      }

      public boolean qteTapAssist() {
         return ModCommonConfig.isQteTapAssistEnabled();
      }

      public boolean grabPlayers() {
         return ModCommonConfig.dragonsMayGrabPlayers();
      }

      public float strangerShare() {
         return ModCommonConfig.strangerReactionShare();
      }
   }
}

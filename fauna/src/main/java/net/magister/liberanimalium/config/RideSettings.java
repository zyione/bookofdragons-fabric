package net.magister.liberanimalium.config;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RideSettings {
   private static final RideSettings.Source DEFAULTS = new RideSettings.Source() {};
   private static final Map<String, RideSettings.Source> HOSTS = Collections.synchronizedMap(new LinkedHashMap<>());
   private static volatile String riding;

   private RideSettings() {
   }

   public static void install(String namespace, RideSettings.Source host) {
      HOSTS.put(namespace, host);
   }

   public static void riding(String namespace) {
      riding = namespace;
   }

   private static RideSettings.Source now() {
      String host = riding;
      RideSettings.Source source = host == null ? null : HOSTS.get(host);
      if (source != null) {
         return source;
      }

      synchronized (HOSTS) {
         Iterator var3 = HOSTS.values().iterator();
         if (var3.hasNext()) {
            return (RideSettings.Source)var3.next();
         }
      }

      return DEFAULTS;
   }

   public static boolean isProceduralRigEnabled() {
      return now().proceduralRig();
   }

   public static boolean isTurbulenceEnabled() {
      return now().turbulence();
   }

   public static double getTurbulenceStrength() {
      return now().turbulenceStrength();
   }

   public static boolean isSpeedLinesEnabled() {
      return now().speedLines();
   }

   public static boolean isSprintToggleMode() {
      return now().sprintToggles();
   }

   public static boolean isCameraRollEnabled() {
      return now().cameraRoll();
   }

   public static double getThirdPersonDistance() {
      return now().thirdPersonDistance();
   }

   public static double getFirstPersonFovOffset() {
      return now().firstPersonFovOffset();
   }

   public static boolean isBarrelRollEnabled() {
      return now().barrelRoll();
   }

   public static boolean isSpeedFovEffectEnabled() {
      return now().speedFov();
   }

   public static double getSpeedFovEffectStrength() {
      return now().speedFovStrength();
   }

   public static double getMouseSensitivity() {
      return now().mouseSensitivity();
   }

   public static boolean ridesInFirstPerson() {
      return now().ridesInFirstPerson();
   }

   public static boolean isRememberLastCameraPerspective() {
      return now().remembersLastPerspective();
   }

   public static boolean isCameraLooseFollowEnabled() {
      return now().cameraLooseFollow();
   }

   public static double getCinematicRotStiffness() {
      return now().cinematicRotStiffness();
   }

   public static double getCinematicRotDamping() {
      return now().cinematicRotDamping();
   }

   public static double getCinematicPosStiffness() {
      return now().cinematicPosStiffness();
   }

   public static double getCinematicPosDamping() {
      return now().cinematicPosDamping();
   }

   public static double getCinematicRotLooseness() {
      return now().cinematicRotLooseness();
   }

   public static double getCinematicPosLoosenessLateral() {
      return now().cinematicPosLoosenessLateral();
   }

   public static double getCinematicPosLoosenessForward() {
      return now().cinematicPosLoosenessForward();
   }

   public interface Source {
      default boolean sprintToggles() {
         return true;
      }

      default boolean cameraRoll() {
         return true;
      }

      default double thirdPersonDistance() {
         return 8.0;
      }

      default double firstPersonFovOffset() {
         return 0.0;
      }

      default boolean barrelRoll() {
         return false;
      }

      default boolean speedFov() {
         return true;
      }

      default double speedFovStrength() {
         return 1.0;
      }

      default double mouseSensitivity() {
         return 1.0;
      }

      default boolean ridesInFirstPerson() {
         return false;
      }

      default boolean remembersLastPerspective() {
         return false;
      }

      default boolean cameraLooseFollow() {
         return false;
      }

      default double cinematicRotStiffness() {
         return 30.0;
      }

      default double cinematicRotDamping() {
         return 15.0;
      }

      default double cinematicPosStiffness() {
         return 30.0;
      }

      default double cinematicPosDamping() {
         return 15.0;
      }

      default double cinematicRotLooseness() {
         return 0.5;
      }

      default double cinematicPosLoosenessLateral() {
         return 0.5;
      }

      default double cinematicPosLoosenessForward() {
         return 0.125;
      }

      default boolean proceduralRig() {
         return true;
      }

      default boolean turbulence() {
         return true;
      }

      default double turbulenceStrength() {
         return 1.0;
      }

      default boolean speedLines() {
         return true;
      }
   }
}

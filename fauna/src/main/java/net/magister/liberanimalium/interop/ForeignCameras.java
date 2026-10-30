package net.magister.liberanimalium.interop;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public final class ForeignCameras {
   private static boolean looked;
   private static Object shoulder;
   private static MethodHandle shoulderActive;
   private static MethodHandle shoulderChange;
   private static Object shoulderPlain;
   private static Object shoulderOwn;
   private static MethodHandle perspectiveEnabled;
   private static MethodHandle perspectiveEnable;
   private static boolean riding;
   private static boolean shoulderAtMount;
   private static boolean perspectiveAtMount;

   private ForeignCameras() {
   }

   public static void noteMount() {
      if (!looked) {
         look();
      }

      if (!riding) {
         shoulderAtMount = shoulderOn();
         perspectiveAtMount = perspectiveOn();
      }
   }

   public static void riding(boolean now) {
      if (!looked) {
         look();
      }

      if (shoulderChange != null || perspectiveEnable != null) {
         if (now) {
            if (!riding) {
               riding = true;
               shoulderAtMount = shoulderAtMount | shoulderOn();
               perspectiveAtMount = perspectiveAtMount | perspectiveOn();
            }

            if (shoulderOn()) {
               shoulderSet(shoulderPlain);
            }

            if (perspectiveOn()) {
               perspectiveSet(false);
            }
         } else if (riding) {
            riding = false;
            if (perspectiveAtMount) {
               perspectiveSet(true);
            }

            if (shoulderAtMount && Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
               shoulderSet(shoulderOwn);
            }

            shoulderAtMount = false;
            perspectiveAtMount = false;
         }
      }
   }

   private static boolean shoulderOn() {
      if (shoulderActive == null) {
         return false;
      }

      try {
         return (boolean)shoulderActive.invoke((Object)shoulder);
      } catch (Throwable broken) {
         shoulderActive = null;
         shoulderChange = null;
         return false;
      }
   }

   private static void shoulderSet(Object perspective) {
      if (shoulderChange != null) {
         try {
            shoulderChange.invoke((Object)shoulder, (Object)perspective);
         } catch (Throwable broken) {
            shoulderActive = null;
            shoulderChange = null;
         }
      }
   }

   private static boolean perspectiveOn() {
      if (perspectiveEnabled == null) {
         return false;
      }

      try {
         return (boolean)perspectiveEnabled.invoke();
      } catch (Throwable broken) {
         perspectiveEnabled = null;
         perspectiveEnable = null;
         return false;
      }
   }

   private static void perspectiveSet(boolean enabled) {
      if (perspectiveEnable != null) {
         try {
            perspectiveEnable.invoke((boolean)enabled);
         } catch (Throwable broken) {
            perspectiveEnabled = null;
            perspectiveEnable = null;
         }
      }
   }

   private static synchronized void look() {
      if (!looked) {
         lookShoulder();
         lookPerspective();
         looked = true;
      }
   }

   private static void lookShoulder() {
      try {
         Class<?> entry = Class.forName("com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing");
         Method access = entry.getMethod("getInstance");
         Object instance = access.invoke(null);
         if (instance == null) {
            return;
         }

         Class<?> api = access.getReturnType();
         Method active = api.getMethod("isShoulderSurfing");
         Method change = null;

         for (Method method : api.getMethods()) {
            if (method.getName().equals("changePerspective") && method.getParameterCount() == 1 && method.getParameterTypes()[0].isEnum()) {
               change = method;
               break;
            }
         }

         if (change == null) {
            return;
         }

         Class<? extends Enum> kind = (Class<? extends Enum>)change.getParameterTypes()[0];
         shoulderPlain = Enum.valueOf(kind, "THIRD_PERSON_BACK");
         shoulderOwn = Enum.valueOf(kind, "SHOULDER_SURFING");
         Lookup lookup = MethodHandles.publicLookup();
         shoulderActive = lookup.unreflect(active);
         shoulderChange = lookup.unreflect(change);
         shoulder = instance;
      } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
         shoulderActive = null;
         shoulderChange = null;
      }
   }

   private static void lookPerspective() {
      try {
         Class<?> api = Class.forName("io.github.leawind.perspectiveapi.api.PerspectiveAPI");
         Lookup lookup = MethodHandles.publicLookup();
         perspectiveEnabled = lookup.findStatic(api, "isEnabled", MethodType.methodType(boolean.class));
         perspectiveEnable = lookup.findStatic(api, "setEnabled", MethodType.methodType(void.class, boolean.class));
      } catch (ReflectiveOperationException | LinkageError absent) {
         perspectiveEnabled = null;
         perspectiveEnable = null;
      }
   }
}

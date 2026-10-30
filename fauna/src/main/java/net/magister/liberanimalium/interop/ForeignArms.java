package net.magister.liberanimalium.interop;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class ForeignArms {
   private static boolean looked;
   private static MethodHandle punchyHidden;
   private static boolean wanted;
   private static boolean aside;

   private ForeignArms() {
   }

   public static boolean present() {
      if (!looked) {
         look();
      }

      return punchyHidden != null;
   }

   public static void want() {
      wanted = true;
   }

   public static void frame(boolean expected) {
      if (present()) {
         boolean hide = wanted || expected;
         wanted = false;
         if (hide || aside) {
            try {
               punchyHidden.invoke((boolean)hide);
               aside = hide;
            } catch (Throwable broken) {
               punchyHidden = null;
            }
         }
      }
   }

   private static synchronized void look() {
      if (!looked) {
         try {
            Class<?> renderer = Class.forName("punchy.client.render.PunchyArmRenderer", false, ForeignArms.class.getClassLoader());
            punchyHidden = MethodHandles.publicLookup().findStatic(renderer, "setFirstPersonHidden", MethodType.methodType(void.class, boolean.class));
         } catch (ReflectiveOperationException | LinkageError absent) {
            punchyHidden = null;
         }

         looked = true;
      }
   }
}

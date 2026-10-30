package net.magister.liberanimalium.anim.sample;

import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mod.azure.azurelib.animation.controller.keyframe.AzAnimationPoint;
import mod.azure.azurelib.animation.easing.AzEasingType;

public final class Easings {
   private static final Map<AzEasingType, Easings.Curve> NO_ARG = new ConcurrentHashMap<>();
   private static final Map<AzEasingType, Boolean> SPECIAL = new ConcurrentHashMap<>();

   private Easings() {
   }

   public static Easings.Curve curveFor(AzEasingType type) {
      return NO_ARG.computeIfAbsent(type, t -> new Easings.Curve(t, t.buildTransformer(null), special(t)));
   }

   public static boolean special(AzEasingType type) {
      return SPECIAL.computeIfAbsent(type, Easings::detectSpecial);
   }

   private static boolean detectSpecial(AzEasingType type) {
      return declaredOutsideInterface(type, "apply", AzAnimationPoint.class, Double.class, double.class)
         || declaredOutsideInterface(type, "apply", AzAnimationPoint.class);
   }

   private static boolean declaredOutsideInterface(AzEasingType type, String name, Class<?>... args) {
      try {
         Method m = type.getClass().getMethod(name, args);
         return m.getDeclaringClass() != AzEasingType.class;
      } catch (NoSuchMethodException e) {
         return true;
      }
   }

   public record Curve(AzEasingType type, Double2DoubleFunction fn, boolean special) {
   }
}

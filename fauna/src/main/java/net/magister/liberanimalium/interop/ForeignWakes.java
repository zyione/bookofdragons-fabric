package net.magister.liberanimalium.interop;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import net.minecraft.world.entity.EntityType;

public final class ForeignWakes {
   private static final Set<EntityType<?>> OURS = new ReferenceOpenHashSet();
   private static boolean absent;
   private static Method leftAlone;
   private static Object filled;

   private ForeignWakes() {
   }

   public static void own(EntityType<?> type) {
      if (OURS.add(type)) {
         filled = null;
         look();
      }
   }

   public static void look() {
      if (!absent && !OURS.isEmpty()) {
         try {
            if (leftAlone == null) {
               leftAlone = Class.forName("com.leclowndu93150.wakes.config.WakesConfig").getMethod("getMobBlacklist");
            }

            Object set = leftAlone.invoke(null);
            if (set == filled) {
               return;
            }

            if (!(set instanceof Set)) {
               absent = true;
               return;
            }

            ((Set)set).addAll(OURS);
            filled = set;
         } catch (InvocationTargetException var1) {
         } catch (ReflectiveOperationException | LinkageError | UnsupportedOperationException gone) {
            absent = true;
         }
      }
   }
}

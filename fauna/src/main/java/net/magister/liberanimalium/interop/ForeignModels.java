package net.magister.liberanimalium.interop;

import java.util.function.Function;
import net.magister.liberanimalium.ride.render.RiderAnimator;
import net.magister.liberanimalium.ride.touch.TouchEase;
import net.minecraft.world.entity.LivingEntity;

public final class ForeignModels {
   private static boolean looked;

   private ForeignModels() {
   }

   public static void look() {
      if (!looked) {
         looked = true;

         try {
            Class<?> api = Class.forName("traben.entity_model_features.EMFAnimationApi");
            Function<Object, Boolean> posedHere = subject -> subject instanceof LivingEntity entity
               && (RiderAnimator.poses(entity) || TouchEase.weight(entity.getUUID()) > 0.0F);
            api.getMethod("registerPauseCondition", Function.class).invoke(null, posedHere);
         } catch (ReflectiveOperationException | LinkageError var2) {
         }
      }
   }
}

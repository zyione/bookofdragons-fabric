package net.magister.liberanimalium.chassis.component.qte;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.RiderInputComponent;
import net.magister.liberanimalium.qte.QteInput;
import net.minecraft.util.Mth;

public final class QteInputSource {
   private QteInputSource() {
   }

   public static QteInput read(FaunaBase body) {
      RiderInputComponent input = body.componentRegistry.get(RiderInputComponent.class);
      if (input == null) {
         return QteInput.NONE;
      }

      float axisX = Mth.clamp(input.getStrafe(), -1.0F, 1.0F);
      float axisY = Mth.clamp(input.getForward(), -1.0F, 1.0F);
      boolean primary = input.boostHeld();
      boolean secondary = input.sprintHeld();
      int letter = input.tameKey();
      int presses = input.tameKeyPresses();
      input.drainTameKey();
      return new QteInput(axisX, axisY, primary, secondary, letter, presses);
   }
}

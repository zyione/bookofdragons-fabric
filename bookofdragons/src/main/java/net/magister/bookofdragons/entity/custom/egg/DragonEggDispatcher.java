package net.magister.bookofdragons.entity.custom.egg;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;

public class DragonEggDispatcher {
   private static final AzCommand IDLE = AzCommand.create("egg_controller", "animation.egg_idle", AzPlayBehaviors.LOOP);
   private static final AzCommand WOBBLE = AzCommand.create("egg_controller", "animation.egg_wobble", AzPlayBehaviors.LOOP);
   private static final AzCommand HATCH = AzCommand.create("egg_controller", "animation.egg_hatch", AzPlayBehaviors.PLAY_ONCE);
   private final DragonEggEntity egg;

   public DragonEggDispatcher(DragonEggEntity egg) {
      this.egg = egg;
   }

   public void idle() {
      IDLE.sendForEntity(this.egg);
   }

   public void wobble() {
      WOBBLE.sendForEntity(this.egg);
   }

   public void hatch() {
      HATCH.sendForEntity(this.egg);
   }

   public void dispatchAnimation() {
      int currentTime = this.egg.getCurrentHatchTime();
      if (currentTime < 10) {
         this.hatch();
      } else if (currentTime < 200) {
         this.wobble();
      } else {
         this.idle();
      }
   }
}

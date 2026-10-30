package net.magister.liberanimalium.chassis;

import net.magister.liberanimalium.body.state.TransportMode;
import net.minecraft.world.entity.ai.control.LookControl;

public class FaunaLookControl extends LookControl {
   private final FaunaBase body;

   public FaunaLookControl(FaunaBase body) {
      super(body);
      this.body = body;
   }

   protected boolean resetXRotOnTick() {
      return !this.body.isFlying() && this.body.getTransportMode() != TransportMode.AQUATIC;
   }
}

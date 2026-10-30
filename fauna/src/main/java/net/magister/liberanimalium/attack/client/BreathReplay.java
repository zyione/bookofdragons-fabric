package net.magister.liberanimalium.attack.client;

import net.magister.liberanimalium.attack.runtime.AttackPhaseClock;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.ranged.IPrimaryAttackHandler;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public final class BreathReplay {
   private BreathReplay() {
   }

   public static void apply(int entityId, int tick, AttackPhaseClock.Phase phase, float progress, Vec3 mouth, Vec3 direction) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.getEntity(entityId) instanceof FaunaBase body) {
         if (body.componentRegistry.get(IPrimaryAttackHandler.class) instanceof OmniAttackHandler omni) {
            omni.emitFromServer(tick, phase, progress, mouth, direction);
         }
      }
   }
}

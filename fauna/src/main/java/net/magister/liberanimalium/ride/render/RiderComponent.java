package net.magister.liberanimalium.ride.render;

import java.util.HashMap;
import java.util.Map;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.IFaunaComponent;
import net.minecraft.world.entity.Entity;

public final class RiderComponent implements IFaunaComponent {
   private final Map<Integer, RiderAnimator.State> riders = new HashMap<>();
   private FaunaBase host;

   public static RiderComponent of(FaunaBase body) {
      return body == null ? null : body.componentRegistry.get(RiderComponent.class);
   }

   @Override
   public void onAttach(FaunaBase host) {
      this.host = host;
   }

   RiderAnimator.State state(int playerId) {
      return this.riders.computeIfAbsent(playerId, id -> new RiderAnimator.State());
   }

   RiderAnimator.State peek(int playerId) {
      return this.riders.get(playerId);
   }

   @Override
   public void clientTick() {
      if (!this.riders.isEmpty()) {
         this.riders.keySet().removeIf(id -> {
            for (Entity p : this.host.getPassengers()) {
               if (p.getId() == id) {
                  return false;
               }
            }

            return true;
         });
      }
   }
}

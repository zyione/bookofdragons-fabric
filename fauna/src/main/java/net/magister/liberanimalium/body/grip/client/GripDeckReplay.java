package net.magister.liberanimalium.body.grip.client;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.GripComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;

public final class GripDeckReplay {
   private GripDeckReplay() {
   }

   public static void apply(int bodyId, @Nullable AABB part) {
      if (Minecraft.getInstance().level != null) {
         if (Minecraft.getInstance().level.getEntity(bodyId) instanceof FaunaBase body) {
            GripComponent grip = body.grip();
            if (grip != null) {
               grip.acceptDeck(part);
            }
         }
      }
   }
}

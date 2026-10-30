package net.magister.liberanimalium.body.event;

import net.magister.liberanimalium.body.PartIndex;
import net.magister.liberanimalium.body.PartIndexed;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaMultipartComponent;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class PartIndexEvents {
   private PartIndexEvents() {
   }

   public static void init() {
      CommonEvents.ENTITY_JOIN.register(PartIndexEvents::onJoin);
      CommonEvents.ENTITY_LEAVE.register(PartIndexEvents::onLeave);
   }

   public static void onJoin(Entity entity, Level level, boolean loadedFromDisk) {
      if (entity instanceof FaunaBase body) {
         FaunaMultipartComponent multipart = body.componentRegistry.get(FaunaMultipartComponent.class);
         if (multipart != null && multipart.parts().length != 0) {
            index(level).add(multipart);
         }
      }
   }

   public static void onLeave(Entity entity, Level level) {
      if (entity instanceof FaunaBase body) {
         FaunaMultipartComponent multipart = body.componentRegistry.get(FaunaMultipartComponent.class);
         if (multipart != null) {
            index(level).remove(multipart);
         }
      }
   }

   public static PartIndex index(Level level) {
      return ((PartIndexed)level).liberanimalium$partIndex();
   }
}

package net.magister.bookofdragons.discovery;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.magister.liberanimalium.fauna.know.Representation;

public final class Witness {
   private Witness() {
   }

   @Nullable
   public static String seen(DragonBase dragon) {
      FaunaMind mind = dragon.faunaMind();
      if (mind == null) {
         return null;
      }

      UUID engaged = mind.engagedId();
      if (engaged != null) {
         Representation rep = mind.knowledge().of(engaged);
         String kind = rep != null && !rep.isPlayer() && !rep.sameSpecies() ? Lore.kind(rep.staticRelation().type()) : null;
         if ("prey".equals(kind) || "foe".equals(kind)) {
            return "seen:" + kind;
         }
      }

      if (mind.holder() != null && "threat".equals(mind.holder().id())) {
         for (Representation rep : mind.knowledge().all()) {
            if (rep.visible() && !rep.isPlayer() && !rep.sameSpecies() && rep.staticRelation().type() == RelationType.AFRAID) {
               return "seen:fear";
            }
         }
      }

      return null;
   }
}

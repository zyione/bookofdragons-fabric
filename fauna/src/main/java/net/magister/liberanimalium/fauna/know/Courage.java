package net.magister.liberanimalium.fauna.know;

import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.identity.Temper;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.minecraft.world.phys.Vec3;

public final class Courage {
   private Courage() {
   }

   public static float of(float bravery, float sympathy, int kin) {
      return bravery + (1.0F - bravery) * lift(sympathy, kin);
   }

   public static float lift(float sympathy, int kin) {
      return kin <= 0 ? 0.0F : 0.6F * sympathy * kin / (kin + 2.0F);
   }

   public static float calm(float lift) {
      return 1.0F + 1.6F * Math.max(0.0F, lift);
   }

   public static float of(Temper temper, Iterable<Representation> known, Vec3 at) {
      return of(temper.get(Trait.BRAVERY), temper.get(Trait.SYMPATHY), kinNear(known, at));
   }

   public static int kinNear(Iterable<Representation> known, Vec3 at) {
      double range = 256.0;
      int kin = 0;

      for (Representation rep : known) {
         if (rep.relation().type() == RelationType.PACK && !rep.isPlayer() && rep.visible() && rep.lastKnownPos().distanceToSqr(at) <= range) {
            kin++;
         }
      }

      return kin;
   }
}

package net.magister.liberanimalium.chassis.component.melee;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.part.FaunaPartEntity;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.platform.part.Parts;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CombatReach {
   public static final double MELEE_REACH_BASE = 2.5;
   public static final double MELEE_REACH_SCALE = 1.3;
   public static final double TRY_SHARE = 0.8;
   public static final double MELEE_REACH = 3.25;

   private CombatReach() {
   }

   public static double contactSqr(LivingEntity self, LivingEntity target) {
      return contactSqr(self.getBbWidth(), target.getBbWidth());
   }

   public static double contactSqr(double selfWidth, double targetWidth) {
      double reach = selfWidth / 2.0 + targetWidth / 2.0 + 3.25;
      return reach * reach;
   }

   public static Vec3 closestPoint(AABB box, Vec3 from) {
      return new Vec3(
         Mth.clamp(from.x, box.minX, box.maxX),
         Mth.clamp(from.y, box.minY, box.maxY),
         Mth.clamp(from.z, box.minZ, box.maxZ)
      );
   }

   public static Vec3 closestPoint(Entity target, Vec3 from) {
      List<AABB> boxes = new ArrayList<>();
      boxes.add(target.getBoundingBox());
      if (Parts.isMultipart(target)) {
         for (Entity part : Parts.partsOf(target)) {
            boxes.add(part.getBoundingBox());
         }
      }

      return closestPoint(boxes, from);
   }

   public static boolean unblocked(AnimalHandle self, Vec3 from, Entity target) {
      return self.headClear()
         && self.lineOfSight(from, closestPoint(target, from))
         && (self.entity() == null || self.lineOfSight(self.entity().getEyePosition(), target.getBoundingBox().getCenter()));
   }

   public static Vec3 closestPoint(List<AABB> boxes, Vec3 from) {
      Vec3 best = null;
      double bestSq = Double.MAX_VALUE;

      for (AABB box : boxes) {
         Vec3 candidate = closestPoint(box, from);
         double sq = candidate.distanceToSqr(from);
         if (best == null || sq < bestSq) {
            best = candidate;
            bestSq = sq;
         }
      }

      return best == null ? from : best;
   }

   @Nullable
   public static FaunaPartEntity nearestHead(FaunaBase self, Vec3 toward) {
      List<FaunaPartEntity> heads = self.headParts();
      FaunaPartEntity best = null;
      double bestSq = Double.MAX_VALUE;

      for (FaunaPartEntity head : heads) {
         double sq = head.position().distanceToSqr(toward);
         if (sq < bestSq) {
            bestSq = sq;
            best = head;
         }
      }

      return best;
   }

   public static Vec3 aimOrigin(FaunaBase self) {
      return self.getEyePosition();
   }

   public static Vec3 biteOrigin(FaunaBase self, Vec3 toward) {
      FaunaPartEntity head = nearestHead(self, toward);
      return head != null ? head.position() : self.getEyePosition();
   }
}

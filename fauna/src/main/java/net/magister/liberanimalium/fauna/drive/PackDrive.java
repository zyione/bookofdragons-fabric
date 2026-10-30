package net.magister.liberanimalium.fauna.drive;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Packmates;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.Wants;
import net.minecraft.world.phys.Vec3;

public final class PackDrive implements Drive {
   public static final String ID = "pack";

   @Override
   public String id() {
      return "pack";
   }

   @Override
   public Wants.Want shows() {
      return Wants.Want.LONELY;
   }

   @Override
   public String store() {
      return "company";
   }

   @Override
   public float score(DriveContext context) {
      Vec3 centre = centroid(context);
      if (centre == null) {
         return 0.0F;
      }

      float pressure = BidShape.beyond(centre.distanceTo(context.position()), context.flockRadius(), context.sightRange());
      float apart = context.reservoir("company");
      float theta = BidShape.threshold(context.trait(Trait.SYMPATHY), 0.25F, 0.75F);
      return BidShape.felt(apart, theta) * BidShape.gate(pressure * intensity(context), apart);
   }

   @Nullable
   public static Vec3 centroid(DriveContext context) {
      Set<UUID> loose = new HashSet<>();

      for (Representation rep : context.known()) {
         if (context.tie(rep) <= 0.0F) {
            loose.add(rep.id());
         }
      }

      return Packmates.centroid(context.known(), context::estimate, id -> context.playingWith(id) || loose.contains(id));
   }

   private static float intensity(DriveContext context) {
      float sum = 0.0F;
      int count = 0;

      for (Representation rep : context.known()) {
         if (rep.relation().type() == RelationType.PACK && !context.playingWith(rep.id())) {
            float tie = context.tie(rep);
            if (!(tie <= 0.0F)) {
               sum += rep.relation().intensity() * tie;
               count++;
            }
         }
      }

      return count == 0 ? 0.0F : sum / count;
   }
}

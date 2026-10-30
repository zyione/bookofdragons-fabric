package net.magister.liberanimalium.fauna.nature;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.behavior.Obey;
import net.magister.liberanimalium.fauna.maneuver.Ground;
import net.minecraft.world.phys.Vec3;

public final class ObeyHandoff implements Handoff {
   private final Obey obey;

   public ObeyHandoff(Obey obey) {
      this.obey = obey;
   }

   @Override
   public void brief(HandoffContext ctx) {
      AnimalHandle handle = ctx.handle();
      UUID ownerId = Halter.of(handle).ownerId();
      this.obey.holdPostWithin(Ground.arrivalTolerance(handle));
      this.obey.ownerCrouching(Halter.of(handle).ownerCrouching());
      this.obey
         .ordered(
            Halter.of(handle).order(),
            ownerId,
            Halter.of(handle).ownerPosition(),
            post(handle, ctx.knowledge().obedience().post()),
            ownerId != null && Halter.of(handle).riddenBy(ownerId)
         );
   }

   @Nullable
   private static Vec3 post(AnimalHandle handle, @Nullable Vec3 kept) {
      return kept == null ? null : handle.placeNow(kept);
   }
}

package net.magister.liberanimalium.fauna.nature;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Ladder;
import net.magister.liberanimalium.fauna.behavior.Bout;
import net.magister.liberanimalium.fauna.behavior.Fighter;
import net.magister.liberanimalium.fauna.drive.DriveContext;
import net.magister.liberanimalium.fauna.know.Knowledge;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.maneuver.Subject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public interface HandoffContext {
   AnimalHandle handle();

   Knowledge knowledge();

   DriveContext drives();

   long age();

   boolean routeFailed();

   Fighter fighter();

   Bout boutAgainst(@Nullable Representation var1);

   @Nullable
   Subject subjectOf(UUID var1);

   ToDoubleFunction<Vec3> regard();

   int ambientRange();

   boolean aloft();

   @Nullable
   Predicate<BlockPos> withinReach();

   @Nullable
   Vec3 patrolHeading(Vec3 var1);

   Ladder ladderFor();

   double standoffFor(Representation var1);
}

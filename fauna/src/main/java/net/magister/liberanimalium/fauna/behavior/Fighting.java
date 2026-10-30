package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface Fighting {
   Engage engagement();

   @Nullable
   UUID engagedWith();

   boolean armed();
}

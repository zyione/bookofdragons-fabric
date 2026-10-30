package net.magister.liberanimalium.fauna.nature;

import java.util.Set;
import net.magister.liberanimalium.fauna.behavior.Behavior;
import net.magister.liberanimalium.fauna.drive.Drive;

public record Pursuit(Drive drive, Behavior behavior, Handoff handoff, Set<Class<?>> endowments) {
   public String id() {
      return this.drive.id();
   }
}

package net.magister.liberanimalium.fauna.nature;

import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.behavior.Rest;
import net.magister.liberanimalium.fauna.drive.DriveContext;
import net.magister.liberanimalium.fauna.drive.PackDrive;
import net.magister.liberanimalium.fauna.drive.RestDrive;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Crowding;
import net.magister.liberanimalium.fauna.know.Knowledge;
import net.magister.liberanimalium.fauna.maneuver.Ground;

public final class RestHandoff implements Handoff {
   private final Rest rest;

   public RestHandoff(Rest rest) {
      this.rest = rest;
   }

   @Override
   public void brief(HandoffContext ctx) {
      Knowledge knowledge = ctx.knowledge();
      AnimalHandle handle = ctx.handle();
      long age = ctx.age();
      if (knowledge.bed() == null) {
         knowledge.searchBed(handle, age);
      }

      this.rest.bedDownAt(knowledge.bed(), Ground.arrivalTolerance(handle));
      DriveContext drives = ctx.drives();
      this.rest
         .huddleBy(PackDrive.centroid(drives), Crowding.huddle(handle.bounds().getXsize(), drives.trait(Trait.SYMPATHY), drives.trait(Trait.NERVOUSNESS)));
      this.rest.overcome(RestDrive.overcome(drives));
      if (this.rest.wantsConfirmation(handle)) {
         knowledge.confirmBed(handle, age);
         this.rest.confirmed();
         this.rest.bedDownAt(knowledge.bed(), Ground.arrivalTolerance(handle));
      }
   }
}

package net.magister.liberanimalium.fauna.nature;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.behavior.Pilfer;
import net.magister.liberanimalium.fauna.drive.DriveContext;
import net.magister.liberanimalium.fauna.drive.PilferDrive;
import net.magister.liberanimalium.fauna.know.Larder;

public final class PilferHandoff implements Handoff {
   private final Pilfer pilfer;
   @Nullable
   private UUID incumbent;

   public PilferHandoff(Pilfer pilfer) {
      this.pilfer = pilfer;
   }

   @Override
   public void brief(HandoffContext ctx) {
      DriveContext drives = ctx.drives();
      UUID robbed = drives.loot();
      if (robbed != null) {
         this.pilfer.target(robbed, drives.whereIs(robbed));
      } else {
         Larder.Offer best = PilferDrive.bestOffer(drives, this.incumbent);
         this.incumbent = best == null ? null : best.holder();
         this.pilfer.target(this.incumbent, this.incumbent == null ? null : drives.whereIs(this.incumbent));
      }
   }
}

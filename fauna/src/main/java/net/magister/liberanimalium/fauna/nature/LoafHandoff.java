package net.magister.liberanimalium.fauna.nature;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.behavior.Loaf;
import net.magister.liberanimalium.fauna.drive.DriveContext;
import net.magister.liberanimalium.fauna.drive.LoafDrive;
import net.magister.liberanimalium.fauna.know.Chests;
import net.minecraft.core.BlockPos;

public final class LoafHandoff implements Handoff {
   private final Loaf loaf;
   @Nullable
   private BlockPos incumbent;

   public LoafHandoff(Loaf loaf) {
      this.loaf = loaf;
   }

   @Override
   public void brief(HandoffContext ctx) {
      DriveContext drives = ctx.drives();
      Chests.Chest best = LoafDrive.bestChest(drives, this.incumbent);
      this.incumbent = best == null ? null : best.at();
      this.loaf.sitOn(this.incumbent, best == null ? null : drives.whereIs(best.opener()));
   }

   @Override
   public void debrief(HandoffContext ctx) {
      boolean gaveUp = this.loaf.takeGaveUp() || ctx.routeFailed();
      BlockPos at = this.incumbent;
      if (at != null && gaveUp) {
         ctx.knowledge().faculty(Chests.class).ifPresent(chests -> chests.forget(at));
         this.incumbent = null;
      }
   }
}

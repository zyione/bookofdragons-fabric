package net.magister.liberanimalium.fauna.drive;

import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Representation;
import net.minecraft.util.Mth;

public final class Arousal {
   private Arousal() {
   }

   public static float arousal(DriveContext context) {
      float fear = ThreatDrive.threatStore(context);
      Representation enemy = EnmityDrive.worstEnemy(context, null);
      float war = enemy == null ? 0.0F : EnmityDrive.menace(context, enemy);
      float rival = RivalDrive.pressure(context);
      return Mth.clamp(Math.max(fear, Math.max(war, rival)), 0.0F, 1.0F);
   }

   public static float absorption(DriveContext context) {
      float t = Mth.clamp((context.trait(Trait.BRAVERY) + context.trait(Trait.AGGRESSION)) / 2.0F, 0.0F, 1.0F);
      return Mth.lerp(t, 0.4F, 0.95F);
   }

   public static float appetite(DriveContext context) {
      return Mth.clamp(1.0F - arousal(context) * absorption(context), 0.0F, 1.0F);
   }
}

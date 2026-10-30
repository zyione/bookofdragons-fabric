package net.magister.liberanimalium.fauna.core;

import java.util.UUID;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.Order;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.api.Stance;
import net.magister.liberanimalium.fauna.drive.Drive;
import net.magister.liberanimalium.fauna.drive.PlayDrive;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.SocialBook;

final class Regard {
   private Regard() {
   }

   static float read(FaunaMind mind, Stance stance, float wariness) {
      if (mind.engagedId() != null) {
         return of(true, false, false, 0.0F, false, 0.0F, 0.0F, false);
      }

      UUID owner = Halter.of(mind.handle()).ownerId();
      UUID watched = mind.gazeHold().creature();
      if (watched == null && holds(mind, "command")) {
         watched = owner;
      }

      float like = 0.0F;
      float fear = 0.0F;
      boolean kin = false;
      if (watched != null) {
         SocialBook.Opinion opinion = mind.knowledge().social().of(watched);
         like = opinion.like();
         fear = opinion.fear();
         Representation rep = mind.knowledge().of(watched);
         kin = watched.equals(owner) || rep != null && rep.relation().type() == RelationType.PACK;
      }

      boolean playing = holds(mind, "play") || mind.arbiter().drives().stream().anyMatch(d -> "play".equals(d.id())) && PlayDrive.playful(mind.context());
      return of(false, stance != Stance.NONE, mind.maneuvers().concealing() || creepingWithOwner(mind), wariness, playing, like, fear, kin);
   }

   static boolean creepingWithOwner(FaunaMind mind) {
      if (!holds(mind, "command")) {
         return false;
      }

      UUID owner = Halter.of(mind.handle()).ownerId();
      return owner != null
         && Halter.of(mind.handle()).order() == Order.FOLLOW
         && Halter.of(mind.handle()).ownerCrouching()
         && !Halter.of(mind.handle()).riddenBy(owner);
   }

   private static boolean holds(FaunaMind mind, String drive) {
      Drive held = mind.holder();
      return held != null && drive.equals(held.id());
   }

   static float of(boolean engaged, boolean bristling, boolean stalking, float wariness, boolean playful, float like, float fear, boolean kin) {
      if (engaged) {
         return -1.0F;
      }

      float narrow = Math.max(Math.max(bristling ? 0.7F : 0.0F, stalking ? 0.8F : 0.0F), Math.max(Math.max(wariness, fear), -like));
      float wide = Math.max(Math.max(playful ? 0.8F : 0.0F, like), kin ? 0.5F : 0.0F);
      return Math.max(-1.0F, Math.min(1.0F, wide - narrow));
   }
}

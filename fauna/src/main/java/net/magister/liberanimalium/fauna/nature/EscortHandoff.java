package net.magister.liberanimalium.fauna.nature;

import net.magister.liberanimalium.fauna.behavior.Escort;
import net.magister.liberanimalium.fauna.drive.FriendDrive;
import net.magister.liberanimalium.fauna.know.Representation;

public final class EscortHandoff implements Handoff {
   private final Escort escort;

   public EscortHandoff(Escort escort) {
      this.escort = escort;
   }

   @Override
   public void brief(HandoffContext ctx) {
      Representation friend = FriendDrive.bestFriend(ctx.drives());
      this.escort.accompany(friend == null ? null : friend.id(), friend == null ? null : ctx.knowledge().estimate(friend, ctx.age(), ctx.handle().species()));
   }
}

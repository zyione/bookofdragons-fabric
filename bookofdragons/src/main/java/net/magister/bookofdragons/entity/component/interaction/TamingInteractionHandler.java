package net.magister.bookofdragons.entity.component.interaction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.magister.liberanimalium.body.gameplay.TamingPredicate;
import net.magister.liberanimalium.body.touch.TouchState;
import net.magister.liberanimalium.body.touch.TouchState.Gesture;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.interaction.IFaunaInteractionHandler;
import net.magister.liberanimalium.fauna.api.Touch;
import net.magister.liberanimalium.fauna.api.Check.Engagement;
import net.magister.liberanimalium.fauna.api.Ladder.StageDef;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.magister.liberanimalium.fauna.core.PairSession;
import net.magister.liberanimalium.fauna.know.Courtship;
import net.magister.liberanimalium.fauna.know.Knowledge;
import net.magister.liberanimalium.fauna.know.Courtship.TouchFace;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.network.TouchGestureMessage;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public class TamingInteractionHandler implements IFaunaInteractionHandler {
   private static final int BRUSH_STROKE_TICKS = 10;
   private final Map<UUID, TamingInteractionHandler.Hold> holds = new HashMap<>();

   public InteractionResult handle(FaunaBase dragon, Player player, InteractionHand hand) {
      if (dragon.isTame()) {
         return InteractionResult.PASS;
      }

      FaunaMind mind = dragon.faunaMind();
      if (mind == null) {
         return InteractionResult.PASS;
      }

      UUID courter = player.getUUID();
      Knowledge knowledge = mind.knowledge();
      StageDef stage = knowledge.courting(dragon, knowledge.about(courter)) && knowledge.courtship().playing(courter)
         ? mind.ladderFor().stageOf(Engagement.TOUCH)
         : null;
      boolean fits = fits(dragon, player, hand, stage);
      PairSession open = PairSession.find(dragon.faunaId(), courter);
      boolean playing = open != null && "play".equals(open.kind());
      if (!playing && !fits && !player.getItemInHand(hand).isEmpty()) {
         return InteractionResult.PASS;
      }

      TamingInteractionHandler.Hold hold = this.accumulate(courter, dragon.level().getGameTime());
      if (hold.held <= 0 || !playing && stage != null && stage.holdTicks() != 0) {
         if (playing) {
            open.join(courter);
         }

         Gesture want = fits && stage != null ? (stage.item() == null ? Gesture.REACH : Gesture.BRUSH) : Gesture.NONE;
         gesture(player, hold, want);
         if (want == Gesture.BRUSH && hold.held % 10 < 4) {
            dragon.level().playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1.0F, 1.0F);
         }

         mind.perception().deliver(dragon, new Touch(courter, dragon.kindOfAttacker(player), player.position(), mind.age(), hold.held, fits));
         TouchFace face = Courtship.faceOf(dragon.tamingMatrix().act("groom"), stage, fits, hold.held, playing);
         if (face == TouchFace.CHECK) {
            hold.held = 0;
         }

         return face == TouchFace.CATCH ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
      } else {
         return InteractionResult.CONSUME;
      }
   }

   public void tickHolds(FaunaBase dragon) {
      if (!dragon.level().isClientSide() && !this.holds.isEmpty()) {
         long now = dragon.level().getGameTime();
         Iterator<Entry<UUID, TamingInteractionHandler.Hold>> it = this.holds.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, TamingInteractionHandler.Hold> entry = it.next();
            TamingInteractionHandler.Hold hold = entry.getValue();
            if (now - hold.last > 12L) {
               Player who = dragon.level().getPlayerByUUID(entry.getKey());
               if (who != null) {
                  gesture(who, hold, Gesture.NONE);
               }

               it.remove();
            }
         }
      }
   }

   private TamingInteractionHandler.Hold accumulate(UUID courter, long now) {
      TamingInteractionHandler.Hold hold = this.holds.computeIfAbsent(courter, id -> {
         TamingInteractionHandler.Hold fresh = new TamingInteractionHandler.Hold();
         fresh.last = Long.MIN_VALUE;
         return fresh;
      });
      long gap = hold.last == Long.MIN_VALUE ? Long.MAX_VALUE : now - hold.last;
      hold.held = gap >= 0L && gap <= 12L ? hold.held + (int)gap : 0;
      hold.last = now;
      return hold;
   }

   private static void gesture(Player player, TamingInteractionHandler.Hold hold, Gesture want) {
      if (hold.gesture != want) {
         hold.gesture = want;
         TouchState.set(player.getUUID(), false, want);
         FaunaNetwork.sendToClientsTrackingAndSelf(new TouchGestureMessage(player.getId(), want), player);
      }
   }

   public int holdFor(UUID courter) {
      TamingInteractionHandler.Hold hold = this.holds.get(courter);
      return hold == null ? 0 : hold.held;
   }

   private static boolean fits(FaunaBase dragon, Player player, InteractionHand hand, StageDef stage) {
      if (stage == null || stage.check().engages() != Engagement.TOUCH) {
         return false;
      }

      if (stage.crouched() && !player.isShiftKeyDown()) {
         return false;
      }

      TamingPredicate wanted = TamingPredicate.forKey(stage.item());
      return wanted == null ? player.getItemInHand(hand).isEmpty() : wanted.test(dragon, player);
   }

   private static final class Hold {
      long last;
      int held;
      Gesture gesture = Gesture.NONE;
   }
}

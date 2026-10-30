package net.magister.liberanimalium.anim;

import java.util.HashMap;
import java.util.Map;
import net.magister.liberanimalium.anim.api.AnimatedBody;
import net.magister.liberanimalium.anim.event.EventRow;
import net.magister.liberanimalium.anim.event.EventTable;
import net.magister.liberanimalium.anim.event.ExpressionPlayer;
import net.magister.liberanimalium.anim.event.ExpressionSchedule;
import net.magister.liberanimalium.network.FaunaExpressionEventMessage;
import net.minecraft.client.Minecraft;

public final class ExpressionEventClient {
   private static final Map<Integer, ExpressionSchedule> SCHEDULES = new HashMap<>();

   private ExpressionEventClient() {
   }

   public static void clear() {
      SCHEDULES.clear();
   }

   public static void accept(FaunaExpressionEventMessage msg) {
      Minecraft client = Minecraft.getInstance();
      if (client.level != null) {
         if (client.level.getEntity(msg.entityId()) instanceof AnimatedBody body) {
            EventTable table = tableFor(body);
            ExpressionSchedule schedule = SCHEDULES.computeIfAbsent(msg.entityId(), id -> new ExpressionSchedule());
            String due = schedule.accept(msg.key(), client.level.getGameTime(), table);
            if (due != null) {
               EventRow row = table.row(due);
               if (row != null) {
                  ExpressionPlayer.play(body, row);
               }
            }
         }
      }
   }

   public static void tick(AnimatedBody body, int entityId, long gameTime) {
      ExpressionSchedule schedule = SCHEDULES.get(entityId);
      if (schedule != null) {
         EventTable table = tableFor(body);

         for (String key : schedule.due(gameTime, table)) {
            EventRow row = table.row(key);
            if (row != null) {
               ExpressionPlayer.play(body, row);
            }
         }
      }
   }

   public static void fireLocal(AnimatedBody body, int entityId, String key, long gameTime) {
      EventTable table = tableFor(body);
      ExpressionSchedule schedule = SCHEDULES.computeIfAbsent(entityId, id -> new ExpressionSchedule());
      String due = schedule.accept(key, gameTime, table);
      if (due != null) {
         EventRow row = table.row(due);
         if (row != null) {
            ExpressionPlayer.play(body, row);
         }
      }
   }

   public static void forget(int entityId) {
      SCHEDULES.remove(entityId);
   }

   private static EventTable tableFor(AnimatedBody body) {
      return body.animEvents();
   }
}

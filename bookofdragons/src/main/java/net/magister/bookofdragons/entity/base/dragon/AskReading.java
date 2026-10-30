package net.magister.bookofdragons.entity.base.dragon;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.magister.bookofdragons.discovery.DiscoveryManager;
import net.magister.bookofdragons.network.ControlNetwork;
import net.magister.bookofdragons.network.message.BookStripMessage;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class AskReading {
   static final long DWELL_TICKS = 40L;
   static final long COOLDOWN_TICKS = 1200L;
   static final double READ_RANGE = 24.0;
   static final double LOOK_MARGIN = 0.5;
   private static final int PERIOD = 2;
   private static final Map<UUID, AskReading.Reader> READERS = new HashMap<>();

   private AskReading() {
   }

   public static void init() {
      CommonEvents.SERVER_TICK_END.register(AskReading::tick);
      CommonEvents.PLAYER_LOGOUT.register((Consumer<ServerPlayer>)player -> READERS.remove(player.getUUID()));
   }

   static void say(DragonBase dragon, ServerPlayer player, String kind, String act) {
      if (!kind.equals("ask")) {
         deliver(dragon, player, kind, act);
      } else {
         AskReading.Reader reader = READERS.computeIfAbsent(player.getUUID(), id -> new AskReading.Reader());
         AskReading.Ask ask = reader.asks.computeIfAbsent(dragon.getId(), id -> new AskReading.Ask());
         ask.act = act;
         long now = player.level().getGameTime();
         if (reader.looking == dragon.getId() && now - reader.since >= 40L && !act.equals(ask.shown)) {
            show(dragon, player, ask, now);
         }
      }
   }

   private static void tick(MinecraftServer server) {
      if (server.getTickCount() % 2 == 0 && !READERS.isEmpty()) {
         Iterator<Entry<UUID, AskReading.Reader>> it = READERS.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, AskReading.Reader> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
               it.remove();
            } else {
               AskReading.Reader reader = entry.getValue();
               reader.asks.keySet().removeIf(idx -> wild(player, idx) == null);
               if (!reader.asks.isEmpty()) {
                  long now = player.level().getGameTime();
                  DragonBase seen = looked(reader, player);
                  int id = seen == null ? -1 : seen.getId();
                  if (id != reader.looking) {
                     reader.looking = id;
                     reader.since = now;
                  }

                  if (seen != null && now - reader.since >= 40L) {
                     AskReading.Ask ask = reader.asks.get(id);
                     if (ask.act != null && (!ask.act.equals(ask.shown) || now - ask.shownAt >= 1200L)) {
                        show(seen, player, ask, now);
                     }
                  }
               }
            }
         }
      }
   }

   private static void show(DragonBase dragon, ServerPlayer player, AskReading.Ask ask, long now) {
      ask.shown = ask.act;
      ask.shownAt = now;
      deliver(dragon, player, "ask", ask.act);
   }

   private static DragonBase looked(AskReading.Reader reader, ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(player.getViewVector(1.0F).scale(24.0));
      DragonBase best = null;
      double bestDistance = Double.MAX_VALUE;

      for (int id : reader.asks.keySet()) {
         DragonBase d = wild(player, id);
         if (d != null) {
            AABB box = d.getBoundingBox().inflate(0.5);
            Vec3 hit = box.contains(eye) ? eye : (Vec3)box.clip(eye, end).orElse(null);
            if (hit != null) {
               double distance = eye.distanceToSqr(hit);
               if (!(distance >= bestDistance)) {
                  HitResult wall = player.level().clip(new ClipContext(eye, hit, Block.VISUAL, Fluid.NONE, player));
                  if (wall.getType() != Type.BLOCK || !(eye.distanceToSqr(wall.getLocation()) < distance - 1.0E-4)) {
                     bestDistance = distance;
                     best = d;
                  }
               }
            }
         }
      }

      return best;
   }

   private static DragonBase wild(ServerPlayer player, int id) {
      return player.level().getEntity(id) instanceof DragonBase d && d.isAlive() && !d.isTame() ? d : null;
   }

   private static void deliver(DragonBase dragon, ServerPlayer player, String kind, String act) {
      if (kind.equals("ask")) {
         DiscoveryManager.learn(player, dragon.getDragonType(), "ask:" + act);
      } else if (!kind.equals("lost")) {
         DiscoveryManager.learn(player, dragon.getDragonType(), "phase:" + kind);
      }

      ControlNetwork.sendToPlayer(new BookStripMessage(dragon.getId(), kind, act), player);
   }

   private static final class Ask {
      String act;
      String shown;
      long shownAt = Long.MIN_VALUE;
   }

   private static final class Reader {
      final Map<Integer, AskReading.Ask> asks = new HashMap<>();
      int looking = -1;
      long since;
   }
}

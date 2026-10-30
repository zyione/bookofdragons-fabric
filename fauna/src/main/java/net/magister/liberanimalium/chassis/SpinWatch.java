package net.magister.liberanimalium.chassis;

import com.mojang.logging.LogUtils;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.magister.liberanimalium.body.state.TransportMode;
import net.magister.liberanimalium.fauna.api.MoveIntent;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class SpinWatch {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final int WINDOW_TICKS = 100;
   static final float TURN_DEGREES = 720.0F;
   static final double MOVED_BLOCKS = 1.5;
   static final int QUIET_TICKS = 600;
   private static final Map<FaunaBase, SpinWatch.State> STATES = new WeakHashMap<>();

   private SpinWatch() {
   }

   static void serverTick(FaunaBase body, MoveIntent intent) {
      if (body.getTransportMode() == TransportMode.GROUNDED && !body.steeredByRider() && !body.isPassenger()) {
         SpinWatch.State s = STATES.computeIfAbsent(body, b -> new SpinWatch.State());
         float yaw = body.getYRot();
         if (!Float.isNaN(s.yaw)) {
            s.turned = s.turned + Math.abs(Mth.wrapDegrees(yaw - s.yaw));
         }

         s.yaw = yaw;
         int now = body.tickCount;
         if (s.anchor == null || now - s.since >= 100) {
            if (s.anchor != null && s.turned >= 720.0F && now >= s.quietUntil) {
               double moved = body.position().distanceTo(s.anchor);
               if (moved < 1.5) {
                  s.quietUntil = now + 600;
                  say(body, intent, s.turned, now - s.since, moved);
               }
            }

            s.anchor = body.position();
            s.since = now;
            s.turned = 0.0F;
         }
      } else {
         STATES.remove(body);
      }
   }

   private static void say(FaunaBase body, MoveIntent intent, float turned, int ticks, double moved) {
      FaunaMind mind = body.faunaMind();
      String holder = mind != null && mind.holder() != null ? mind.holder().id() : "none";
      String route = mind == null ? "no mind" : (mind.route().idle() ? "idle" : mind.route().progress());
      Vec3 node = mind == null ? null : mind.route().currentNodePoint();
      Vec3 at = body.position();
      String intentText = intent == null
         ? "none"
         : String.format(
            Locale.ROOT, "dir=(%.2f,%.2f) speed=%.2f moving=%s", intent.direction().x, intent.direction().z, intent.speed(), intent.moving()
         );
      LOGGER.debug(
         "[Spin] {} #{} at ({}, {}, {}) turned {} deg in {} ticks, moved {} | drive={} route={} node={} | intent {} | yaw={} onGround={} wall={} on={} water={}",
         new Object[]{
            body.getName().getString(),
            body.getId(),
            f(at.x),
            f(at.y),
            f(at.z),
            Math.round(turned),
            ticks,
            f(moved),
            holder,
            route,
            node == null ? "none" : "(" + f(node.x) + ", " + f(node.y) + ", " + f(node.z) + ")",
            intentText,
            f(body.getYRot()),
            body.onGround(),
            body.horizontalCollision,
            body.getBlockStateOn().getBlock().getDescriptionId(),
            body.isInWater()
         }
      );
   }

   private static String f(double v) {
      return String.format(Locale.ROOT, "%.2f", v);
   }

   private static final class State {
      float yaw = Float.NaN;
      float turned;
      Vec3 anchor;
      int since;
      int quietUntil = Integer.MIN_VALUE;
   }
}

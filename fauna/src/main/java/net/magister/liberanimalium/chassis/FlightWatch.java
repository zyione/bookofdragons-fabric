package net.magister.liberanimalium.chassis;

import com.mojang.logging.LogUtils;
import java.util.Locale;
import net.magister.liberanimalium.fauna.api.MoveIntent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

public final class FlightWatch {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static volatile int watched = -1;
   private static volatile int remaining = 0;
   private static float lastYaw = Float.NaN;
   private static int lastClientTick = Integer.MIN_VALUE;
   private static volatile String renderBranch = "?";

   private FlightWatch() {
   }

   public static void watch(int entityId, int ticks) {
      watched = entityId;
      remaining = ticks;
      lastYaw = Float.NaN;
      lastClientTick = Integer.MIN_VALUE;
      LOGGER.debug("[FlightWatch] watching entity {} for {} ticks", entityId, ticks);
   }

   public static boolean watching(int entityId) {
      return watched == entityId && remaining > 0;
   }

   public static void serverTick(FaunaBase d, MoveIntent intent) {
      SpinWatch.serverTick(d, intent);
      if (watching(d.getId())) {
         remaining--;
         float yaw = d.yBodyRot;
         float turned = Float.isNaN(lastYaw) ? 0.0F : Mth.wrapDegrees(yaw - lastYaw);
         lastYaw = yaw;
         Vec3 v = d.getDeltaMovement();
         String intentText = intent == null
            ? "none"
            : String.format(
               Locale.ROOT,
               "dir=(%.2f,%.2f,%.2f) speed=%.2f mode=%s moving=%s",
               intent.direction().x,
               intent.direction().y,
               intent.direction().z,
               intent.speed(),
               intent.mode(),
               intent.moving()
            );
         LOGGER.debug(
            "[FlightWatch S] {} t={} transport={} flying={} stoodOn={} steered={} vehicle={} | intent {} | syncedPitch={} syncedRoll={} xRot={} yBodyRot={} turned={}/t calcPitch={} throttle={} | vel=({},{},{}) onGround={}",
            new Object[]{
               d.getName().getString(),
               d.tickCount,
               d.getTransportMode(),
               d.isFlying(),
               d.stoodOn(),
               d.steeredByRider(),
               d.isVehicle(),
               intentText,
               f(d.getSyncedPitch()),
               f(d.getSyncedRoll()),
               f(d.getXRot()),
               f(yaw),
               f(turned),
               f(d.getStateContext() == null ? Float.NaN : d.getStateContext().getCalculatedPitch()),
               f(d.getThrottleLevel()),
               f((float)v.x),
               f((float)v.y),
               f((float)v.z),
               d.onGround()
            }
         );
         if (remaining == 0) {
            LOGGER.debug("[FlightWatch] done");
         }
      }
   }

   public static void clientFrame(
      FaunaBase d, float targetPitch, float targetRoll, float smoothedPitch, float smoothedRoll, boolean rolling, Quaternionf orientation
   ) {
      if (watching(d.getId()) && d.tickCount != lastClientTick) {
         lastClientTick = d.tickCount;
         Vector3f euler = orientation == null ? new Vector3f(Float.NaN) : orientation.getEulerAnglesXYZ(new Vector3f());
         LOGGER.debug(
            "[FlightWatch C] {} t={} transport={} flying={} | targets pitch={} roll={} (rad) | smoothed pitch={} roll={} | isRolling={} | syncedPitch={} syncedRoll={} xRot={} | orientation euler=({},{},{}) deg | renderFlightBranch={}",
            new Object[]{
               d.getName().getString(),
               d.tickCount,
               d.getTransportMode(),
               d.isFlying(),
               f(targetPitch),
               f(targetRoll),
               f(smoothedPitch),
               f(smoothedRoll),
               rolling,
               f(d.getSyncedPitch()),
               f(d.getSyncedRoll()),
               f(d.getXRot()),
               f((float)Math.toDegrees(euler.x)),
               f((float)Math.toDegrees(euler.y)),
               f((float)Math.toDegrees(euler.z)),
               renderBranch
            }
         );
      }
   }

   public static void renderBranch(FaunaBase d, String what) {
      if (watched == d.getId()) {
         renderBranch = what;
      }
   }

   private static String f(float v) {
      return String.format(Locale.ROOT, "%.2f", v);
   }
}

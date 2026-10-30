package net.magister.liberanimalium.ride.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;

public class CameraShakeUtil {
   private static float shakeIntensity = 0.0F;
   private static long shakeStartTime = 0L;
   private static boolean isShaking = false;
   private static final RandomSource RANDOM = RandomSource.create();
   private static long pausedAt = 0L;
   private static long pausedFor = 0L;
   private static final long SHAKE_DURATION_MS = 2000L;

   public static void startShake(float intensity) {
      shakeIntensity = intensity;
      shakeStartTime = playMillis();
      isShaking = true;
   }

   public static boolean isShaking() {
      if (!isShaking) {
         return false;
      } else {
         long elapsed = playMillis() - shakeStartTime;
         if (elapsed >= 2000L) {
            isShaking = false;
            shakeIntensity = 0.0F;
            return false;
         } else {
            return true;
         }
      }
   }

   public static float getShakePitch() {
      if (isShaking() && !Minecraft.getInstance().isPaused()) {
         long elapsed = playMillis() - shakeStartTime;
         float progress = (float)elapsed / 2000.0F;
         float falloff = 1.0F - progress;
         return (RANDOM.nextFloat() - 0.5F) * 2.0F * shakeIntensity * falloff;
      } else {
         return 0.0F;
      }
   }

   public static float getShakeYaw() {
      if (isShaking() && !Minecraft.getInstance().isPaused()) {
         long elapsed = playMillis() - shakeStartTime;
         float progress = (float)elapsed / 2000.0F;
         float falloff = 1.0F - progress;
         return (RANDOM.nextFloat() - 0.5F) * 2.0F * shakeIntensity * falloff;
      } else {
         return 0.0F;
      }
   }

   private static long playMillis() {
      long now = System.currentTimeMillis();
      boolean paused = Minecraft.getInstance().isPaused();
      if (paused && pausedAt == 0L) {
         pausedAt = now;
      } else if (!paused && pausedAt != 0L) {
         pausedFor = pausedFor + (now - pausedAt);
         pausedAt = 0L;
      }

      return (pausedAt != 0L ? pausedAt : now) - pausedFor;
   }

   public static void reset() {
      shakeIntensity = 0.0F;
      shakeStartTime = 0L;
      isShaking = false;
   }
}

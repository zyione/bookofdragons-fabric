package net.magister.liberanimalium.body.anim.data;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

public class BoneAccessCache {
   private final Map<String, Vec3> smoothedRotations = new HashMap<>();
   private final Map<String, BoneAccessCache.WingPeakState> wingPeakStates = new HashMap<>();
   private int lastFlapSoundTick = Integer.MIN_VALUE;
   private float smoothedProximityScale = 0.0F;
   private double gaitPhase = 0.0;
   private double gaitPhasePrev = 0.0;
   private int lastGaitTick = -1;
   private final Map<String, Float> smoothedSpeedFactors = new HashMap<>();

   public Vec3 getSmoothedRotation(String key) {
      return this.smoothedRotations.getOrDefault(key, Vec3.ZERO);
   }

   public void setSmoothedRotation(String key, Vec3 rotation) {
      this.smoothedRotations.put(key, rotation);
   }

   public BoneAccessCache.WingPeakState getWingPeakState(String wingKey) {
      return this.wingPeakStates.computeIfAbsent(wingKey, k -> new BoneAccessCache.WingPeakState());
   }

   public boolean admitFlapSound(int tick, int gapTicks) {
      if (this.lastFlapSoundTick != Integer.MIN_VALUE && tick >= this.lastFlapSoundTick && tick - this.lastFlapSoundTick < gapTicks) {
         return false;
      }

      this.lastFlapSoundTick = tick;
      return true;
   }

   public void clear() {
      this.smoothedRotations.clear();
      this.wingPeakStates.clear();
      this.lastFlapSoundTick = Integer.MIN_VALUE;
      this.smoothedProximityScale = 0.0F;
      this.gaitPhase = 0.0;
      this.gaitPhasePrev = 0.0;
      this.lastGaitTick = -1;
      this.smoothedSpeedFactors.clear();
   }

   public double getGaitPhase() {
      return this.gaitPhase;
   }

   public double getGaitPhasePrev() {
      return this.gaitPhasePrev;
   }

   public int getLastGaitTick() {
      return this.lastGaitTick;
   }

   public void setGaitPhase(double phase) {
      this.gaitPhase = phase;
   }

   public void setGaitPhasePrev(double phase) {
      this.gaitPhasePrev = phase;
   }

   public void setLastGaitTick(int tick) {
      this.lastGaitTick = tick;
   }

   public float getSmoothedSpeedFactor(String controller, float fallback) {
      return this.smoothedSpeedFactors.getOrDefault(controller, fallback);
   }

   public void setSmoothedSpeedFactor(String controller, float factor) {
      this.smoothedSpeedFactors.put(controller, factor);
   }

   public float getSmoothedProximityScale() {
      return this.smoothedProximityScale;
   }

   public void setSmoothedProximityScale(float scale) {
      this.smoothedProximityScale = scale;
   }

   public static class WingPeakState {
      public boolean seeded;
      public boolean movingUp;
      public float peakRotation;
      public float strokeFrom;
      public long strokeTick;
      public long washTick = -4611686018427387904L;
   }
}

package net.magister.liberanimalium.render;

import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.body.anim.data.BoneAccessCache;
import net.magister.liberanimalium.body.anim.extraction.PerEntityWingBoneExtractor;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.render.vfx.FaunaFlightParticleHelper;
import net.magister.liberanimalium.sound.FaunaSoundHelper;

final class ExtractPass implements RigPass {
   private static final int FLAP_SOUND_GAP_TICKS = 4;
   private static final float WING_TURN = (float)Math.toRadians(3.0);
   private static final float WING_STROKE = (float)Math.toRadians(20.0);
   private static final float WING_STROKE_RATE = (float)Math.toRadians(1.75);
   private static final int WASH_GAP_TICKS = 5;

   @Override
   public String name() {
      return "extract";
   }

   @Override
   public void solve(RigFrame frame) {
      extractFrameData(frame, frame.partialTick);
   }

   private static void extractFrameData(RigFrame frame, float partialTick) {
      FaunaBase body = (FaunaBase)frame.body;
      AzBakedModel model = frame.model;
      long currentTime = body.level().getGameTime();
      String name = null;
      String leftWing = FaunaProceduralAnimator.first(frame, "left_wing");
      String rightWing = FaunaProceduralAnimator.first(frame, "right_wing");
      AzBone leftWingBone = leftWing == null ? null : model.getBoneOrNull(leftWing);
      AzBone rightWingBone = rightWing == null ? null : model.getBoneOrNull(rightWing);
      if (leftWingBone != null || rightWingBone != null) {
         PerEntityWingBoneExtractor.extractAndStore(body.getId(), name, leftWingBone, rightWingBone, currentTime);
      }

      if (leftWingBone != null && rightWingBone != null) {
         PerEntityWingBoneExtractor.WingTipData tipData = PerEntityWingBoneExtractor.extractWorldTipPositions(body, model, partialTick);
         if (tipData != null) {
            FaunaRendererAz.WingTipPositionCache.store(body.getId(), "left", tipData.leftTip());
            FaunaRendererAz.WingTipPositionCache.store(body.getId(), "right", tipData.rightTip());
         }
      }

      trackWingFlapSounds(body);
   }

   private static void trackWingFlapSounds(FaunaBase body) {
      if (body.level().isClientSide()) {
         if (body.isFlying()) {
            long currentTime = body.level().getGameTime();
            PerEntityWingBoneExtractor.WingBoneData wingData = PerEntityWingBoneExtractor.getWingBoneData(body.getId(), currentTime);
            if (wingData != null) {
               BoneAccessCache cache = body.getBoneAccessCache();
               boolean leftBeat = processWingFlap(body, "left", wingData, cache);
               boolean rightBeat = processWingFlap(body, "right", wingData, cache);
               if ((leftBeat || rightBeat) && cache.admitFlapSound(body.tickCount, 4)) {
                  FaunaSoundHelper.playFlapSound(body);
               }
            }
         }
      }
   }

   private static boolean processWingFlap(FaunaBase body, String side, PerEntityWingBoneExtractor.WingBoneData wingData, BoneAccessCache cache) {
      long now = body.level().getGameTime();
      BoneAccessCache.WingPeakState state = cache.getWingPeakState(side);
      if (body.tickCount < 10) {
         state.seeded = false;
      }

      boolean beat = beat(state, wingData.getWingData(side).rotationY, now, side.equals("left"));
      if (beat && now - state.washTick >= 5L) {
         FaunaFlightParticleHelper.spawnWingWashParticles(body, side);
         state.washTick = now;
      }

      return beat;
   }

   static boolean beat(BoneAccessCache.WingPeakState state, float rotY, long now, boolean left) {
      if (!state.seeded) {
         state.seeded = true;
         state.peakRotation = rotY;
         state.strokeFrom = rotY;
         state.strokeTick = now;
         return false;
      }

      float back = state.movingUp ? state.peakRotation - rotY : rotY - state.peakRotation;
      if (back < 0.0F) {
         state.peakRotation = rotY;
         return false;
      }

      if (back <= WING_TURN) {
         return false;
      }

      float swept = Math.abs(state.peakRotation - state.strokeFrom);
      long took = Math.max(1L, now - state.strokeTick);
      boolean beat = swept >= WING_STROKE && swept / (float)took >= WING_STROKE_RATE && (left ? !state.movingUp : state.movingUp);
      state.strokeFrom = state.peakRotation;
      state.strokeTick = now;
      state.peakRotation = rotY;
      state.movingUp = !state.movingUp;
      return beat;
   }
}

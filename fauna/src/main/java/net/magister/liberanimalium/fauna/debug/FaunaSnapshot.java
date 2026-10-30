package net.magister.liberanimalium.fauna.debug;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.SoundKind;
import net.magister.liberanimalium.fauna.know.Company;
import net.minecraft.core.BlockPos;

public record FaunaSnapshot(
   UUID animalId,
   String speciesName,
   long age,
   boolean farThink,
   String mode,
   float health,
   float maxHealth,
   @Nullable BlockPos goal,
   List<BlockPos> nodes,
   List<Byte> nodeModes,
   int nodeIndex,
   String routeState,
   @Nullable String routeFailure,
   List<String> driveNames,
   List<Float> driveScores,
   int holderIndex,
   float hunger,
   float releaseLevel,
   float energy,
   float annoyance,
   float compliance,
   float complianceEq,
   String orderWord,
   float fightClaim,
   float demandStay,
   float demandFollow,
   String beat,
   String rung,
   int beatTicks,
   String beatRunnerUp,
   float beatMargin,
   float[] posture,
   String weapon,
   boolean firing,
   boolean outrun,
   boolean tankSpent,
   boolean breakingOff,
   float mysteries,
   float freshGround,
   @Nullable BlockPos territoryCenter,
   float territoryRadius,
   @Nullable BlockPos roostAnchor,
   @Nullable BlockPos avoidedCenter,
   float avoidedRadius,
   long exileRemainingTicks,
   List<FaunaSnapshot.Known> known,
   List<Company.Source> company,
   List<FaunaSnapshot.Ping> pings,
   float[] interest,
   float[] danger,
   @Nullable float[] intent,
   float[] facing,
   float[] eyeOffset,
   float ownSpeed,
   float sightRadius,
   float perfectVisionDeg,
   float peripheralDeg,
   float blindSpotDeg,
   boolean nightVision,
   float hearingSkill,
   int markCount,
   float stuckPressure,
   float[] temper,
   int senseRuns,
   int thinkRuns,
   int steerRuns,
   float sleepDebt,
   float sleepPhase,
   float sleepTheta,
   boolean asleep,
   @Nullable BlockPos bed,
   float bedScore,
   String bedSource
) {
   public MoveMode modeAt(int i) {
      int ordinal = this.nodeModes.get(i);
      MoveMode[] values = MoveMode.values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : MoveMode.GROUND;
   }

   public record Known(
      UUID targetId,
      String typeKey,
      String relation,
      boolean visible,
      float find,
      float sightScore,
      long age,
      BlockPos lastKnown,
      BlockPos estimate,
      int lightLevel,
      boolean sneaking,
      boolean inWater,
      float manner,
      float attribution,
      float crowding
   ) {
   }

   public record Ping(boolean sound, int kind, float x, float y, float z, float strength, int age) {
      @Nullable
      public SoundKind soundKind() {
         SoundKind[] values = SoundKind.values();
         return this.sound && this.kind >= 0 && this.kind < values.length ? values[this.kind] : null;
      }
   }
}

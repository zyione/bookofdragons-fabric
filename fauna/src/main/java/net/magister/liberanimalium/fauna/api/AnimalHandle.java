package net.magister.liberanimalium.fauna.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.magister.liberanimalium.fauna.move.NavSolver;
import net.magister.liberanimalium.fauna.move.Ribbon;
import net.magister.liberanimalium.fauna.nature.Nature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface AnimalHandle {
   UUID faunaId();

   SpeciesProfile species();

   default <T> Optional<T> endowed(Class<T> endowment) {
      return endowment.isInstance(this) ? Optional.of(endowment.cast(this)) : Optional.empty();
   }

   Nature nature();

   default SpeciesProfile.Senses senses() {
      return this.species().senses().effectiveFor(this.asleep(), this.currentMode() == MoveMode.AIR);
   }

   default MoveMode modeAt(Vec3 point) {
      return MoveMode.GROUND;
   }

   FaunaMind faunaMind();

   Vec3 bodyPosition();

   Vec3 velocity();

   Vec3 eyePosition();

   Vec3 lookDirection();

   Vec3 sightDirection();

   AABB bounds();

   default double clearanceTo(double girth) {
      return (this.bounds().getXsize() + girth) * 0.5;
   }

   float health();

   float maxHealth();

   default float healthFraction() {
      float max = this.maxHealth();
      return max <= 0.0F ? 1.0F : Mth.clamp(this.health() / max, 0.0F, 1.0F);
   }

   float hungerLevel();

   float fatigueLevel();

   boolean asleep();

   long dayTime();

   Terrain terrain();

   MoveMode currentMode();

   boolean bodyOnGround();

   boolean inWater();

   boolean stoodOn();

   default boolean held() {
      return false;
   }

   long gameTime();

   default boolean peacefulWorld() {
      return false;
   }

   ServerLevel serverLevel();

   LivingEntity entity();

   NavSolver.NeighborSource navSource(BlockPos var1);

   float navRange();

   default boolean onDeck() {
      return false;
   }

   @Nullable
   default BlockPos deckGoal(BlockPos world) {
      return null;
   }

   default Vec3 keepPlace(Vec3 world) {
      return world;
   }

   @Nullable
   default Vec3 placeNow(Vec3 kept) {
      return kept;
   }

   @Nullable
   BlockPos findStandableNear(BlockPos var1);

   @Nullable
   default BlockPos findSwimmableNear(BlockPos approx) {
      return null;
   }

   void requestMoveMode(MoveMode var1);

   Map<MoveMode, Float> effectiveModeCost();

   boolean isBlockedAt(Vec3 var1);

   @Nullable
   default Vec3 passDoor(BlockPos cell) {
      return null;
   }

   boolean lineOfSight(Vec3 var1, Vec3 var2);

   default boolean sightLine(Vec3 from, Vec3 to) {
      return this.lineOfSight(from, to);
   }

   default boolean headClear() {
      return true;
   }

   float stepHeight();

   default double hopHeight() {
      return 0.0;
   }

   default boolean settleOn(BlockPos block) {
      return false;
   }

   default boolean leap(@Nullable UUID target, Vec3 at) {
      return false;
   }

   default boolean hop(double rise) {
      return false;
   }

   double groundHeightNear(Vec3 var1, double var2, double var4);

   default double turnRateDegrees(double speedPerTick) {
      return 360.0;
   }

   default double turnRadius(double speedPerTick) {
      return Ribbon.radius(speedPerTick, this.turnRateDegrees(speedPerTick));
   }

   default double topSpeed(MoveMode mode) {
      return 1.0;
   }

   default int modeChangeTicks() {
      return 0;
   }

   default boolean incapacitated() {
      return false;
   }

   List<SensedBody> scanNearby(double var1);

   List<SensedItem> scanItems(double var1);

   double nearestPlayerDistance(double var1);

   default double simulationRange() {
      return SectionPos.sectionToBlockCoord(this.serverLevel().getServer().getPlayerList().getSimulationDistance());
   }

   void applyMove(MoveIntent var1);

   void applyLook(Vec3 var1);

   void gazeAt(@Nullable Vec3 var1);

   default void gazeAt(@Nullable Vec3 target, @Nullable UUID creature) {
      this.gazeAt(target);
   }

   void setPose(PoseFlag var1, boolean var2);

   default boolean hasPose(PoseFlag flag) {
      return false;
   }

   void applySignals(Signals var1);

   void express(ExpressionKey var1);

   default void express(ExpressionKey key, @Nullable UUID about) {
      this.express(key);
   }

   void saveMind(CompoundTag var1);

   void loadMind(CompoundTag var1);
}

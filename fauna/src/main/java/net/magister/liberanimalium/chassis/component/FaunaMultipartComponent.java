package net.magister.liberanimalium.chassis.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.anim.rig.HeadTrackSpec;
import net.magister.liberanimalium.anim.rig.RigSpec;
import net.magister.liberanimalium.body.BodyPlan;
import net.magister.liberanimalium.body.BodyPlans;
import net.magister.liberanimalium.body.CullBox;
import net.magister.liberanimalium.body.PartIndex;
import net.magister.liberanimalium.body.PartReport;
import net.magister.liberanimalium.body.PartSpec;
import net.magister.liberanimalium.body.ReporterElection;
import net.magister.liberanimalium.body.RestChain;
import net.magister.liberanimalium.body.locomotion.WalkEngine;
import net.magister.liberanimalium.body.part.FaunaPartEntity;
import net.magister.liberanimalium.body.state.TransportMode;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.network.PartSyncPayload;
import net.magister.liberanimalium.platform.part.PartBody;
import net.magister.liberanimalium.platform.part.Parts;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FaunaMultipartComponent implements IFaunaComponent, PartIndex.Holder {
   private FaunaBase body;
   private BodyPlan plan = BodyPlan.EMPTY;
   private FaunaPartEntity[] parts = new FaunaPartEntity[0];
   private final List<FaunaPartEntity> heads = new ArrayList<>();
   private int[] towardPart = new int[0];
   @Nullable
   private PartReport report;
   @Nullable
   private Vec3[] observed;
   private int observedTick = Integer.MIN_VALUE;
   private int sentTick = Integer.MIN_VALUE;
   private int stillTicks;
   @Nullable
   private RigSpec composedRig;
   @Nullable
   private RigSpec composedFrom;
   private float scale = 1.0F;
   @Nullable
   private AABB partsUnion;
   private boolean unionDirty = true;
   private static final PerfClock.Section CLOCK = PerfClock.server("parts.server");

   @Override
   public PartBody<?>[] parts() {
      return this.parts;
   }

   @Override
   public AABB partsUnion() {
      if (this.unionDirty || this.partsUnion == null) {
         this.partsUnion = this.parts.length == 0 ? this.body.getBoundingBox() : PartIndex.unionOf(this.parts);
         this.unionDirty = false;
      }

      return this.partsUnion;
   }

   public void markUnionDirty() {
      this.unionDirty = true;
   }

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
      this.plan = BodyPlans.of(host.animSpeciesId());
      this.build();
   }

   private void build() {
      List<PartSpec> specs = this.plan.parts();
      FaunaPartEntity[] built = new FaunaPartEntity[specs.size()];
      int[] toward = new int[specs.size()];
      this.heads.clear();

      for (int i = 0; i < specs.size(); i++) {
         PartSpec spec = specs.get(i);
         built[i] = new FaunaPartEntity(this.body, spec, this.scale);
         built[i].own(this);
         if (spec.head()) {
            this.heads.add(built[i]);
         }

         toward[i] = -1;
         if (spec.elongated()) {
            for (int j = 0; j < specs.size(); j++) {
               if (specs.get(j).bone().equals(spec.toward())) {
                  toward[i] = j;
                  break;
               }
            }
         }
      }

      this.parts = built;
      this.towardPart = toward;
      this.unionDirty = true;
   }

   public BodyPlan plan() {
      return this.plan;
   }

   public AABB cullBox() {
      List<AABB> boxes = new ArrayList<>(this.parts.length);

      for (FaunaPartEntity part : this.parts) {
         boxes.add(part.getBoundingBox());
      }

      return CullBox.of(this.body.getBoundingBox(), boxes, this.body.getBbWidth());
   }

   public PartBody<?>[] getParts() {
      return this.parts;
   }

   public List<FaunaPartEntity> getHeadParts() {
      return this.heads;
   }

   public RigSpec rigFor(RigSpec binding) {
      if (this.composedRig == null || this.composedFrom != binding) {
         this.composedRig = binding.withBody(this.plan.prefixes(), this.plan.chains());
         this.composedFrom = binding;
      }

      return this.composedRig;
   }

   @Override
   public void serverTick() {
      if (this.body != null && this.parts.length != 0) {
         long clock = CLOCK.start();

         try {
            this.serverTickTimed();
         } finally {
            CLOCK.end(clock);
         }
      }
   }

   private void serverTickTimed() {
      this.rescale();
      this.elect();
      long now = this.body.level().getGameTime();
      if (this.report != null && !this.report.stale(now)) {
         this.placeFromReport();
      } else {
         this.placeFromRest(FaunaPartEntity.Rung.FALLBACK);
      }

      this.tickStill();
      this.pushOut();
   }

   public boolean stoodOn() {
      return this.stillTicks > 0;
   }

   private void tickStill() {
      if (this.stillTicks > 0) {
         this.stillTicks--;
      }

      if (this.hasPlayerOnTop()) {
         this.stillTicks = 10;
      }
   }

   private boolean hasPlayerOnTop() {
      if (this.body.getTransportMode() != TransportMode.GROUNDED) {
         return false;
      }

      AABB whole = this.partsUnion().minmax(this.body.getBoundingBox());

      for (Player player : this.body.level().players()) {
         if (!player.isSpectator()
            && !player.isPassenger()
            && !player.getAbilities().flying
            && !player.isFallFlying()
            && couldBeOnTop(whole, player.getX(), player.getY(), player.getZ())) {
            if (onTopOf(this.body.getBoundingBox(), player.getX(), player.getY(), player.getZ())) {
               return true;
            }

            for (FaunaPartEntity part : this.parts) {
               if (part.spec().walkable() && onTopOf(part.getBoundingBox(), player.getX(), player.getY(), player.getZ())) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   static boolean couldBeOnTop(AABB whole, double x, double y, double z) {
      double margin = 1.0;
      return x >= whole.minX - margin
         && x < whole.maxX + margin
         && z >= whole.minZ - margin
         && z < whole.maxZ + margin
         && y >= whole.minY
         && y < whole.maxY + whole.getYsize() * 0.5;
   }

   static boolean onTopOf(AABB surface, double x, double y, double z) {
      double margin = 1.0;
      AABB above = new AABB(
         surface.minX - margin,
         surface.maxY,
         surface.minZ - margin,
         surface.maxX + margin,
         surface.maxY + surface.getYsize() * 0.5,
         surface.maxZ + margin
      );
      return above.contains(x, y, z);
   }

   private void pushOut() {
      AABB union = this.partsUnion();

      for (Entity other : this.body.level().getEntities(this.body, union.inflate(0.2, 0.0, 0.2), EntitySelector.pushableBy(this.body))) {
         if (!Parts.isPart(other) && other.getRootVehicle() != this.body.getRootVehicle()) {
            for (FaunaPartEntity part : this.parts) {
               AABB reach = part.getBoundingBox().inflate(0.2, 0.0, 0.2);
               if (reach.intersects(other.getBoundingBox())) {
                  Vec3 shove = shoveFrom(part.centre(), other.position());
                  other.push(shove.x, 0.0, shove.z);
                  break;
               }
            }
         }
      }
   }

   static Vec3 shoveFrom(Vec3 centre, Vec3 at) {
      double dx = at.x - centre.x;
      double dz = at.z - centre.z;
      double distance = Mth.absMax(dx, dz);
      if (distance < 0.01) {
         return Vec3.ZERO;
      }

      dx /= distance;
      dz /= distance;
      double inverse = Math.min(1.0, 1.0 / distance);
      return new Vec3(dx * inverse * 0.05, 0.0, dz * inverse * 0.05);
   }

   public void accept(int senderId, long tick, short[] raw) {
      if (senderId == this.body.partReporter()) {
         if (PartReport.accept(raw, this.plan.reportSlots().size())) {
            if (this.report == null || tick >= this.report.tick()) {
               this.report = new PartReport(tick, PartReport.decode(raw));
            }
         }
      }
   }

   public int reporter() {
      return this.body.partReporter();
   }

   public long reportAge() {
      return this.report == null ? -1L : this.body.level().getGameTime() - this.report.tick();
   }

   private void elect() {
      if (this.body.level() instanceof ServerLevel level) {
         int var7 = -1;
         Entity controlling = this.body.getControllingPassenger();
         if (controlling instanceof Player) {
            Player player = (Player)controlling;
            var7 = player.getId();
         }

         List<ReporterElection.Candidate> candidates = new ArrayList<>();

         for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()) {
               candidates.add(new ReporterElection.Candidate(player.getId(), player.distanceToSqr(this.body)));
            }
         }

         int elected = ReporterElection.elect(this.body.partReporter(), var7, candidates);
         if (elected != this.body.partReporter()) {
            this.body.setPartReporter(elected);
            this.report = null;
         }
      }
   }

   private void placeFromReport() {
      Vec3 bodyAt = this.body.position();
      List<PartSpec> specs = this.plan.parts();

      for (int i = 0; i < this.parts.length; i++) {
         int slot = this.plan.slotOf(i);
         Vec3 bone = this.report.world(slot, bodyAt);
         Vec3 toward = specs.get(i).elongated() ? this.report.world(slot + 1, bodyAt) : null;
         this.parts[i].place(bone, toward, FaunaPartEntity.Rung.REPORT);
      }
   }

   private void placeFromRest(FaunaPartEntity.Rung rung) {
      float bodyYaw = WalkEngine.travelFacing(this.body, this.body.steeredByRider());
      Vec3[] bones = RestChain.solve(
         this.plan,
         this.body.position(),
         bodyYaw,
         this.body.getHeadTrackingSystem().getSmoothedHeadYaw(),
         this.body.getHeadTrackingSystem().getSmoothedHeadPitch(),
         HeadTrackSpec.DEFAULT
      );

      for (int i = 0; i < this.parts.length; i++) {
         Vec3 toward = this.towardPart[i] >= 0 ? bones[this.towardPart[i]] : null;
         this.parts[i].place(bones[i], toward, rung);
      }
   }

   @Nullable
   public Vec3 trackedPoint(String name) {
      int slot = this.plan.trackedSlot(name);
      if (slot < 0) {
         return null;
      } else if (!this.body.level().isClientSide()) {
         return this.report != null && !this.report.stale(this.body.level().getGameTime()) ? this.report.world(slot, this.body.position()) : null;
      } else {
         return this.observed != null && slot < this.observed.length ? this.observed[slot] : null;
      }
   }

   @Override
   public void clientTick() {
      if (this.body != null && this.parts.length != 0) {
         this.rescale();
         this.tickStill();
         if (this.body.tickCount - this.observedTick > 5) {
            this.placeFromRest(FaunaPartEntity.Rung.FALLBACK);
         }
      }
   }

   public void observe(Vec3[] slots, int localId) {
      if (slots.length == this.plan.reportSlots().size()) {
         this.observed = slots;
         this.observedTick = this.body.tickCount;
         List<PartSpec> specs = this.plan.parts();

         for (int i = 0; i < this.parts.length; i++) {
            int slot = this.plan.slotOf(i);
            Vec3 toward = specs.get(i).elongated() ? slots[slot + 1] : null;
            this.parts[i].place(slots[slot], toward, FaunaPartEntity.Rung.MODEL);
         }

         if (this.body.partReporter() == localId && this.sentTick != this.body.tickCount) {
            this.sentTick = this.body.tickCount;
            FaunaNetwork.sendToServer(
               new PartSyncPayload(this.body.getId(), this.body.level().getGameTime(), PartReport.encode(Arrays.asList(slots), this.body.position()))
            );
         }
      }
   }

   private void rescale() {
      float typeWidth = this.body.getType().getWidth();
      float now = typeWidth <= 0.0F ? 1.0F : this.body.getBbWidth() / typeWidth;
      if (!(Math.abs(now - this.scale) < 0.001F)) {
         this.scale = now;

         for (FaunaPartEntity part : this.parts) {
            part.resize(this.scale);
         }
      }
   }

   @Override
   public void save(CompoundTag tag) {
   }

   @Override
   public void load(CompoundTag tag) {
   }
}

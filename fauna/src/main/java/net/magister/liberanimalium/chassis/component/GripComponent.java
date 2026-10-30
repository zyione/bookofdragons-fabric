package net.magister.liberanimalium.chassis.component;

import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.BodyPlans;
import net.magister.liberanimalium.body.grip.DrawnHips;
import net.magister.liberanimalium.body.grip.Grip;
import net.magister.liberanimalium.body.grip.GripGeometry;
import net.magister.liberanimalium.body.grip.GripHang;
import net.magister.liberanimalium.body.grip.GripReach;
import net.magister.liberanimalium.body.grip.GripRules;
import net.magister.liberanimalium.body.grip.GripSpec;
import net.magister.liberanimalium.body.grip.GripState;
import net.magister.liberanimalium.body.grip.Held;
import net.magister.liberanimalium.body.locomotion.WaterEngine;
import net.magister.liberanimalium.body.part.FaunaPartEntity;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.body.stats.FaunaStatSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.config.WorldRules;
import net.magister.liberanimalium.interop.SubLevelGrip;
import net.magister.liberanimalium.interop.SubLevels;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.network.GripDeckPayload;
import net.magister.liberanimalium.network.GripStancePayload;
import net.magister.liberanimalium.network.SyncQtePacket;
import net.magister.liberanimalium.qte.QteInput;
import net.magister.liberanimalium.qte.QteOutcome;
import net.magister.liberanimalium.qte.games.ArrowSequenceQte;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.slf4j.Logger;

public class GripComponent implements IFaunaComponent {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String KEY_HELD = "FaunaGrip";
   public static final String KEY_PULL = "FaunaGripPull";
   private FaunaBase body;
   @Nullable
   private Entity held;
   @Nullable
   private UUID claim;
   private Vec3 pull = Vec3.ZERO;
   private final GripHang hang = new GripHang();
   private boolean keyDown;
   private boolean forced;
   private boolean heldByForce;
   private boolean benchHold;
   @Nullable
   private Vec3[] lastHips;
   private double speed;
   private final Quaternionf tickTurn = new Quaternionf();
   private final Quaternionf tickTurnO = new Quaternionf();
   private final DrawnHips drawnHips = new DrawnHips();
   @Nullable
   private Vec3 sentStood;
   private int sentStoodTick = Integer.MIN_VALUE;
   private Vec3 reportedStood = Vec3.ZERO;
   private long reportedStoodAt = Long.MIN_VALUE;
   @Nullable
   private ArrowSequenceQte escape;
   private final ArrayDeque<QteInput.Dir> struggle = new ArrayDeque<>();
   @Nullable
   private UUID escaped;
   private int escapedUntil;
   @Nullable
   private Entity reachTarget;
   private float weight;
   private float weightO;
   @Nullable
   private Vec3 lastBoxCentre;
   @Nullable
   private Quaternionf lastBoxTurn;
   private double lastBoxHalfWidth;
   private double lastBoxHeight;
   private double lastBandMid = Double.NaN;
   @Nullable
   private Object deck;
   @Nullable
   private AABB deckPart;
   @Nullable
   private SubLevelGrip.Joint joint;
   private final Quaterniond deckTurn = new Quaterniond();
   @Nullable
   private Vec3 lastAnchor;
   @Nullable
   private Vec3 lastMiddle;
   private double deckForce;
   @Nullable
   private Object reachDeck;
   @Nullable
   private AABB reachPart;
   private static final ThreadLocal<Object> PASSING = new ThreadLocal<>();

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   public GripSpec spec() {
      return BodyPlans.grip(this.body.animSpeciesId());
   }

   public GripState state() {
      return GripState.of((Byte)this.body.getEntityData().get(FaunaBase.DATA_GRIP_STATE));
   }

   public static GripState stateOf(FaunaBase body) {
      return GripState.of((Byte)body.getEntityData().get(FaunaBase.DATA_GRIP_STATE));
   }

   @Nullable
   public Entity held() {
      return this.held;
   }

   public boolean holding() {
      return this.held != null || this.deck != null;
   }

   public static void moveThrough(FaunaBase body, Runnable move) {
      GripComponent grip = body.grip();
      Object deck = grip == null ? null : grip.deck;
      if (deck == null) {
         move.run();
      } else {
         if (SubLevels.under(body) == deck) {
            SubLevels.untrack(body);
         }

         PASSING.set(deck);

         try {
            move.run();
         } finally {
            PASSING.remove();
         }
      }
   }

   @Nullable
   public static Object passingThrough() {
      return PASSING.get();
   }

   public static boolean keepsAloft(FaunaBase body) {
      return false;
   }

   public float load() {
      return (Float)this.body.getEntityData().get(FaunaBase.DATA_GRIP_LOAD);
   }

   public void riderKey(boolean down) {
      if (this.spec().grips()) {
         if (down && !this.keyDown) {
            if (this.held != null || this.deck != null) {
               this.release(this.body.onGround() ? GripComponent.Release.SET_DOWN : GripComponent.Release.DROP);
            } else if (this.allowed(this.spec())) {
               this.setState(GripState.REACHING);
            }
         } else if (!down && this.keyDown && this.state() == GripState.REACHING) {
            this.setState(GripState.NONE);
         }

         this.keyDown = down;
      }
   }

   public void afterMove() {
      GripSpec s = this.spec();
      if (s.grips()) {
         if (this.body.level().isClientSide()) {
            this.clientTick(s);
         } else {
            this.serverTick(s);
         }
      }
   }

   private void serverTick(GripSpec s) {
      double size = GripRules.size(this.body);
      Quaternionf attitude = this.tickAttitude();
      Vec3[] hips = GripReach.hips(s, this.body.position(), this.body.getBbHeight() * 0.5, attitude, size);
      if (this.deck != null && (SubLevelGrip.removed(this.deck) || this.joint == null || !this.joint.valid())) {
         this.release(GripComponent.Release.LET_GO);
      }

      if (this.held != null && !this.stillHeld(this.held)) {
         boolean unloaded = this.held.isRemoved() && this.held.getRemovalReason() != null && !this.held.getRemovalReason().shouldDestroy() && !(this.held instanceof Player);
         UUID kept = unloaded ? this.claim : null;
         this.release(GripComponent.Release.LET_GO);
         this.claim = kept;
      }

      if (this.held == null && this.claim != null && this.body.level() instanceof ServerLevel level) {
         Entity found = level.getEntity(this.claim);
         if (found != null) {
            double reach = s.longestReach() * size + found.getBbHeight();
            if (found.isAlive() && found.distanceTo(this.body) <= this.body.getBbHeight() + reach) {
               this.link(found, false);
            } else {
               this.claim = null;
            }
         }
      }

      GripState state = this.state();
      if (this.held == null && this.deck == null) {
         if (state == GripState.REACHING) {
            if (this.allowed(s) && (this.forced || this.body.steeredByRider())) {
               Vec3[] from = this.lastHips != null && this.lastHips.length == hips.length ? this.lastHips : hips;
               AABB search = GripReach.sweep(s, from, hips, size);
               List<Entity> candidates = this.body.level().getEntities(this.body, search, e -> this.fitsShape(e, s, size));
               Entity best = GripReach.choose(s, candidates, from, hips, size, this::fitsWeight);
               double bestScore = best == null ? Double.MAX_VALUE : GripReach.reach(s, best.getBoundingBox(), from, hips, size);
               GripComponent.DeckReach structure = this.reachDeck(s, attitude, search, from, hips, size);
               if (structure != null && structure.score() < bestScore) {
                  this.takeDeck(structure, s, attitude, size);
               } else if (best != null) {
                  this.take(best, s, attitude, size);
               }
            } else {
               this.setState(GripState.NONE);
            }
         } else if (state == GripState.HOLDING) {
            this.setState(GripState.NONE);
         }
      } else {
         if (this.allowed(s)) {
            this.benchHold = false;
         }

         boolean steered = this.body.steeredByRider() || this.benchHold || this.heldByForce;
         if ((this.benchHold || this.allowed(s)) && steered) {
            double step = this.physics().pull() * size;
            double left = this.pull.length();
            Vec3 next = left <= step ? Vec3.ZERO : this.pull.scale((left - step) / left);
            if (!next.equals(this.pull)) {
               this.pull = next;
               this.body
                  .getEntityData()
                  .set(FaunaBase.DATA_GRIP_PULL, new Vector3f((float)this.pull.x, (float)this.pull.y, (float)this.pull.z));
            }

            if (this.held != null) {
               this.place(s, attitude, size);
               if (this.held instanceof Player player) {
                  this.holdPlayer(player, s, size);
               }
            } else if (this.body.level() instanceof ServerLevel level) {
               this.holdDeck(level, s, attitude, size);
            }
         } else {
            this.release(this.body.onGround() ? GripComponent.Release.SET_DOWN : GripComponent.Release.DROP);
         }
      }

      this.lastHips = hips;
   }

   private void clientTick(GripSpec s) {
      int id = (Integer)this.body.getEntityData().get(FaunaBase.DATA_GRIP_HELD);
      if (this.held != null && (this.held.getId() != id || this.held.isRemoved())) {
         this.unmark(this.held);
         Grip.thrown(this.held, PhysicsSheet.of(this.body).grip().thrownDrag());
         this.held = null;
      }

      if (this.held == null && id >= 0) {
         Entity found = this.body.level().getEntity(id);
         if (found != null) {
            this.held = found;
            this.mark(found);
            Quaternionf attitude = this.tickAttitude();
            this.startHang(gripPoint(found, s), this.hips(s, attitude, GripRules.size(this.body)), attitude);
            Vector3f synced = (Vector3f)this.body.getEntityData().get(FaunaBase.DATA_GRIP_PULL);
            this.pull = new Vec3(synced.x, synced.y, synced.z);
         }
      }

      Vector3f synced = (Vector3f)this.body.getEntityData().get(FaunaBase.DATA_GRIP_PULL);
      this.pull = new Vec3(synced.x, synced.y, synced.z);
      double size = GripRules.size(this.body);
      Quaternionf attitude = this.tickAttitude();
      GripState state = this.state();
      this.reachTarget = null;
      this.reachDeck = null;
      this.reachPart = null;
      this.drawnHips.settle(this.body.tickCount);
      if (state == GripState.NONE) {
         this.deckPart = null;
      }

      this.deck = this.deckPart == null ? null : SubLevels.at(this.body.level(), this.deckPart.getCenter());
      if (this.held != null) {
         this.place(s, attitude, size);
      } else if (this.deck != null) {
         if (this.body.isControlledByLocalInstance()) {
            this.tether(s, this.rest(s, attitude, size).point(), size);
         }
      } else if (state == GripState.REACHING) {
         Vec3[] hips = GripReach.hips(s, this.body.position(), this.body.getBbHeight() * 0.5, attitude, size);
         AABB search = GripReach.sweep(s, hips, hips, size);
         List<Entity> candidates = this.body.level().getEntities(this.body, search, e -> this.fitsShape(e, s, size));
         this.reachTarget = GripReach.choose(s, candidates, hips, hips, size, this::fitsWeight);
         double bestScore = this.reachTarget == null ? Double.MAX_VALUE : GripReach.reach(s, this.reachTarget.getBoundingBox(), hips, hips, size);
         GripComponent.DeckReach structure = this.reachDeck(s, attitude, search, hips, hips, size);
         if (structure != null && structure.score() < bestScore) {
            this.reachTarget = null;
            this.reachDeck = structure.sub();
            this.reachPart = structure.part();
         }
      }

      this.weightO = this.weight;
      float target = this.held == null && this.reachTarget == null && this.deck == null && this.reachDeck == null ? 0.0F : 1.0F;
      this.weight = this.weight + Mth.clamp(target - this.weight, -0.2F, 0.2F);
   }

   private boolean stillHeld(Entity entity) {
      return !entity.isRemoved() && entity.isAlive() && entity.level() == this.body.level() && Grip.holderOf(entity) == this.body;
   }

   private boolean fitsShape(Entity entity, GripSpec s, double size) {
      if (entity == this.body || !entity.isAlive() || entity.isRemoved() || entity.isSpectator()) {
         return false;
      }

      if (entity.isPickable() && !(entity instanceof FaunaPartEntity)) {
         if (entity.getVehicle() == this.body || this.body.hasPassenger(entity) || entity.isVehicle()) {
            return false;
         } else if (Grip.held(entity)) {
            return false;
         } else if (entity instanceof FaunaBase other && other.grip() != null && other.grip().holding()) {
            return false;
         } else if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
         } else {
            return entity.getUUID().equals(this.escaped) && this.body.tickCount < this.escapedUntil ? false : entity.getBbWidth() <= s.span() * size;
         }
      } else {
         return false;
      }
   }

   private boolean fitsWeight(Entity entity) {
      if (GripRules.ungrabbable(entity)) {
         return false;
      }

      if (entity instanceof Player && !WorldRules.mayGrabPlayers()) {
         return false;
      }

      if (!this.body.level().isClientSide()) {
         if (entity.isInvulnerable()) {
            return false;
         }

         if (entity instanceof Player && (this.body.getServer() == null || !this.body.getServer().isPvpAllowed())) {
            return false;
         }
      }

      double capacity = GripRules.capacity(this.body);
      return capacity > 0.0 && GripRules.mass(entity) <= capacity;
   }

   private void take(Entity entity, GripSpec s, Quaternionfc attitude, double size) {
      if (entity instanceof FaunaBase pet && pet.perched()) {
         pet.leavePerch();
      }

      if (entity.isPassenger()) {
         entity.stopRiding();
      }

      Vec3 hold = GripGeometry.worldPoint(this.body.position(), this.body.getBbHeight() * 0.5, attitude, s.hold().scale(size));
      Vec3 offset = GripGeometry.toBody(attitude, gripPoint(entity, s).subtract(hold));
      double most = s.longestReach() * size;
      if (offset.length() > most) {
         offset = offset.scale(most / offset.length());
      }

      this.pull = offset;
      this.body.getEntityData().set(FaunaBase.DATA_GRIP_PULL, new Vector3f((float)this.pull.x, (float)this.pull.y, (float)this.pull.z));
      this.link(entity, true);
   }

   private void link(Entity entity, boolean fresh) {
      this.held = entity;
      this.claim = entity.getUUID();
      this.heldByForce = this.forced;
      this.forced = false;
      this.mark(entity);
      GripSpec s = this.spec();
      Quaternionf attitude = this.tickAttitude();
      this.startHang(gripPoint(entity, s), this.hips(s, attitude, GripRules.size(this.body)), attitude);
      this.body.getEntityData().set(FaunaBase.DATA_GRIP_HELD, entity.getId());
      double capacity = GripRules.capacity(this.body);
      this.setLoad(capacity > 0.0 ? (float)Math.min(1.0, GripRules.mass(entity) / capacity) : 0.0F);
      this.setState(GripState.HOLDING);
      if (entity instanceof ServerPlayer player) {
         this.beginEscape(player);
      }
   }

   public int escapeRow() {
      int heavier = GripRules.weightClass(this.body).ordinal();
      double row = (3 + 2 * heavier) * GripRules.size(this.body);
      return Math.max(3, (int)Math.round(row));
   }

   private void beginEscape(ServerPlayer player) {
      this.escape = new ArrowSequenceQte(this.escapeRow());
      this.escape.start(this.body.getRandom());
      this.struggle.clear();
      FaunaNetwork.sendToPlayer(new SyncQtePacket(this.body.getId(), true, this.escape.renderState()), player);
   }

   private void endEscape(@Nullable Entity entity) {
      if (this.escape != null) {
         this.escape = null;
         this.struggle.clear();
         if (entity instanceof ServerPlayer player) {
            FaunaNetwork.sendToPlayer(new SyncQtePacket(this.body.getId(), false, null), player);
         }
      }
   }

   public void struggle(Player player, QteInput.Dir dir) {
      if (this.held == player && this.escape != null && this.struggle.size() < 8) {
         this.struggle.add(dir);
      }
   }

   private void holdPlayer(Player player, GripSpec s, double size) {
      Held.Pin pin = ((Held)player).liberanimalium$pin();
      double slack = s.longestReach() * size + 2.0 + this.hang.carrier().length() * 6.0;
      if (pin.placed && player.position().distanceTo(new Vec3(pin.x, pin.y, pin.z)) > slack) {
         this.release(GripComponent.Release.LET_GO);
      } else if (this.escape != null && player instanceof ServerPlayer served) {
         boolean changed = !this.struggle.isEmpty();
         QteOutcome outcome = QteOutcome.RUNNING;

         while (!this.struggle.isEmpty() && outcome == QteOutcome.RUNNING) {
            outcome = this.escape.tick(ArrowSequenceQte.press(this.struggle.poll()));
         }

         if (outcome == QteOutcome.RUNNING) {
            outcome = this.escape.tick(QteInput.NONE);
         }

         if (outcome == QteOutcome.WON) {
            this.escaped = player.getUUID();
            this.escapedUntil = this.body.tickCount + 60;
            this.release(this.body.onGround() ? GripComponent.Release.SET_DOWN : GripComponent.Release.DROP);
         } else {
            if (changed || this.escape.renderState().get(2) < 20.0F) {
               FaunaNetwork.sendToPlayer(new SyncQtePacket(this.body.getId(), true, this.escape.renderState()), served);
            }
         }
      }
   }

   private void place(GripSpec s, Quaternionfc attitude, double size) {
      Entity entity = this.held;
      if (entity != null) {
         GripComponent.Rest r = this.rest(s, attitude, size);
         PhysicsSheet.Grip physics = this.physics();
         Quaternionf swept = r.swept();
         Vec3 hips = r.hips();
         Vec3 carry = r.carry();
         Vec3 rest = r.point();
         Vector3f down = attitude.transform(
            carry.lengthSqr() > 1.0E-8
               ? new Vector3f((float)carry.x, (float)carry.y, (float)(-carry.z)).normalize()
               : new Vector3f(0.0F, -1.0F, 0.0F)
         );
         this.hang.step(hips, rest, swept, down, attitude.transform(new Vector3f(1.0F, 0.0F, 0.0F)), physics, s);
         this.tickTurnO.set(this.tickTurn);
         this.tickTurn.set(attitude);
         Vec3 grip = this.hang.pivot(1.0F);
         if (this.drawnHips.seen()) {
            grip = grip.add(GripGeometry.worldDirection(attitude, this.drawnHips.answered()));
         }

         GripGeometry.Placement p = outOfTheGround(entity, grip, this.hang.turn(1.0F), s);
         Held.Pin pin = ((Held)entity).liberanimalium$pin();
         Vec3 at = p.boxOrigin();
         if (pin.placed) {
            pin.xO = pin.x;
            pin.yO = pin.y;
            pin.zO = pin.z;
            pin.yawO = pin.yaw;
         } else {
            pin.xO = at.x;
            pin.yO = at.y;
            pin.zO = at.z;
            pin.yawO = this.body.yBodyRot;
         }

         pin.x = at.x;
         pin.y = at.y;
         pin.z = at.z;
         pin.yaw = this.body.yBodyRot;
         Vec3 velocity = this.hang.velocity();
         pin.vx = velocity.x;
         pin.vy = velocity.y;
         pin.vz = velocity.z;
         pin.placed = true;
         Grip.pin(entity);
      }
   }

   private GripComponent.Rest rest(GripSpec s, Quaternionfc attitude, double size) {
      this.speed = this.speed + (this.speedShare() - this.speed) * 0.25;
      PhysicsSheet.Grip physics = this.physics();
      Quaternionf swept = GripGeometry.sweptBack(attitude, GripGeometry.back(attitude, this.speed, physics, s.back()), new Quaternionf());
      Vec3 hips = this.hips(s, attitude, size);
      Vec3 carry = s.hold().subtract(s.middle()).scale(size).add(this.pull);
      Vec3 point = hips.add(GripGeometry.worldDirection(swept, carry).scale(1.0 - physics.tuck() * tilted(attitude)));
      return new GripComponent.Rest(hips, swept, carry, point);
   }

   @Nullable
   private GripComponent.DeckReach reachDeck(GripSpec s, Quaternionfc attitude, AABB search, Vec3[] from, Vec3[] to, double size) {
      return null;
   }

   private Vec3 heldPoint(AABB part) {
      return new Vec3((part.minX + part.maxX) * 0.5, part.minY + part.getYsize() * this.spec().bandMid(), (part.minZ + part.maxZ) * 0.5);
   }

   private void takeDeck(GripComponent.DeckReach found, GripSpec s, Quaternionf attitude, double size) {
      if (this.body.level() instanceof ServerLevel level) {
         Vec3 middle = SubLevelGrip.toWorld(found.sub(), this.heldPoint(found.part()), 1.0F);
         if (middle != null) {
            Vec3 hold = GripGeometry.worldPoint(this.body.position(), this.body.getBbHeight() * 0.5, attitude, s.hold().scale(size));
            Vec3 offset = GripGeometry.toBody(attitude, middle.subtract(hold));
            double most = s.longestReach() * size;
            if (offset.length() > most) {
               offset = offset.scale(most / offset.length());
            }

            this.pull = offset;
            Quaterniond heading = this.heading();
            Quaterniond turn = SubLevelGrip.turn(found.sub(), 1.0F);
            this.deckTurn.set(heading).conjugate().mul(turn);
            Vec3 anchor = this.rest(s, attitude, size).point();
            SubLevelGrip.Joint made = SubLevelGrip.join(level, found.sub(), anchor, turn, this.heldPoint(found.part()), new Quaterniond());
            if (made != null) {
               double lift = this.liftForce(level);
               made.drive(400.0, 28.0, lift * 1.5, 100.0, 14.0, lift * 1.0);
               this.body
                  .getEntityData()
                  .set(FaunaBase.DATA_GRIP_PULL, new Vector3f((float)this.pull.x, (float)this.pull.y, (float)this.pull.z));
               this.joint = made;
               this.deck = found.sub();
               this.deckPart = found.part();
               this.lastAnchor = null;
               this.lastMiddle = null;
               this.deckForce = 0.0;
               this.heldByForce = this.forced;
               this.forced = false;
               this.setState(GripState.HOLDING);
               this.tellDeck(this.deckPart);
               LOGGER.debug("[Grip] {} takes a structure of mass {} at {}", new Object[]{this.body.getName().getString(), SubLevelGrip.mass(this.deck), middle});
            }
         }
      }
   }

   private void holdDeck(ServerLevel level, GripSpec s, Quaternionf attitude, double size) {
      if (this.joint != null && this.deck != null && this.deckPart != null) {
         Vec3 anchor = this.rest(s, attitude, size).point();
         Vec3 moved = this.lastAnchor == null ? Vec3.ZERO : anchor.subtract(this.lastAnchor);
         this.lastAnchor = anchor;
         Vec3 lead = moved.scale(1.4);
         Vec3 target = anchor.add(lead);
         this.joint.frame(target, this.heading().mul(this.deckTurn));
         SubLevelGrip.wake(level, this.deck);
         double lift = this.liftForce(level);
         Vec3 middle = SubLevelGrip.toWorld(this.deck, this.heldPoint(this.deckPart), 1.0F);
         if (middle != null) {
            Vec3 speed = this.lastMiddle == null ? Vec3.ZERO : middle.subtract(this.lastMiddle).scale(20.0);
            Vec3 pulled = target.subtract(middle).scale(400.0).subtract(speed.scale(28.0));
            double force = Math.min(lift * 1.5, SubLevelGrip.mass(this.deck) * pulled.length());
            this.deckForce = this.deckForce + (force - this.deckForce) * 0.2;
         }

         this.lastMiddle = middle;
         double load = lift > 0.0 ? Math.min(1.0, this.deckForce / lift) : 1.0;
         this.setLoad((float)Math.round(load / 0.05F) * 0.05F);
         if (this.body.tickCount % 20 == 0) {
            this.tellDeck(this.deckPart);
         }

         if (this.body.isControlledByLocalInstance()) {
            this.tether(s, anchor, size);
         }
      }
   }

   private void tether(GripSpec s, Vec3 anchor, double size) {
      Vec3 middle = this.deck != null && this.deckPart != null ? SubLevelGrip.toWorld(this.deck, this.heldPoint(this.deckPart), 1.0F) : null;
      if (middle != null) {
         Vec3 gap = middle.subtract(anchor);
         double apart = gap.length();
         double slack = s.longestReach() * size + 1.0;
         if (!(apart <= slack)) {
            if (apart > slack + 6.0) {
               if (!this.body.level().isClientSide()) {
                  this.release(GripComponent.Release.LET_GO);
               }
            } else {
               Vec3 toward = gap.scale(1.0 / apart);
               this.body.setPos(this.body.position().add(toward.scale(apart - slack)));
               Vec3 velocity = this.body.getDeltaMovement();
               double away = velocity.dot(toward);
               if (away < 0.0) {
                  this.body.setDeltaMovement(velocity.subtract(toward.scale(away)));
               }
            }
         }
      }
   }

   private double liftForce(ServerLevel level) {
      return GripRules.capacity(this.body) / GripRules.structureMass(1.0) * SubLevelGrip.gravity(level);
   }

   private Quaterniond heading() {
      return new Quaterniond(GripGeometry.yawOnly(this.body.yBodyRot, new Quaternionf()));
   }

   private void tellDeck(@Nullable AABB part) {
      FaunaNetwork.sendToClientsTracking(new GripDeckPayload(this.body.getId(), part), this.body);
   }

   public void acceptDeck(@Nullable AABB part) {
      this.deckPart = part;
      if (part == null) {
         this.deck = null;
      }
   }

   @Nullable
   private GripGeometry.Box deckBox(Object sub, AABB part, float partial) {
      Vec3 centre = SubLevelGrip.toWorld(sub, part.getCenter(), partial);
      if (centre == null) {
         return null;
      }

      Quaterniond turn = SubLevelGrip.turn(sub, partial);
      float yaw = Mth.rotLerp(partial, this.body.yBodyRotO, this.body.yBodyRot);
      Vector3f right = GripGeometry.yawOnly(yaw, new Quaternionf()).transform(new Vector3f(1.0F, 0.0F, 0.0F));
      Quaternionf best = null;
      int bestQuarter = 0;
      float bestDot = -2.0F;

      for (int quarter = 0; quarter < 4; quarter++) {
         Quaternionf q = new Quaternionf(turn).rotateY(quarter * (float) (Math.PI / 2));
         float dot = q.transform(new Vector3f(1.0F, 0.0F, 0.0F)).dot(right);
         if (dot > bestDot) {
            bestDot = dot;
            best = q;
            bestQuarter = quarter;
         }
      }

      double scale = SubLevelGrip.scale(sub);
      double across = bestQuarter % 2 == 0 ? part.getXsize() : part.getZsize();
      return new GripGeometry.Box(centre, best, across * 0.5 * scale, part.getYsize() * scale);
   }

   private static Vec3 gripPoint(Entity entity, GripSpec s) {
      return entity.getBoundingBox().getCenter().add(0.0, (s.bandMid(entity) - 0.5) * entity.getBbHeight(), 0.0);
   }

   private void startHang(Vec3 grip, Vec3 hips, Quaternionfc attitude) {
      this.hang.start(grip, hips);
      this.tickTurn.set(attitude);
      this.tickTurnO.set(attitude);
   }

   private Vec3 hips(GripSpec s, Quaternionfc attitude, double size) {
      Vec3 stood = this.drawnHips.seen() ? this.drawnHips.stood(1.0F) : this.reportedStood();
      return GripGeometry.worldPoint(this.body.position(), this.body.getBbHeight() * 0.5, attitude, s.middle().scale(size).add(stood));
   }

   private static GripGeometry.Placement outOfTheGround(Entity entity, Vec3 pivot, Quaternionf turn, GripSpec s) {
      GripGeometry.Placement p = GripGeometry.hang(pivot, turn, entity.getBbHeight(), s.bandMid(entity));
      AABB box = entity.getDimensions(entity.getPose()).makeBoundingBox(p.boxOrigin());
      double top = box.minY;

      for (VoxelShape shape : entity.level().getBlockCollisions(entity, box)) {
         if (!shape.isEmpty()) {
            top = Math.max(top, shape.max(Axis.Y));
         }
      }

      double lift = top - box.minY;
      return !(lift <= 1.0E-4) && !(lift > box.getYsize() * 0.5)
         ? GripGeometry.hang(pivot.add(0.0, lift, 0.0), turn, entity.getBbHeight(), s.bandMid(entity))
         : p;
   }

   private Vec3 reportedStood() {
      return this.body.level().getGameTime() - this.reportedStoodAt <= 40L ? this.reportedStood : Vec3.ZERO;
   }

   public void reportStance(int localId) {
      if (this.body.partReporter() == localId && this.drawnHips.seen()) {
         if (this.body.tickCount - this.sentStoodTick >= 5) {
            Vec3 stood = this.drawnHips.stood(1.0F);
            if (this.sentStood == null || !(stood.distanceTo(this.sentStood) <= 0.05)) {
               this.sentStood = stood;
               this.sentStoodTick = this.body.tickCount;
               FaunaNetwork.sendToServer(new GripStancePayload(this.body.getId(), stood));
            }
         }
      }
   }

   public void acceptStance(int senderId, Vec3 offset) {
      if (senderId == this.body.partReporter()) {
         if (Double.isFinite(offset.x) && Double.isFinite(offset.y) && Double.isFinite(offset.z)) {
            if (!(offset.length() > this.spec().longestReach() * GripRules.size(this.body))) {
               this.reportedStood = offset;
               this.reportedStoodAt = this.body.level().getGameTime();
            }
         }
      }
   }

   private PhysicsSheet.Grip physics() {
      return PhysicsSheet.of(this.body).grip().laden(this.load());
   }

   private boolean allowed(GripSpec s) {
      GripSpec.Medium medium = this.body.isFlying() ? GripSpec.Medium.AIR : (this.body.isAquatic() ? GripSpec.Medium.WATER : GripSpec.Medium.GROUND);
      return s.media().contains(medium);
   }

   private double speedShare() {
      double top;
      if (this.body.isAquatic()) {
         FaunaStatSheet stats = this.body.getStatSheet();
         top = stats == null ? 0.0 : WaterEngine.topSpeed(this.body, stats);
      } else {
         AttributeInstance flying = this.body.getAttribute(Attributes.FLYING_SPEED);
         top = flying == null ? 0.0 : flying.getValue();
      }

      return !(top > 0.001) ? 0.0 : this.body.position().subtract(this.body.xo, this.body.yo, this.body.zo).length() / top;
   }

   public void noteDrawnHips(double time, Vec3 offset, Vec3 at, Quaternionfc turn, Quaternionfc torso) {
      this.drawnHips.note(time, this.body.tickCount, offset, at, turn, torso);
   }

   @Nullable
   public Quaternionf stoodTorso(float partial) {
      return this.drawnHips.seen() ? this.drawnHips.stoodTorso(partial) : null;
   }

   private static double tilted(Quaternionfc attitude) {
      double up = attitude.transform(new Vector3f(0.0F, 1.0F, 0.0F)).y;
      return Mth.clamp((1.0 - up) / 0.5, 0.0, 1.0);
   }

   private Quaternionf tickAttitude() {
      FaunaFlightRotationComponent rotation = this.body.componentRegistry.get(FaunaFlightRotationComponent.class);
      if (rotation == null || !this.body.inThreeDMedium()) {
         return GripGeometry.yawOnly(this.body.yBodyRot, new Quaternionf());
      } else {
         return this.body.level().isClientSide() && rotation.isRolling()
            ? rotation.getRenderOrientation(1.0F)
            : GripGeometry.attitude(this.body.yBodyRot, this.body.getXRot(), rotation.getRoll(), new Quaternionf());
      }
   }

   public void release(GripComponent.Release how) {
      if (this.deck != null || this.joint != null) {
         if (this.joint != null) {
            this.joint.remove();
         }

         this.joint = null;
         this.deck = null;
         this.deckPart = null;
         this.lastAnchor = null;
         this.tellDeck(null);
      }

      Entity entity = this.held;
      this.endEscape(entity);
      this.held = null;
      this.claim = null;
      this.forced = false;
      this.heldByForce = false;
      this.benchHold = false;
      this.pull = Vec3.ZERO;
      this.body.getEntityData().set(FaunaBase.DATA_GRIP_PULL, new Vector3f());
      this.body.getEntityData().set(FaunaBase.DATA_GRIP_HELD, -1);
      this.setLoad(0.0F);
      this.setState(GripState.NONE);
      if (entity != null) {
         this.unmark(entity);
         entity.fallDistance = 0.0F;
         if (how == GripComponent.Release.DROP) {
            Vec3 swing = this.hang.velocity().subtract(this.hang.carrier());
            Vec3 thrown = this.hang.velocity().add(swing.scale(PhysicsSheet.of(this.body).grip().fling() - 1.0));
            entity.setDeltaMovement(thrown);
            entity.hurtMarked = true;
            Grip.thrown(entity, PhysicsSheet.of(this.body).grip().thrownDrag());
            LOGGER.debug(
               "[Grip] {} lets go of {}: thrown at {}, the grippers moving at {}",
               new Object[]{this.body.getName().getString(), entity.getName().getString(), thrown, this.hang.carrier()}
            );
         } else if (how == GripComponent.Release.SET_DOWN) {
            entity.setDeltaMovement(Vec3.ZERO);
            entity.teleportTo(entity.getX(), Math.max(entity.getY(), this.body.getY()), entity.getZ());
            entity.hurtMarked = true;
         }
      }
   }

   public void bodyRemoved(boolean destroyed) {
      if (this.body.level().isClientSide()) {
         if (this.held != null) {
            this.unmark(this.held);
         }

         this.held = null;
         this.deck = null;
         this.deckPart = null;
      } else {
         if (this.deck != null) {
            this.release(GripComponent.Release.LET_GO);
         }

         if (this.held != null) {
            if (destroyed) {
               this.release(GripComponent.Release.DROP);
            } else {
               UUID kept = this.claim;
               this.endEscape(this.held);
               this.unmark(this.held);
               this.held = null;
               this.claim = kept;
            }
         }
      }
   }

   private void mark(Entity entity) {
      Held h = (Held)entity;
      h.liberanimalium$pin().placed = false;
      h.liberanimalium$setHolder(this.body);
      entity.noPhysics = true;
   }

   private void unmark(Entity entity) {
      Held h = (Held)entity;
      if (h.liberanimalium$holder() == this.body) {
         h.liberanimalium$setHolder(null);
      }

      h.liberanimalium$pin().placed = false;
      if (!entity.isSpectator()) {
         entity.noPhysics = false;
      }

      entity.setOnGround(false);
   }

   private void setState(GripState state) {
      this.body.getEntityData().set(FaunaBase.DATA_GRIP_STATE, (byte)state.ordinal());
   }

   private void setLoad(float load) {
      if (this.load() != load) {
         this.body.getEntityData().set(FaunaBase.DATA_GRIP_LOAD, load);
         this.body.recalculateStats();
      }
   }

   public String force(boolean anyway) {
      GripSpec s = this.spec();
      if (!s.grips()) {
         return "has no grip block";
      }

      if (this.held != null) {
         return "already holds " + this.held.getName().getString();
      }

      if (this.deck != null) {
         return "already holds a structure";
      }

      if (!anyway) {
         this.forced = true;
         this.setState(GripState.REACHING);
         return "reaches: the next creature in reach is taken";
      }

      double size = GripRules.size(this.body);
      Entity best = null;
      double nearest = Double.MAX_VALUE;

      for (Entity e : this.body.level().getEntities(this.body, this.body.getBoundingBox().inflate(8.0), ex -> this.fitsShape(ex, s, size) && this.fitsWeight(ex))) {
         double d = e.distanceToSqr(this.body);
         if (d < nearest) {
            nearest = d;
            best = e;
         }
      }

      if (best == null) {
         StringBuilder why = new StringBuilder(
            String.format(Locale.ROOT, "finds nothing it can lift within %d blocks (size %.2f, lifts %.0f)", 8, size, GripRules.capacity(this.body))
         );

         for (Entity e : this.body.level().getEntities(this.body, this.body.getBoundingBox().inflate(8.0), ex -> true)) {
            String refused = !this.fitsShape(e, s, size) ? "shape" : (!this.fitsWeight(e) ? String.format(Locale.ROOT, "weight %.0f", GripRules.mass(e)) : "?");
            why.append("; ").append(e.getName().getString()).append(": ").append(refused);
         }

         return why.toString();
      } else {
         this.take(best, s, this.tickAttitude(), size);
         this.benchHold = !this.allowed(s);
         return "takes " + best.getName().getString() + String.format(Locale.ROOT, " (load %.2f)", this.load());
      }
   }

   public float shownWeight(float partial) {
      return Mth.lerp(partial, this.weightO, this.weight);
   }

   public boolean closing() {
      return this.held != null || this.deck != null;
   }

   @Nullable
   public GripGeometry.Placement drawn(float partial) {
      Entity entity = this.held;
      if (entity != null && this.hang.started()) {
         GripSpec s = this.spec();
         Vec3 pivot = this.hang.pivot(partial);
         Quaternionf turn = this.hang.turn(partial);
         if (this.body.level().isClientSide() && this.drawnHips.seen()) {
            double time = this.body.tickCount + partial;
            Quaternionf frame = this.drawnHips.turn();
            Quaternionf lead = new Quaternionf(frame).mul(new Quaternionf(this.tickTurnO).slerp(this.tickTurn, partial).conjugate());
            Vec3 stood = this.drawnHips.stood(partial);
            Vec3 stance = GripGeometry.worldPoint(
               this.drawnHips.at(time, this.body.getPosition(partial)),
               this.body.getBbHeight() * 0.5,
               frame,
               s.middle().scale(GripRules.size(this.body)).add(stood)
            );
            Vec3 hung = pivot.subtract(this.hang.hips(partial));
            Vector3f swung = lead.transform(new Vector3f((float)hung.x, (float)hung.y, (float)hung.z));
            PhysicsSheet.Grip physics = this.physics();
            Vec3 beat = GripGeometry.worldDirection(
               frame, this.drawnHips.answer(time, this.drawnHips.hips(time).subtract(stood), physics.beatRate(), physics.beatDamping())
            );
            pivot = stance.add(swung.x, swung.y, swung.z).add(beat);
            turn = lead.mul(turn);
         }

         return outOfTheGround(entity, pivot, turn, s);
      } else {
         return null;
      }
   }

   public double bandMid() {
      if (this.deck != null || this.reachDeck != null) {
         return this.spec().bandMid();
      } else if (this.held != null) {
         return this.spec().bandMid(this.held);
      } else if (this.reachTarget != null) {
         return this.spec().bandMid(this.reachTarget);
      } else {
         return Double.isNaN(this.lastBandMid) ? this.spec().bandMid() : this.lastBandMid;
      }
   }

   @Nullable
   public GripGeometry.Box box(float partial, Quaternionfc attitude) {
      Vec3 drawnAt = this.body.getPosition(partial);
      Entity entity = this.held;
      if (entity != null) {
         GripGeometry.Placement p = this.drawn(partial);
         if (p == null) {
            return null;
         }

         Quaternionf rotation = new Quaternionf(p.rotation());
         Quaternionf inverse = new Quaternionf(attitude).conjugate();
         Vector3f local = inverse.transform(
            new Vector3f(
               (float)(p.centre().x - drawnAt.x),
               (float)(p.centre().y - drawnAt.y),
               (float)(p.centre().z - drawnAt.z)
            )
         );
         this.lastBoxCentre = new Vec3(local.x, local.y, local.z);
         this.lastBoxTurn = new Quaternionf(inverse).mul(rotation);
         this.lastBoxHalfWidth = entity.getBbWidth() * 0.5;
         this.lastBoxHeight = entity.getBbHeight();
         this.lastBandMid = this.spec().bandMid(entity);
         return new GripGeometry.Box(p.centre(), rotation, this.lastBoxHalfWidth, this.lastBoxHeight);
      } else if (this.deck != null && this.deckPart != null) {
         GripGeometry.Box b = this.deckBox(this.deck, this.deckPart, partial);
         if (b == null) {
            return null;
         }

         Quaternionf inverse = new Quaternionf(attitude).conjugate();
         Vector3f local = inverse.transform(
            new Vector3f(
               (float)(b.centre().x - drawnAt.x),
               (float)(b.centre().y - drawnAt.y),
               (float)(b.centre().z - drawnAt.z)
            )
         );
         this.lastBoxCentre = new Vec3(local.x, local.y, local.z);
         this.lastBoxTurn = new Quaternionf(inverse).mul(b.rotation());
         this.lastBoxHalfWidth = b.halfWidth();
         this.lastBoxHeight = b.height();
         this.lastBandMid = this.spec().bandMid();
         return b;
      } else if (this.reachDeck != null && this.reachPart != null) {
         return this.deckBox(this.reachDeck, this.reachPart, partial);
      } else {
         Entity target = this.reachTarget;
         if (target != null) {
            Vec3 at = target.getPosition(partial);
            float yaw = Mth.rotLerp(partial, this.body.yBodyRotO, this.body.yBodyRot);
            return new GripGeometry.Box(
               at.add(0.0, target.getBbHeight() * 0.5, 0.0), GripGeometry.yawOnly(yaw, new Quaternionf()), target.getBbWidth() * 0.5, target.getBbHeight()
            );
         } else if (this.lastBoxCentre != null && this.weight > 0.0F) {
            Vector3f world = new Quaternionf(attitude)
               .transform(new Vector3f((float)this.lastBoxCentre.x, (float)this.lastBoxCentre.y, (float)this.lastBoxCentre.z));
            return new GripGeometry.Box(
               drawnAt.add(world.x, world.y, world.z), new Quaternionf(attitude).mul(this.lastBoxTurn), this.lastBoxHalfWidth, this.lastBoxHeight
            );
         } else {
            return null;
         }
      }
   }

   @Override
   public void save(CompoundTag tag) {
      UUID who = this.held != null ? this.held.getUUID() : this.claim;
      if (who != null) {
         tag.putUUID("FaunaGrip", who);
         tag.putDouble("FaunaGripPullX", this.pull.x);
         tag.putDouble("FaunaGripPullY", this.pull.y);
         tag.putDouble("FaunaGripPullZ", this.pull.z);
      }
   }

   @Override
   public void load(CompoundTag tag) {
      this.claim = tag.hasUUID("FaunaGrip") ? tag.getUUID("FaunaGrip") : null;
      this.pull = this.claim == null
         ? Vec3.ZERO
         : new Vec3(tag.getDouble("FaunaGripPullX"), tag.getDouble("FaunaGripPullY"), tag.getDouble("FaunaGripPullZ"));
   }

   private record DeckReach(Object sub, double score, AABB part) {
   }

   public enum Release {
      DROP,
      SET_DOWN,
      LET_GO;
   }

   private record Rest(Vec3 hips, Quaternionf swept, Vec3 carry, Vec3 point) {
   }
}

package net.magister.liberanimalium.fauna.know;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.Check;
import net.magister.liberanimalium.fauna.api.Kind;
import net.magister.liberanimalium.fauna.api.Relation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Representation {
   private final UUID id;
   private final String typeKey;
   private Relation relation = Relation.NONE;
   private final Relation staticRelation;
   private Vec3 lastKnownPos;
   private Vec3 lastVelocity = Vec3.ZERO;
   private boolean visible;
   private float alarmSpike;
   private float alarmEnvelope;
   private long lastFixTick;
   private long lastSeenTick;
   private final long firstKnownTick;
   private float findScale = 1.0F;
   private float salience;
   private float lastSightScore;
   private float pressure;
   private float manner = Manner.NEUTRAL;
   private float crowding;
   private float offering;
   private float provided;
   private float loom;
   private float armament;
   private float defending;
   @Nullable
   private Check asked;
   private float armorFraction;
   private boolean sneakingSeen;
   private boolean playfulSeen;
   private boolean answersSeen = true;
   private boolean pressingSeen;
   private boolean afloat;
   private float width;
   private float height;
   private boolean ownedSeen;
   @Nullable
   private UUID leashHolder;
   private boolean tethered;
   @Nullable
   private UUID providedBy;
   private List<Kind> leads = List.of();
   private float apparentSize;
   private long unreachableSince = Long.MIN_VALUE;
   private final boolean sameSpecies;
   @Nullable
   private Relation quietRaw;
   @Nullable
   private Relation quietWorn;
   private long lastScanSerial = Long.MIN_VALUE;
   private long lastLookTick = Long.MIN_VALUE;
   private long lastStartledTick = Long.MIN_VALUE;
   private float presenceSpike;
   private float presenceEnvelope;
   private long challengedTick = Long.MIN_VALUE;
   private int challengeWindow;
   private float vouched;
   private long lastHurtTick = Long.MIN_VALUE;
   private long lastFedTick = Long.MIN_VALUE;
   private long lastSearchCutTick = Long.MIN_VALUE;

   Representation(UUID id, String typeKey, boolean sameSpecies, Relation relation, Vec3 position, long now) {
      this(id, typeKey, sameSpecies, relation, relation, position, now);
   }

   Representation(UUID id, String typeKey, boolean sameSpecies, Relation relation, Relation staticRelation, Vec3 position, long now) {
      this.id = id;
      this.typeKey = typeKey;
      this.sameSpecies = sameSpecies;
      this.relation = relation;
      this.staticRelation = staticRelation;
      this.lastKnownPos = position;
      this.firstKnownTick = now;
      this.lastFixTick = now;
      this.lastSeenTick = now;
   }

   public boolean sameSpecies() {
      return this.sameSpecies;
   }

   public UUID id() {
      return this.id;
   }

   public String typeKey() {
      return this.typeKey;
   }

   public boolean isPlayer() {
      return "minecraft:player".equals(this.typeKey);
   }

   public Relation relation() {
      return this.relation;
   }

   public Relation staticRelation() {
      return this.staticRelation;
   }

   public boolean bent() {
      return !this.relation.equals(this.staticRelation) && (this.quietWorn == null || !this.relation.equals(this.quietWorn));
   }

   private void unquiet() {
      if (this.quietWorn != null && this.relation.equals(this.quietWorn)) {
         this.relation = this.quietRaw;
      }

      this.quietRaw = null;
      this.quietWorn = null;
   }

   void quiet(Relation worn) {
      if (this.relation.equals(this.staticRelation) && !worn.equals(this.relation)) {
         this.quietRaw = this.relation;
         this.quietWorn = worn;
         this.relation = worn;
      }
   }

   void bendTo(Relation value) {
      if (value.type() != this.relation.type()) {
         this.alarmSpike = 0.0F;
         this.alarmEnvelope = 0.0F;
      }

      this.relation = value;
   }

   public Vec3 lastKnownPos() {
      return this.lastKnownPos;
   }

   public Vec3 lastVelocity() {
      return this.lastVelocity;
   }

   public boolean visible() {
      return this.visible;
   }

   public long lastSeenTick() {
      return this.lastSeenTick;
   }

   public long lastFixTick() {
      return this.lastFixTick;
   }

   public float salience() {
      return this.salience;
   }

   public float lastSightScore() {
      return this.lastSightScore;
   }

   public boolean startledWithin(long now, int window) {
      return this.lastStartledTick != Long.MIN_VALUE && now - this.lastStartledTick <= window;
   }

   public float apparentSize() {
      return this.apparentSize;
   }

   public long age(long now) {
      return Math.max(0L, now - this.lastFixTick);
   }

   public long sinceLook(long now) {
      return this.lastLookTick == Long.MIN_VALUE ? 0L : Math.max(0L, now - this.lastLookTick);
   }

   public float find(long now) {
      return this.visible ? 1.0F : Mth.clamp(this.findScale * curve(this.age(now)), 0.0F, 1.0F);
   }

   public float alarm() {
      return Math.max(this.alarmSpike, this.alarmEnvelope);
   }

   public float presence() {
      return Math.max(this.presenceSpike, this.presenceEnvelope);
   }

   void updatePresence(float live, long elapsed, double retentionPerTick) {
      this.presenceEnvelope = Alarm.envelope(this.presenceEnvelope, live, elapsed, retentionPerTick);
      this.presenceSpike = live;
   }

   public float attribution() {
      return this.pressure;
   }

   public float defending() {
      return this.defending;
   }

   @Nullable
   public Check asked() {
      return this.asked;
   }

   void ask(@Nullable Check check) {
      this.asked = check;
   }

   void sawDefend() {
      this.defending = 1.0F;
   }

   void coolDefending(long elapsed, double retentionPerTick) {
      if (!(this.defending <= 0.0F) && elapsed > 0L) {
         this.defending = (float)(this.defending * Math.pow(retentionPerTick, elapsed));
      }
   }

   public float manner() {
      return this.manner;
   }

   void updateManner(float nextPressure, float nextManner) {
      this.pressure = nextPressure;
      this.manner = nextManner;
   }

   void roaredAt(long now, float strength) {
      float heard = Mth.clamp(strength, 0.0F, 1.0F);
      this.challengedTick = now;
      this.challengeWindow = Math.round(80.0F * heard);
      float spiked = Manner.spiked(this.attribution(), 0.6666667F * heard);
      this.updateManner(spiked, Manner.of(spiked, this.armament(), this.armorFraction(), this.sneakingSeen()));
   }

   public boolean challengedWithin(long now) {
      return this.challengedTick != Long.MIN_VALUE && now - this.challengedTick <= this.challengeWindow;
   }

   public float crowding() {
      return this.crowding;
   }

   void updateCrowding(float next) {
      this.crowding = next;
   }

   void updateOffering(float shade) {
      this.offering = shade;
   }

   void notePersonVouchedFor(float vouch) {
      this.vouched = vouch;
   }

   public float vouched() {
      return this.vouched;
   }

   void updateProvision(float provided, float loom) {
      this.provided = provided;
      this.loom = loom;
   }

   public float offering() {
      return this.offering;
   }

   public float provided() {
      return this.provided;
   }

   public float loom() {
      return this.loom;
   }

   public float armament() {
      return this.armament;
   }

   public float armorFraction() {
      return this.armorFraction;
   }

   public boolean owned() {
      return this.ownedSeen;
   }

   public boolean sneakingSeen() {
      return this.sneakingSeen;
   }

   public boolean playful() {
      return this.visible() && this.playfulSeen;
   }

   public boolean answers() {
      return this.answersSeen;
   }

   public boolean pressing() {
      return this.pressingSeen;
   }

   public boolean afloat() {
      return this.afloat;
   }

   public float width() {
      return this.width;
   }

   public float height() {
      return this.height;
   }

   @Nullable
   public UUID leashHolder() {
      return this.leashHolder;
   }

   public boolean tethered() {
      return this.tethered;
   }

   @Nullable
   public UUID providedBy() {
      return this.providedBy;
   }

   public List<Kind> leads() {
      return this.leads;
   }

   public float alarmSpike() {
      return this.alarmSpike;
   }

   public float alarmEnvelope() {
      return this.alarmEnvelope;
   }

   void updateAlarm(float spike, long elapsed, double retentionPerTick) {
      this.alarmEnvelope = Alarm.envelope(this.alarmEnvelope, spike, elapsed, retentionPerTick);
      this.alarmSpike = spike;
   }

   public static float curve(long age) {
      if (age <= 0L) {
         return 1.0F;
      }

      float fast = (float)Math.exp((float)(-age) / 40.0F);
      if (age <= 80L) {
         return fast;
      }

      float atHandover = (float)Math.exp(-2.0);
      return atHandover * 80.0F / (float)age;
   }

   float evictionScore(long now) {
      return this.salience / (float)Math.max(1L, now - this.firstKnownTick);
   }

   void sighted(
      Vec3 position,
      long now,
      float sightScore,
      float visualSize,
      boolean startled,
      long scanSerial,
      float armament,
      float armorFraction,
      boolean sneaking,
      boolean owned
   ) {
      this.sighted(
         position,
         now,
         sightScore,
         visualSize,
         startled,
         scanSerial,
         armament,
         armorFraction,
         sneaking,
         owned,
         null,
         false,
         null,
         false,
         0.0F,
         0.0F,
         false,
         List.of(),
         true,
         false
      );
   }

   void sighted(
      Vec3 position,
      long now,
      float sightScore,
      float visualSize,
      boolean startled,
      long scanSerial,
      float armament,
      float armorFraction,
      boolean sneaking,
      boolean owned,
      @Nullable UUID leashHolder,
      boolean tethered,
      @Nullable UUID providedBy,
      boolean afloat,
      float width,
      float height,
      boolean playful,
      List<Kind> leads,
      boolean answers,
      boolean pressing
   ) {
      this.unquiet();
      this.playfulSeen = playful;
      this.answersSeen = answers;
      this.pressingSeen = pressing;
      this.leashHolder = leashHolder;
      this.tethered = tethered;
      this.providedBy = providedBy;
      this.leads = leads;
      this.afloat = afloat;
      this.width = width;
      this.height = height;
      this.apparentSize = visualSize;
      this.ownedSeen = owned;
      this.armament = armament;
      this.armorFraction = armorFraction;
      this.sneakingSeen = sneaking;
      if (startled) {
         this.lastStartledTick = now;
      }

      if (scanSerial != this.lastScanSerial) {
         long since = now - this.lastLookTick;
         if (since > 0L && this.lastLookTick != Long.MIN_VALUE) {
            this.lastVelocity = position.subtract(this.lastKnownPos).scale(1.0 / since);
         }

         this.lastScanSerial = scanSerial;
         this.lastLookTick = now;
      }

      this.lastKnownPos = position;
      this.visible = true;
      this.lastSeenTick = now;
      this.lastFixTick = now;
      this.findScale = 1.0F;
      this.lastSightScore = sightScore;
      this.note(sightScore);
   }

   void lostSight() {
      this.unquiet();
      this.visible = false;
      this.lastSightScore = 0.0F;
   }

   void heard(Vec3 position, long now, float strength) {
      if (this.visible) {
         this.note(strength);
      } else {
         if (this.find(now) < 0.8F) {
            this.findScale = 0.8F;
            this.lastFixTick = now;
         }

         this.lastKnownPos = position;
         this.note(strength);
      }
   }

   void hurtBy(Vec3 position, long now, float severity) {
      this.lastKnownPos = position;
      this.lastFixTick = now;
      this.findScale = 1.0F;
      this.lastHurtTick = now;
      this.note(severity);
   }

   void fedBy(long now) {
      this.lastFedTick = now;
   }

   public boolean fedSince(long tick) {
      return this.lastFedTick != Long.MIN_VALUE && this.lastFedTick > tick;
   }

   public boolean hurtWithin(long now, int window) {
      return this.lastHurtTick != Long.MIN_VALUE && now - this.lastHurtTick < window;
   }

   public boolean unreachableAt(long now, int patienceTicks) {
      return this.unreachableSince != Long.MIN_VALUE && now - this.unreachableSince < patienceTicks;
   }

   void couldNotReach(long now) {
      this.unreachableSince = now;
   }

   void canReach() {
      this.unreachableSince = Long.MIN_VALUE;
   }

   boolean searchFailed(long now) {
      boolean chargedBefore = this.lastSearchCutTick != Long.MIN_VALUE;
      if (chargedBefore && now - this.lastSearchCutTick < 80L) {
         return false;
      }

      this.lastSearchCutTick = now;
      this.findScale *= 0.25F;
      this.presenceEnvelope *= 0.25F;
      return true;
   }

   private void note(float strength) {
      this.salience = Math.max(this.salience, strength);
   }
}

package net.magister.liberanimalium.fauna.know;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.Check;
import net.magister.liberanimalium.fauna.api.Checks;
import net.magister.liberanimalium.fauna.api.Delivery;
import net.magister.liberanimalium.fauna.api.ExpressionKey;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.Ladder;
import net.magister.liberanimalium.fauna.api.TamingMatrix;
import net.magister.liberanimalium.fauna.api.Valence;
import net.magister.liberanimalium.fauna.identity.Temper;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;

public final class Courtship {
   public static final int ACCEPTED = 0;
   public static final int FAMILIAR = 1;
   public static final int RIDE = 2;
   private final Map<UUID, Integer> rungs = new HashMap<>();
   private final Map<UUID, Integer> catches = new HashMap<>();
   private final Map<UUID, Integer> games = new HashMap<>();
   private final Map<UUID, Integer> hits = new HashMap<>();
   private final Map<UUID, Integer> bucks = new HashMap<>();
   private final Map<UUID, Courtship.Ask> asks = new HashMap<>();
   private final Map<UUID, Integer> rested = new HashMap<>();
   private final Map<UUID, Float> walked = new HashMap<>();
   private final Map<UUID, float[]> had = new HashMap<>();
   private final Map<UUID, Long> hadAt = new HashMap<>();
   private final Set<UUID> tolerated = new HashSet<>();
   private long lastPassAge = -1L;
   private long age;
   private static final String KEY = "Climb";

   public int rung(UUID courter) {
      return this.rungs.getOrDefault(courter, 0);
   }

   public int size() {
      return this.rungs.size();
   }

   public static int length(boolean rideable) {
      return rideable ? 3 : 2;
   }

   public boolean complete(UUID courter, boolean rideable) {
      return this.rung(courter) >= length(rideable);
   }

   public int observe(UUID courter, boolean pack, float familiarity, float line, boolean rideable, Courtship.Rite rite) {
      int length = length(rideable);
      int rung = this.rung(courter);
      if (rung >= length) {
         return rung;
      }

      if (rung > 0 && !pack) {
         this.rungs.put(courter, 0);
         this.bucks.remove(courter);
         this.catches.remove(courter);
         this.hits.remove(courter);
         this.rested.remove(courter);
         this.walked.remove(courter);
         rite.undone(courter);
         return 0;
      }

      while (rung < length && rung < 2) {
         boolean holds = rung == 0 ? pack : familiarity >= line;
         if (!holds) {
            break;
         }

         this.pass(courter, rideable, rite);
         rung = this.rung(courter);
      }

      return rung;
   }

   public void forget(UUID courter) {
      this.rungs.remove(courter);
      this.asks.remove(courter);
      this.bucks.remove(courter);
      this.catches.remove(courter);
      this.games.remove(courter);
      this.hits.remove(courter);
      this.rested.remove(courter);
      this.walked.remove(courter);
      this.had.remove(courter);
      this.hadAt.remove(courter);
      this.tolerated.remove(courter);
   }

   public boolean tolerated(UUID courter, boolean holds) {
      if (holds) {
         return this.tolerated.add(courter);
      }

      this.tolerated.remove(courter);
      return false;
   }

   @Nullable
   public Courtship.Act together(UUID courter, boolean resting, boolean beside, boolean travelling, boolean along, long ticks, float blocks) {
      if (resting && beside) {
         if (this.rested.merge(courter, (int)Math.max(ticks, 0L), Integer::sum) >= 400) {
            this.rested.remove(courter);
            return Courtship.Act.REST;
         }
      } else {
         this.rested.remove(courter);
      }

      if (travelling && along) {
         if (this.walked.merge(courter, Math.max(0.0F, blocks), Float::sum) >= 32.0F) {
            this.walked.remove(courter);
            return Courtship.Act.WALK;
         }
      } else if (travelling) {
         this.walked.remove(courter);
      }

      return null;
   }

   public float had(UUID courter, Courtship.Act act) {
      return 1.0F - (float)Math.pow(0.65F, this.counts(courter)[act.ordinal()]);
   }

   private float[] counts(UUID courter) {
      float[] counts = this.had.computeIfAbsent(courter, c -> new float[Courtship.Act.values().length]);
      long last = this.hadAt.getOrDefault(courter, this.age);
      if (this.age > last) {
         float keep = (float)Math.pow(0.5, (this.age - last) / 1200.0);

         for (int i = 0; i < counts.length; i++) {
            counts[i] *= keep;
         }
      }

      this.hadAt.put(courter, this.age);
      return counts;
   }

   public void age(long age) {
      this.age = age;
   }

   public boolean passedWithin(int ticks) {
      return this.lastPassAge >= 0L && this.age - this.lastPassAge < ticks;
   }

   public void pass(UUID courter, boolean rideable, Courtship.Rite rite) {
      int length = length(rideable);
      int rung = this.rungs.merge(courter, 1, Integer::sum);
      this.lastPassAge = this.age;
      this.trim(courter);
      rite.performed(courter, rung, length);
      if (rung >= length) {
         rite.completed(courter);
      }
   }

   public void forceRung(UUID courter, int target, boolean rideable, Courtship.Rite rite) {
      int want = Mth.clamp(target, 0, length(rideable));
      this.rungs.remove(courter);

      for (int i = 0; i < want; i++) {
         this.pass(courter, rideable, rite);
      }

      if (want == 0) {
         this.rungs.put(courter, 0);
      }
   }

   @Nullable
   public Check asking(UUID courter) {
      Courtship.Ask ask = this.asks.get(courter);
      return ask == null ? null : ask.check();
   }

   @Nullable
   public Check guide(UUID courter, Ladder pool, Wants wants, float hunger, float wary, Temper temper, boolean rideable, boolean present, Courtship.Rite rite) {
      return this.guide(courter, pool, wants, hunger, wary, temper, rideable, present, rite, true);
   }

   public Check guide(
      UUID courter, Ladder pool, Wants wants, float hunger, float wary, Temper temper, boolean rideable, boolean present, Courtship.Rite rite, boolean quarry
   ) {
      int rung = this.rung(courter);
      List<Check> list = askable(pool, rung, rideable);
      Courtship.Ask ask = this.asks.get(courter);
      if (ask != null && !ask.done() && ask.rung() == rung && list.contains(ask.check())) {
         Courtship.Act asked = actOf(ask.check());
         boolean lapsed = asked != null && asked.situational() && desire(asked, wants, hunger, wary, quarry) <= 0.1F;
         Check instead = lapsed ? choose(list, wants, hunger, wary, temper, this.counts(courter), quarry) : null;
         if (instead == null || instead == ask.check()) {
            if (present && !ask.told()) {
               this.asks.put(courter, new Courtship.Ask(rung, ask.check(), true, false));
               rite.asks(courter, ask.check());
            }

            return ask.check();
         }
      }

      Check next = choose(list, wants, hunger, wary, temper, this.counts(courter), quarry);
      if (next == null) {
         this.asks.remove(courter);
         return null;
      }

      this.asks.put(courter, new Courtship.Ask(rung, next, present, false));
      if (present) {
         rite.asks(courter, next);
      }

      return next;
   }

   public boolean answered(UUID courter, Courtship.Act act) {
      Courtship.Ask ask = this.asks.get(courter);
      if (ask != null && !ask.done() && actOf(ask.check()) == act) {
         this.asks.put(courter, new Courtship.Ask(ask.rung(), ask.check(), ask.told(), true));
         return true;
      } else {
         return false;
      }
   }

   public static List<Check> askable(Ladder pool, int rung, boolean rideable) {
      List<Check> out = new ArrayList<>();
      if (rung >= length(rideable)) {
         return out;
      }

      for (Ladder.StageDef stage : pool.stages()) {
         Check.Engagement engages = stage.check().engages();

         boolean fits = switch (rung) {
            case 0 -> engages == Check.Engagement.APPROACH
               || engages == Check.Engagement.FEEDING
               || engages == Check.Engagement.HUNT_TOGETHER
               || engages == Check.Engagement.GIFT;
            case 1 -> engages != Check.Engagement.APPROACH && engages != Check.Engagement.RODEO && engages != Check.Engagement.DEFEND;
            default -> engages == Check.Engagement.RODEO;
         };
         if (fits) {
            out.add(stage.check());
         }
      }

      return out;
   }

   @Nullable
   static Check choose(List<Check> list, Wants wants, float hunger, float wary, Temper temper, float[] given, boolean quarry) {
      Check best = null;
      float most = -1.0F;

      for (Check check : list) {
         Courtship.Act act = actOf(check);
         float want = act == null ? 1.0F : desire(act, wants, hunger, wary, quarry);
         float score = act == null
            ? 1.0F
            : (want > 0.0F ? Math.max(want, 0.1F) : 0.001F) * weight(receptivity(act, temper)) * (float)Math.pow(0.35F, given[act.ordinal()]);
         if (score > most) {
            most = score;
            best = check;
         }
      }

      return best;
   }

   public static float desire(Courtship.Act act, Wants wants, float hunger, float wary) {
      return desire(act, wants, hunger, wary, true);
   }

   public static float desire(Courtship.Act act, Wants wants, float hunger, float wary, boolean quarry) {
      return Mth.clamp(switch (act) {
         case MEAL, PREY -> hunger;
         case TOUCH -> wants.company();
         case TAG, SPAR -> wants.boredom();
         case HEED -> wary;
         case DEFEND -> 0.0F;
         case REST -> wants.want() == Wants.Want.TIRED ? 1.0F : 0.0F;
         case WALK -> wants.want() == Wants.Want.RESTLESS ? 1.0F : wants.boredom();
         case HUNT_TOGETHER -> quarry ? hunger : 0.0F;
         case GIFT -> wants.boredom();
      }, 0.0F, 1.0F);
   }

   public boolean playing(UUID courter) {
      return this.rung(courter) >= 1;
   }

   public boolean atMount(UUID courter, boolean rideable) {
      return rideable && this.rung(courter) == 2;
   }

   public boolean caught(UUID courter, int target) {
      int landed = this.catches.merge(courter, 1, Integer::sum);
      if (landed < Math.max(1, target)) {
         return false;
      }

      this.catches.remove(courter);
      this.games.merge(courter, 1, Integer::sum);
      return true;
   }

   public int catchesBy(UUID courter) {
      return this.catches.getOrDefault(courter, 0);
   }

   public int gamesBy(UUID courter) {
      return this.games.getOrDefault(courter, 0);
   }

   public boolean sparred(UUID courter, int target) {
      int landed = this.hits.merge(courter, 1, Integer::sum);
      if (landed < Math.max(1, target)) {
         return false;
      }

      this.hits.remove(courter);
      return true;
   }

   public int hitsBy(UUID courter) {
      return this.hits.getOrDefault(courter, 0);
   }

   public boolean bucked(UUID courter, int target, boolean rideable, Courtship.Rite rite) {
      if (!this.atMount(courter, rideable)) {
         return false;
      }

      int landed = this.bucks.merge(courter, 1, Integer::sum);
      if (landed < Math.max(1, target)) {
         return false;
      }

      this.bucks.remove(courter);
      this.pass(courter, rideable, rite);
      return true;
   }

   public void ridingOnly(@Nullable UUID rider) {
      if (!this.bucks.isEmpty()) {
         if (rider == null) {
            this.bucks.clear();
         } else {
            this.bucks.keySet().removeIf(id -> !id.equals(rider));
         }
      }
   }

   public int bucksBy(UUID courter) {
      return this.bucks.getOrDefault(courter, 0);
   }

   public static Courtship.TouchFace faceOf(@Nullable Valence groom, @Nullable Ladder.StageDef stage, boolean fits, int heldTicks, boolean playing) {
      if (playing) {
         return Courtship.TouchFace.CATCH;
      } else if (groom == Valence.HATES) {
         return Courtship.TouchFace.INTRUSION;
      } else if (stage == null || !fits || stage.check().engages() != Check.Engagement.TOUCH) {
         return Courtship.TouchFace.DECLINE;
      } else {
         return heldTicks < stage.holdTicks() ? Courtship.TouchFace.HOLDING : Courtship.TouchFace.CHECK;
      }
   }

   @Nullable
   public static Courtship.Act actOf(Check check) {
      return switch (check.engages()) {
         case APPROACH -> Courtship.Act.HEED;
         case FEEDING -> check.fedBy().contains(Delivery.PREY) ? Courtship.Act.PREY : Courtship.Act.MEAL;
         case TOUCH -> Courtship.Act.TOUCH;
         case TAG -> Courtship.Act.TAG;
         case SPAR -> Courtship.Act.SPAR;
         case DEFEND -> Courtship.Act.DEFEND;
         case RODEO -> null;
         case REST -> Courtship.Act.REST;
         case WALK -> Courtship.Act.WALK;
         case HUNT_TOGETHER -> Courtship.Act.HUNT_TOGETHER;
         case GIFT -> Courtship.Act.GIFT;
      };
   }

   public Courtship.Pay court(SocialBook social, UUID courter, Courtship.Act act, float units, Temper temper, Wants wants, float want, boolean asked) {
      if (this.answered(courter, act)) {
         want = 1.0F;
         asked = true;
      } else if (act != Courtship.Act.DEFEND) {
         want = 0.0F;
         asked = false;
      }

      SocialBook.Opinion opinion = social.of(courter);
      Courtship.Pay pay = pay(act, units, temper, want, asked, this.had(courter, act), opinion.fear(), opinion, this.playing(courter));
      if (pay.like() + pay.familiarity() > 0.0F) {
         this.counts(courter)[act.ordinal()] += Mth.clamp(units, 0.0F, 1.0F);
      }

      social.courted(courter, pay.like(), pay.familiarity());
      return pay;
   }

   public static float receptivity(Courtship.Act act, Temper temper) {
      return Mth.clamp(switch (act) {
         case MEAL -> (temper.get(Trait.SYMPATHY) + 1.0F - temper.get(Trait.AGGRESSION)) / 2.0F;
         case PREY -> temper.get(Trait.AGGRESSION);
         case TOUCH -> temper.get(Trait.SYMPATHY);
         case TAG -> temper.get(Trait.ENERGY);
         case SPAR -> (temper.get(Trait.DOMINANCE) + temper.get(Trait.AGGRESSION)) / 2.0F;
         case HEED -> (temper.get(Trait.DOMINANCE) + temper.get(Trait.NERVOUSNESS)) / 2.0F;
         case DEFEND -> 1.0F - temper.get(Trait.BRAVERY);
         case REST -> (temper.get(Trait.SYMPATHY) + 1.0F - temper.get(Trait.NERVOUSNESS)) / 2.0F;
         case WALK -> temper.get(Trait.ENERGY);
         case HUNT_TOGETHER -> temper.get(Trait.AGGRESSION);
         case GIFT -> (temper.get(Trait.ENERGY) + temper.get(Trait.SYMPATHY)) / 2.0F;
      }, 0.0F, 1.0F);
   }

   public static boolean answers(Wants.Want want, Courtship.Act act) {
      return switch (want) {
         case HUNGRY -> act == Courtship.Act.MEAL || act == Courtship.Act.PREY;
         case LONELY -> act == Courtship.Act.TOUCH;
         case PLAYFUL -> act == Courtship.Act.TAG || act == Courtship.Act.SPAR;
         case THREATENED -> act == Courtship.Act.DEFEND;
         default -> false;
      };
   }

   public static float weight(float receptivity) {
      return Mth.lerp(Mth.clamp(receptivity, 0.0F, 1.0F), 0.4F, 1.48F);
   }

   public static boolean asked(float want) {
      return want >= 0.5F;
   }

   public static boolean asksToPlay(float boredom, Temper temper) {
      return boredom >= 0.8F * (1.0F - temper.get(Trait.ENERGY));
   }

   public static float beg(float craving, float hunger) {
      float h = Mth.clamp(hunger, 0.0F, 1.0F);
      return Mth.clamp(craving, 0.0F, 1.0F) * (0.10500002F + 0.895F * h);
   }

   public static float preyCraving(TamingMatrix matrix) {
      Valence row = matrix.act("live_prey");
      if (row == null) {
         return 0.66F;
      }

      return switch (row) {
         case CRAVES, LIKES -> 1.0F;
         case DISLIKES, HATES -> 0.0F;
         default -> 0.66F;
      };
   }

   public static Courtship.Pay pay(
      Courtship.Act act, float units, Temper temper, float want, boolean asked, float had, float fear, SocialBook.Opinion opinion, boolean accepted
   ) {
      if (units <= 0.0F) {
         return Courtship.Pay.NOTHING;
      }

      if (!act.liking() && !accepted) {
         return Courtship.Pay.NOTHING;
      }

      float w = weight(receptivity(act, temper));
      float ask = (0.5F + 0.5F * Mth.clamp(want, 0.0F, 1.0F)) * (1.0F - Mth.clamp(had, 0.0F, 1.0F));
      float pace = act.liking() ? 1.0F : 0.33F;
      float paid = act.base() * pace * units * w * ask * damp(fear);
      if (!act.liking()) {
         return new Courtship.Pay(0.0F, paid * dim(opinion.familiarity()), asked);
      }

      float familiarity = accepted ? paid * 0.586F * dim(opinion.familiarity()) : 0.0F;
      return new Courtship.Pay(paid * dim(opinion.like()), familiarity, asked);
   }

   public static Courtship.Pay company(float minutes, boolean following, Temper temper, float fear, SocialBook.Opinion opinion, float packLine) {
      if (minutes <= 0.0F) {
         return Courtship.Pay.NOTHING;
      }

      float scale = damp(fear) * (following ? 2.0F : 1.0F);
      float familiarity = familiarityLine(temper) / companyMinutes(temper) * minutes * scale;
      float known = Mth.clamp(opinion.familiarity() + familiarity, 0.0F, 1.0F);
      float like = Math.min(Math.max(0.0F, packLine - opinion.like()), 0.237F * known * scale * minutes);
      return new Courtship.Pay(like, familiarity, true);
   }

   public static float companyMinutes(Temper temper) {
      return Mth.lerp(comfort(temper), 15.0F, 30.0F);
   }

   public static float damp(float fear) {
      return 1.0F - Mth.clamp(fear, 0.0F, 1.0F);
   }

   static float dim(float value) {
      return Mth.lerp(Mth.clamp(value, 0.0F, 1.0F), 1.0F, 0.25F);
   }

   public static float comfort(Temper temper) {
      return comfort(temper.get(Trait.DOMINANCE), temper.get(Trait.NERVOUSNESS), temper.get(Trait.SYMPATHY));
   }

   public static float comfort(float dominance, float nervousness, float sympathy) {
      return Mth.clamp((dominance + nervousness + 1.0F - sympathy) / 3.0F, 0.0F, 1.0F);
   }

   public static float familiarityLine(Temper temper) {
      return familiarityLine(comfort(temper));
   }

   public static float familiarityLine(float comfort) {
      return Mth.clamp(Mth.lerp(Mth.clamp(comfort, 0.0F, 1.0F), 0.239F, 1.104F), 0.3F, 0.95F);
   }

   public static int catchTarget(Temper temper) {
      return catchTarget(temper.get(Trait.ENERGY), temper.get(Trait.NERVOUSNESS));
   }

   public static int catchTarget(float energy, float nervousness) {
      float t = Mth.clamp((energy + nervousness) / 2.0F, 0.0F, 1.0F);
      return 2 + Math.min(2, (int)Math.floor(t * 3.0F));
   }

   public static int sparTarget(Temper temper) {
      return sparTarget(temper.get(Trait.AGGRESSION), temper.get(Trait.DOMINANCE));
   }

   public static int sparTarget(float aggression, float dominance) {
      float t = Mth.clamp((aggression + dominance) / 2.0F, 0.0F, 1.0F);
      return 3 + Math.min(3, (int)Math.floor(t * 4.0F));
   }

   public static int buckTarget(Temper temper) {
      return buckTarget(temper.get(Trait.AGGRESSION), temper.get(Trait.DOMINANCE));
   }

   public static int buckTarget(float aggression, float dominance) {
      float t = Mth.clamp((aggression + dominance) / 2.0F, 0.0F, 1.0F);
      return 1 + Math.min(5, (int)Math.floor(t * 6.0F));
   }

   public static float rungRelief(int rung, int length) {
      if (rung <= 0) {
         return 0.0F;
      }

      float t = length <= 1 ? 1.0F : (float)(rung - 1) / (length - 1);
      return Mth.lerp(Mth.clamp(t, 0.0F, 1.0F), 0.3F, 0.85F);
   }

   public float reliefFor(UUID courter, boolean rideable) {
      return rungRelief(this.rung(courter), length(rideable));
   }

   private void trim(UUID justWritten) {
      if (this.rungs.size() > 16) {
         UUID lowest = null;
         int least = Integer.MAX_VALUE;

         for (Entry<UUID, Integer> entry : this.rungs.entrySet()) {
            if (!entry.getKey().equals(justWritten) && entry.getValue() < least) {
               least = entry.getValue();
               lowest = entry.getKey();
            }
         }

         if (lowest != null) {
            this.rungs.remove(lowest);
         }
      }
   }

   public void save(CompoundTag tag) {
      ListTag list = new ListTag();
      Set<UUID> pairs = new LinkedHashSet<>(this.rungs.keySet());
      pairs.addAll(this.asks.keySet());

      for (UUID courter : pairs) {
         CompoundTag row = new CompoundTag();
         row.putUUID("Id", courter);
         row.putInt("R", this.rung(courter));
         Courtship.Ask ask = this.asks.get(courter);
         if (ask != null && !ask.done() && ask.rung() == this.rung(courter)) {
            row.putString("A", ask.check().id());
         }

         list.add(row);
      }

      tag.put("Climb", list);
   }

   public void load(CompoundTag tag) {
      this.rungs.clear();
      this.asks.clear();
      ListTag list = tag.getList("Climb", 10);

      for (int i = 0; i < list.size(); i++) {
         CompoundTag row = list.getCompound(i);
         if (row.hasUUID("Id")) {
            UUID id = row.getUUID("Id");
            int rung = Math.max(0, row.getInt("R"));
            if (rung > 0 || !row.contains("A")) {
               this.rungs.put(id, rung);
            }

            Checks.byId(row.getString("A")).ifPresent(check -> this.asks.put(id, new Courtship.Ask(rung, check, false, false)));
         }
      }
   }

   public enum Act {
      MEAL(true, 0.102F),
      PREY(true, 0.102F),
      DEFEND(true, 0.1F),
      HEED(true, 0.06F),
      TOUCH(false, 0.183F),
      TAG(false, 0.228F),
      SPAR(false, 0.274F),
      REST(false, 0.183F),
      WALK(false, 0.228F),
      HUNT_TOGETHER(true, 0.1F),
      GIFT(true, 0.102F);

      private final boolean liking;
      private final float base;

      Act(boolean liking, float base) {
         this.liking = liking;
         this.base = base;
      }

      public boolean situational() {
         return this == REST || this == WALK || this == HUNT_TOGETHER;
      }

      public boolean liking() {
         return this.liking;
      }

      public float base() {
         return this.base;
      }
   }

   private record Ask(int rung, Check check, boolean told, boolean done) {
   }

   public record Pay(float like, float familiarity, boolean asked) {
      public static final Courtship.Pay NOTHING = new Courtship.Pay(0.0F, 0.0F, false);

      public void shown(Halter halter, UUID courter, Consumer<ExpressionKey> moment) {
         if (!(this.like + this.familiarity <= 0.0F)) {
            moment.accept(ExpressionKey.body(this.asked ? ExpressionKey.Body.TOUCH_ACCEPT : ExpressionKey.Body.TOUCH_DECLINE));
            if (this.asked) {
               halter.tellAskAnswered(courter);
            }
         }
      }
   }

   public interface Rite {
      Courtship.Rite NONE = (courter, rung, length) -> {};

      void performed(UUID var1, int var2, int var3);

      default void completed(UUID courter) {
      }

      default void undone(UUID courter) {
      }

      default void asks(UUID courter, Check check) {
      }
   }

   public enum TouchFace {
      INTRUSION,
      DECLINE,
      HOLDING,
      CHECK,
      CATCH;
   }
}

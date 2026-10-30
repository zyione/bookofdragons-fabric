package net.magister.liberanimalium.fauna.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public final class Ladder {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int DEFAULT_HOLD_TICKS = 60;
   private final List<Ladder.StageDef> stages;

   public static Ladder defaultLadder() {
      return new Ladder(List.of(Ladder.StageDef.of(Checks.MEAL), Ladder.StageDef.of(Checks.TOUCH), Ladder.StageDef.of(Checks.MOUNT)));
   }

   private Ladder(List<Ladder.StageDef> stages) {
      this.stages = List.copyOf(stages);
   }

   public static Ladder of(List<Ladder.StageDef> stages) {
      return stages.isEmpty() ? defaultLadder() : new Ladder(stages);
   }

   public List<Ladder.StageDef> stages() {
      return this.stages;
   }

   public boolean recognises(Check check) {
      return this.stageOf(check) != null;
   }

   public boolean recognises(Check.Engagement engagement) {
      return this.stageOf(engagement) != null;
   }

   @Nullable
   public Ladder.StageDef stageOf(Check check) {
      for (Ladder.StageDef stage : this.stages) {
         if (stage.check() == check) {
            return stage;
         }
      }

      return null;
   }

   @Nullable
   public Ladder.StageDef stageOf(Check.Engagement engagement) {
      for (Ladder.StageDef stage : this.stages) {
         if (stage.check().engages() == engagement) {
            return stage;
         }
      }

      return null;
   }

   @Nullable
   public Check fedBy(Delivery delivery) {
      for (Ladder.StageDef stage : this.stages) {
         Check check = stage.check();
         if (check.engages() == Check.Engagement.FEEDING && check.fedBy().contains(delivery)) {
            return check;
         }
      }

      return null;
   }

   public static Ladder parse(@Nullable JsonObject taming, String speciesId) {
      return parse(taming, speciesId, null);
   }

   public static Ladder parse(@Nullable JsonObject taming, String speciesId, @Nullable String namespace) {
      if (taming != null && taming.has("stages")) {
         List<Ladder.StageDef> stages = new ArrayList<>();

         for (JsonElement element : GsonHelper.getAsJsonArray(taming, "stages")) {
            Ladder.StageDef stage = parseStage(element, speciesId, namespace);
            if (stage == null) {
               return defaultLadder();
            }

            stages.add(stage);
         }

         if (stages.isEmpty()) {
            return defaultLadder();
         }

         for (Check check : stages.stream().map(Ladder.StageDef::check).distinct().toList()) {
            if (stages.stream().filter(s -> s.check() == check).count() > 1L) {
               LOGGER.warn("[taming] {}: act {} listed more than once - using the default acts", speciesId, check);
               return defaultLadder();
            }
         }

         return new Ladder(stages);
      } else {
         return defaultLadder();
      }
   }

   @Nullable
   private static Ladder.StageDef parseStage(JsonElement element, String speciesId, @Nullable String namespace) {
      try {
         JsonObject obj = element.getAsJsonObject();
         String word = GsonHelper.getAsString(obj, "check");
         Check check = Checks.resolve(word, namespace).orElse(null);
         if (check == null) {
            LOGGER.warn("[taming] {}: act '{}' is not a registered check (known: {}) - using the default acts", new Object[]{speciesId, word, Checks.all()});
            return null;
         }

         String item = obj.has("item") ? GsonHelper.getAsString(obj, "item") : null;
         int holdTicks = Math.max(0, GsonHelper.getAsInt(obj, "hold_ticks", check.defaultHoldTicks()));
         boolean crouched = GsonHelper.getAsBoolean(obj, "crouched", false);

         for (String gone : new String[]{"count", "bucks"}) {
            if (obj.has(gone)) {
               LOGGER.warn(
                  "[taming] {}: '{}' on {} is ignored - how much an act pays and how long the rodeo runs come from temperament",
                  new Object[]{speciesId, gone, check}
               );
            }
         }

         return new Ladder.StageDef(check, item, holdTicks, crouched);
      } catch (RuntimeException e) {
         LOGGER.warn("[taming] {}: unreadable act {} - using the default acts", speciesId, element);
         return null;
      }
   }

   public record StageDef(Check check, @Nullable String item, int holdTicks, boolean crouched) {
      public static Ladder.StageDef of(Check check) {
         return new Ladder.StageDef(check, null, check.defaultHoldTicks(), false);
      }
   }
}

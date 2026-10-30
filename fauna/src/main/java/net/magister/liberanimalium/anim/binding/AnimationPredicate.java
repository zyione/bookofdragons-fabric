package net.magister.liberanimalium.anim.binding;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.magister.liberanimalium.anim.api.SignalView;

public final class AnimationPredicate {
   public static final AnimationPredicate ALWAYS = new AnimationPredicate(List.of(), List.of(), List.of(), null);
   private final List<AnimationPredicate.EnumEntry> enumEntries;
   private final List<AnimationPredicate.BoolEntry> boolEntries;
   private final List<AnimationPredicate.ScalarEntry> scalarEntries;
   private final AnimationPredicate notPredicate;

   private AnimationPredicate(
      List<AnimationPredicate.EnumEntry> enumEntries,
      List<AnimationPredicate.BoolEntry> boolEntries,
      List<AnimationPredicate.ScalarEntry> scalarEntries,
      AnimationPredicate notPredicate
   ) {
      this.enumEntries = enumEntries;
      this.boolEntries = boolEntries;
      this.scalarEntries = scalarEntries;
      this.notPredicate = notPredicate;
   }

   public static AnimationPredicate parse(JsonObject json, Predicate<String> isScalarKey) {
      if (json != null && json.size() != 0) {
         List<AnimationPredicate.EnumEntry> enumEntries = new ArrayList<>();
         List<AnimationPredicate.BoolEntry> boolEntries = new ArrayList<>();
         List<AnimationPredicate.ScalarEntry> scalarEntries = new ArrayList<>();
         AnimationPredicate notPredicate = null;

         for (Entry<String, JsonElement> entry : json.entrySet()) {
            String rawKey = entry.getKey();
            JsonElement value = entry.getValue();
            if (rawKey.equals("not") && value.isJsonObject()) {
               notPredicate = parse(value.getAsJsonObject(), isScalarKey);
            } else if (rawKey.equals("scalar") && value.isJsonObject()) {
               for (Entry<String, JsonElement> s : value.getAsJsonObject().entrySet()) {
                  if (s.getValue().isJsonObject()) {
                     scalarEntries.add(AnimationPredicate.ScalarEntry.parse(s.getKey(), s.getValue().getAsJsonObject()));
                  } else if (s.getValue().isJsonPrimitive() && s.getValue().getAsJsonPrimitive().isNumber()) {
                     scalarEntries.add(new AnimationPredicate.ScalarEntry(s.getKey(), AnimationPredicate.Op.GTE, s.getValue().getAsFloat(), 0.0F));
                  }
               }
            } else {
               if (isScalarKey.test(rawKey)) {
                  if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                     scalarEntries.add(new AnimationPredicate.ScalarEntry(rawKey, AnimationPredicate.Op.GTE, value.getAsFloat(), 0.0F));
                     continue;
                  }

                  if (value.isJsonObject()) {
                     scalarEntries.add(AnimationPredicate.ScalarEntry.parse(rawKey, value.getAsJsonObject()));
                     continue;
                  }
               }

               String key = rawKey.endsWith("In") ? rawKey.substring(0, rawKey.length() - 2) : rawKey;
               if (value.isJsonPrimitive()) {
                  if (value.getAsJsonPrimitive().isBoolean()) {
                     boolEntries.add(new AnimationPredicate.BoolEntry(key, value.getAsBoolean()));
                  } else {
                     enumEntries.add(new AnimationPredicate.EnumEntry(key, Set.of(value.getAsString())));
                  }
               } else if (!value.isJsonArray()) {
                  if (value.isJsonObject()) {
                     scalarEntries.add(AnimationPredicate.ScalarEntry.parse(key, value.getAsJsonObject()));
                  }
               } else {
                  JsonArray arr = value.getAsJsonArray();
                  Set<String> values = new LinkedHashSet<>();

                  for (JsonElement el : arr) {
                     values.add(el.getAsString());
                  }

                  enumEntries.add(new AnimationPredicate.EnumEntry(key, Set.copyOf(values)));
               }
            }
         }

         return new AnimationPredicate(List.copyOf(enumEntries), List.copyOf(boolEntries), List.copyOf(scalarEntries), notPredicate);
      } else {
         return ALWAYS;
      }
   }

   public Set<String> enumChoices(String key) {
      for (AnimationPredicate.EnumEntry entry : this.enumEntries) {
         if (entry.key().equals(key)) {
            return entry.allowedValues();
         }
      }

      return null;
   }

   public boolean test(SignalView signals) {
      for (AnimationPredicate.EnumEntry entry : this.enumEntries) {
         String val = signals.enumValue(entry.key());
         if (val == null || !entry.allowedValues().contains(val)) {
            return false;
         }
      }

      for (AnimationPredicate.BoolEntry entry : this.boolEntries) {
         Boolean val = signals.boolValue(entry.key());
         if (val == null || val != entry.required()) {
            return false;
         }
      }

      for (AnimationPredicate.ScalarEntry entry : this.scalarEntries) {
         Float val = signals.scalarValue(entry.key());
         if (val == null || !entry.test(val)) {
            return false;
         }
      }

      return this.notPredicate == null || !this.notPredicate.test(signals);
   }

   private record BoolEntry(String key, boolean required) {
   }

   private record EnumEntry(String key, Set<String> allowedValues) {
   }

   private enum Op {
      GT,
      GTE,
      LT,
      LTE,
      EQ,
      BETWEEN;
   }

   private record ScalarEntry(String key, AnimationPredicate.Op op, float a, float b) {
      static AnimationPredicate.ScalarEntry parse(String key, JsonObject o) {
         if (o.has("between") && o.get("between").isJsonArray()) {
            JsonArray arr = o.getAsJsonArray("between");
            return new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.BETWEEN, arr.get(0).getAsFloat(), arr.get(1).getAsFloat());
         } else if (o.has("min") && o.has("max")) {
            return new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.BETWEEN, o.get("min").getAsFloat(), o.get("max").getAsFloat());
         } else if (o.has("min")) {
            return new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.GTE, o.get("min").getAsFloat(), 0.0F);
         } else if (o.has("max")) {
            return new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.LTE, o.get("max").getAsFloat(), 0.0F);
         } else if (o.has(">=")) {
            return new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.GTE, o.get(">=").getAsFloat(), 0.0F);
         } else if (o.has("gte")) {
            return new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.GTE, o.get("gte").getAsFloat(), 0.0F);
         } else if (o.has(">")) {
            return new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.GT, o.get(">").getAsFloat(), 0.0F);
         } else if (o.has("gt")) {
            return new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.GT, o.get("gt").getAsFloat(), 0.0F);
         } else if (o.has("<=")) {
            return new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.LTE, o.get("<=").getAsFloat(), 0.0F);
         } else if (o.has("lte")) {
            return new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.LTE, o.get("lte").getAsFloat(), 0.0F);
         } else if (o.has("<")) {
            return new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.LT, o.get("<").getAsFloat(), 0.0F);
         } else if (o.has("lt")) {
            return new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.LT, o.get("lt").getAsFloat(), 0.0F);
         } else if (o.has("==")) {
            return new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.EQ, o.get("==").getAsFloat(), 0.0F);
         } else {
            return o.has("eq")
               ? new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.EQ, o.get("eq").getAsFloat(), 0.0F)
               : new AnimationPredicate.ScalarEntry(key, AnimationPredicate.Op.GTE, Float.NEGATIVE_INFINITY, 0.0F);
         }
      }

      boolean test(float x) {
         return switch (this.op) {
            case GT -> x > this.a;
            case GTE -> x >= this.a;
            case LT -> x < this.a;
            case LTE -> x <= this.a;
            case EQ -> Math.abs(x - this.a) < 1.0E-4F;
            case BETWEEN -> x >= this.a && x <= this.b;
         };
      }
   }
}

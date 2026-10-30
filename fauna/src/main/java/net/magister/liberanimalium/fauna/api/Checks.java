package net.magister.liberanimalium.fauna.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

public final class Checks {
   private static final Map<String, Check> BY_ID = new LinkedHashMap<>();
   public static final Check MEAL = register(Check.of("meal", Check.Engagement.FEEDING).fedBy(Delivery.HAND, Delivery.DROPPED));
   public static final Check TOUCH = register(Check.of("touch", Check.Engagement.TOUCH).holding(60));
   public static final Check MOUNT = register(Check.of("mount", Check.Engagement.RODEO));
   public static final Check APPROACH = register(Check.of("approach", Check.Engagement.APPROACH));
   private static boolean librarySealed = true;

   private Checks() {
   }

   public static synchronized Check register(Check check) {
      if (librarySealed && check.id().indexOf(58) < 0) {
         throw new IllegalArgumentException("a host's taming check names its namespace: <namespace>:" + check.id());
      }

      Check known = BY_ID.get(check.id());
      if (known != null) {
         if (known.equals(check)) {
            return known;
         } else {
            throw new IllegalStateException(
               "taming check '" + check.id() + "' is already registered as " + known + " (" + known.engages() + ") and cannot also be " + check.engages()
            );
         }
      } else {
         BY_ID.put(check.id(), check);
         return check;
      }
   }

   public static synchronized Optional<Check> byId(@Nullable String word) {
      return word == null ? Optional.empty() : Optional.ofNullable(BY_ID.get(word.toLowerCase(Locale.ROOT)));
   }

   public static synchronized Optional<Check> resolve(@Nullable String word, @Nullable String namespace) {
      if (word == null) {
         return Optional.empty();
      }

      String id = word.toLowerCase(Locale.ROOT);
      if (namespace != null && id.indexOf(58) < 0) {
         Check own = BY_ID.get(namespace + ":" + id);
         if (own != null) {
            return Optional.of(own);
         }
      }

      return Optional.ofNullable(BY_ID.get(id));
   }

   public static synchronized List<Check> all() {
      return Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
   }
}

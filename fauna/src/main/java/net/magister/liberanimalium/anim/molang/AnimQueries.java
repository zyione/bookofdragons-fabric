package net.magister.liberanimalium.anim.molang;

import java.util.List;
import java.util.Locale;
import mod.azure.azurelib.core.molang.MolangParser;
import net.magister.liberanimalium.anim.api.SignalView;

public final class AnimQueries {
   public static final String PREFIX = "query.";
   private static volatile SignalView current;
   private static volatile List<String> installed = List.of();

   private AnimQueries() {
   }

   public static synchronized void install(List<String> signalNames) {
      if (!signalNames.equals(installed)) {
         MolangParser parser = MolangParser.INSTANCE;

         for (String name : signalNames) {
            String key = "query." + name.toLowerCase(Locale.ROOT);
            String signal = name;
            parser.setValue(key, () -> {
               SignalView view = current;
               if (view == null) {
                  return 0.0;
               }

               Float value = view.scalarValue(signal);
               return value == null ? 0.0 : value.doubleValue();
            });
         }

         installed = List.copyOf(signalNames);
      }
   }

   public static void bind(SignalView view) {
      current = view;
   }

   public static SignalView bound() {
      return current;
   }

   public static boolean isInstalled(String queryName) {
      String bare = queryName.startsWith("query.") ? queryName.substring("query.".length()) : queryName;

      for (String name : installed) {
         if (name.equalsIgnoreCase(bare)) {
            return true;
         }
      }

      return false;
   }

   public static List<String> installed() {
      return installed;
   }
}

package net.magister.liberanimalium.anim.molang;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;
import mod.azure.azurelib.core.molang.MolangParser;

public final class Namespace {
   private static final double[] NONE = new double[0];
   private static final ToDoubleFunction<String> NOTHING = name -> 0.0;
   public final String prefix;
   private final Set<String> installed = ConcurrentHashMap.newKeySet();
   private volatile double[] values = NONE;
   private volatile ToDoubleFunction<String> named = NOTHING;

   public Namespace(String prefix, Enum<?>... vars) {
      this.prefix = prefix;

      for (Enum<?> v : vars) {
         int i = v.ordinal();
         String key = prefix + v.name().toLowerCase(Locale.ROOT);
         this.installed.add(key);
         MolangParser.INSTANCE.setValue(key, () -> {
            double[] vals = this.values;
            return i < vals.length ? vals[i] : 0.0;
         });
      }
   }

   public void installName(String name) {
      String key = name.toLowerCase(Locale.ROOT);
      if (this.installed.add(this.prefix + key)) {
         MolangParser.INSTANCE.setValue(this.prefix + key, () -> this.named.applyAsDouble(key));
      }
   }

   public void bind(double[] current) {
      this.values = current != null ? current : NONE;
   }

   public void bind(ToDoubleFunction<String> current) {
      this.named = current != null ? current : NOTHING;
   }

   public void unbind() {
      this.values = NONE;
      this.named = NOTHING;
   }
}

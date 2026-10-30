package net.magister.liberanimalium.anim.molang;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mod.azure.azurelib.core.molang.MolangParser;
import mod.azure.azurelib.core.molang.expressions.MolangCompoundValue;
import mod.azure.azurelib.core.molang.expressions.MolangValue;

public final class Expression {
   private static final Pattern QUERY = Pattern.compile("\\bquery\\.([a-zA-Z_][a-zA-Z0-9_]*)");
   private static final Pattern SHORT_PREFIX = Pattern.compile("\\bq\\.");
   private final String source;
   private final MolangValue value;
   private final List<String> queries;
   private final String error;

   private Expression(String source, MolangValue value, List<String> queries, String error) {
      this.source = source;
      this.value = value;
      this.queries = queries;
      this.error = error;
   }

   public static Expression parse(String source) {
      if (source != null && !source.isBlank()) {
         String normalised = SHORT_PREFIX.matcher(source).replaceAll("query.").toLowerCase(Locale.ROOT);
         List<String> queries = new ArrayList<>();
         Matcher m = QUERY.matcher(normalised);

         while (m.find()) {
            queries.add(m.group(1));
         }

         try {
            MolangValue value = MolangParser.parseExpression(normalised);
            return defaulted(value)
               ? new Expression(source, null, List.copyOf(queries), "could not parse (the parser logged it and defaulted it to 0)")
               : new Expression(source, value, List.copyOf(queries), null);
         } catch (RuntimeException e) {
            String why = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new Expression(source, null, List.copyOf(queries), "could not parse (" + why + ")");
         }
      } else {
         return new Expression(source, null, List.of(), null);
      }
   }

   private static boolean defaulted(MolangValue value) {
      if (value == MolangParser.ZERO) {
         return true;
      }

      if (value instanceof MolangCompoundValue compound) {
         for (MolangValue statement : compound.values) {
            if (statement == MolangParser.ZERO) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean present() {
      return this.source != null && !this.source.isBlank();
   }

   public boolean valid() {
      return this.value != null;
   }

   public String error() {
      return this.error;
   }

   public String source() {
      return this.source;
   }

   public List<String> queries() {
      return this.queries;
   }

   public double eval() {
      return this.eval(0.0);
   }

   public double eval(double fallback) {
      if (this.value == null) {
         return fallback;
      }

      try {
         return this.value.get();
      } catch (RuntimeException e) {
         return fallback;
      }
   }
}

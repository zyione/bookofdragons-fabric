package net.magister.liberanimalium.anim.molang;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpringChannelSpec {
   public static final Namespace SPRINGS = new Namespace("spring.");
   public static final float MAX_FREQUENCY_HZ = 10.0F;
   public static final float MAX_DAMPING = 4.0F;
   private static final Gson STRINGS = new GsonBuilder().disableHtmlEscaping().create();
   private static final Pattern NAME = Pattern.compile("[a-z_][a-z0-9_]*");
   private static final Pattern DOTTED = Pattern.compile("(?<![\\w.])([a-z_][a-z0-9_]*)\\.([a-z_][a-z0-9_]*)");
   private static final Pattern SHORT_QUERY = Pattern.compile("\\bq\\.");
   private static final Set<String> SPRING_KEYS = Set.of("target", "frequency", "damping", "limit");
   public static final SpringChannelSpec EMPTY = new SpringChannelSpec();
   public final LinkedHashMap<String, SpringChannelSpec.Spring> springs = new LinkedHashMap<>();
   public final LinkedHashMap<String, SpringChannelSpec.Channel> channels = new LinkedHashMap<>();
   private final List<String> structural = new ArrayList<>();
   private final List<String> report = new ArrayList<>();
   private SpringChannelSpec.Names names = new SpringChannelSpec.Names(b -> true, "", d -> true, "", List.of(), q -> true);

   public boolean isEmpty() {
      return this.springs.isEmpty() && this.channels.isEmpty();
   }

   public static SpringChannelSpec parse(JsonObject json, SpringChannelSpec.Names names) {
      SpringChannelSpec spec = new SpringChannelSpec();
      spec.names = names;
      if (json == null) {
         return spec;
      }

      if (json.has("springs") && json.get("springs").isJsonObject()) {
         for (Entry<String, JsonElement> e : json.getAsJsonObject("springs").entrySet()) {
            String name = e.getKey();
            if (!NAME.matcher(name).matches()) {
               spec.structural.add("springs: '" + name + "' is not a valid name (lower case, digits, _)");
            } else if (!e.getValue().isJsonObject()) {
               spec.structural.add("springs." + name + ": not an object");
            } else {
               JsonObject s = e.getValue().getAsJsonObject();

               for (String k : s.keySet()) {
                  if (!SPRING_KEYS.contains(k)) {
                     spec.structural.add("springs." + name + ": unknown key '" + k + "'");
                  }
               }

               String target = s.has("target") ? s.get("target").getAsString() : "0";
               float frequency = spec.clamp("springs." + name + ".frequency", spec.number(s, "frequency", 2.0F), 0.05F, 10.0F);
               float damping = spec.clamp("springs." + name + ".damping", spec.number(s, "damping", 0.5F), 0.0F, 4.0F);
               float limit = Math.max(0.0F, spec.number(s, "limit", 0.0F));
               spec.springs.put(name, new SpringChannelSpec.Spring(name, target, frequency, damping, limit));
               SPRINGS.installName(name);
            }
         }
      }

      if (json.has("channels") && json.get("channels").isJsonObject()) {
         for (Entry<String, JsonElement> e : json.getAsJsonObject("channels").entrySet()) {
            String key = e.getKey();
            int dot = key.lastIndexOf(46);
            String bone = dot < 0 ? null : key.substring(0, dot);
            String dof = dot < 0 ? null : key.substring(dot + 1);
            if (bone != null && !bone.isEmpty() && names.bone().test(bone) && names.dof().test(dof)) {
               spec.channels.put(key, new SpringChannelSpec.Channel(bone, dof, e.getValue().getAsString()));
            } else {
               spec.structural.add("channels: '" + key + "' is not <bone>.<dof> (bones: " + names.boneHint() + "; dofs: " + names.dofHint() + ")");
            }
         }
      }

      spec.validate();
      return spec;
   }

   private float number(JsonObject o, String key, float fallback) {
      if (!o.has(key)) {
         return fallback;
      }

      try {
         return o.get(key).getAsFloat();
      } catch (RuntimeException e) {
         this.structural.add("'" + key + "' is not a number");
         return fallback;
      }
   }

   private float clamp(String what, float v, float lo, float hi) {
      if (!(v < lo) && !(v > hi) && Float.isFinite(v)) {
         return v;
      }

      float c = Float.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo;
      this.structural.add(what + " " + v + " is outside [" + lo + ", " + hi + "] - using " + c);
      return c;
   }

   public List<String> report() {
      return this.report;
   }

   public void validate() {
      this.report.clear();
      this.report.addAll(this.structural);

      for (SpringChannelSpec.Spring s : this.springs.values()) {
         this.check("springs." + s.name + ".target", s.target(), false);
      }

      for (SpringChannelSpec.Channel c : this.channels.values()) {
         this.check("channels." + c.key(), c.expression(), true);
      }
   }

   private void check(String where, Expression e, boolean isChannel) {
      if (e.present()) {
         if (!e.valid()) {
            this.report.add(where + ": " + e.error());
         } else {
            String text = SHORT_QUERY.matcher(e.source().toLowerCase(Locale.ROOT)).replaceAll("query.");
            Matcher m = DOTTED.matcher(text);

            while (m.find()) {
               String prefix = m.group(1);
               String name = m.group(2);
               if (prefix.equals("math")) {
                  int after = m.end();

                  while (after < text.length() && text.charAt(after) == ' ') {
                     after++;
                  }

                  if ((after >= text.length() || text.charAt(after) != '(') && !name.equals("pi")) {
                     this.report.add(where + ": math." + name + " is not a function call or math.pi - this parser reads it as 0; write the number");
                  }
               } else if (prefix.equals("spring")) {
                  if (!this.springs.containsKey(name)) {
                     this.report.add(where + ": spring." + name + " is not declared in springs (reads 0)");
                  }
               } else if (prefix.equals("query")) {
                  if (!this.names.query().test(name)) {
                     this.report.add(where + ": query." + name + " is not a signal the body publishes (reads 0)");
                  } else if (isChannel) {
                     this.report
                        .add(
                           where
                              + ": reads query."
                              + name
                              + " directly - signals change twenty times a second, so the bone will step; make it a spring's target and read the spring"
                        );
                  }
               } else {
                  SpringChannelSpec.Vars vars = this.vars(prefix);
                  if (vars == null) {
                     this.report.add(where + ": '" + prefix + "." + name + "' is not a name this engine binds (" + this.prefixes() + ") and reads 0");
                  } else if (!vars.known().test(name)) {
                     this.report.add(where + ": " + prefix + "." + name + " does not exist (reads 0); " + prefix + ". names: " + vars.hint());
                  }
               }
            }
         }
      }
   }

   private SpringChannelSpec.Vars vars(String prefix) {
      for (SpringChannelSpec.Vars v : this.names.vars()) {
         if (v.prefix().equals(prefix)) {
            return v;
         }
      }

      return null;
   }

   private String prefixes() {
      StringBuilder sb = new StringBuilder("query., ");

      for (SpringChannelSpec.Vars v : this.names.vars()) {
         sb.append(v.prefix()).append("., ");
      }

      return sb.append("spring., math.").toString();
   }

   public String springsJson(String indent) {
      StringBuilder sb = new StringBuilder("{");
      int n = 0;

      for (SpringChannelSpec.Spring s : this.springs.values()) {
         sb.append(n++ == 0 ? "\n" : ",\n");
         sb.append(indent)
            .append("  \"")
            .append(s.name)
            .append("\": { \"target\": ")
            .append(STRINGS.toJson(s.targetSource()))
            .append(", \"frequency\": ")
            .append(num(s.frequency))
            .append(", \"damping\": ")
            .append(num(s.damping))
            .append(", \"limit\": ")
            .append(num(s.limit))
            .append(" }");
      }

      return sb.append(n == 0 ? "}" : "\n" + indent + "}").toString();
   }

   public String channelsJson(String indent) {
      StringBuilder sb = new StringBuilder("{");
      int n = 0;

      for (SpringChannelSpec.Channel c : this.channels.values()) {
         sb.append(n++ == 0 ? "\n" : ",\n");
         sb.append(indent).append("  \"").append(c.key()).append("\": ").append(STRINGS.toJson(c.source()));
      }

      return sb.append(n == 0 ? "}" : "\n" + indent + "}").toString();
   }

   public static String num(float v) {
      double r = Math.round(v * 1000.0) / 1000.0;
      if (r == 0.0) {
         r = 0.0;
      }

      String s = BigDecimal.valueOf(r).stripTrailingZeros().toPlainString();
      return s.contains(".") ? s : s + ".0";
   }

   public static final class Channel {
      public final String bone;
      public final String dof;
      private Expression expression;

      public Channel(String bone, String dof, String source) {
         this.bone = bone;
         this.dof = dof;
         this.expression = Expression.parse(source);
      }

      public String key() {
         return key(this.bone, this.dof);
      }

      public static String key(String bone, String dof) {
         return bone + "." + dof;
      }

      public Expression expression() {
         return this.expression;
      }

      public String source() {
         return this.expression.source() == null ? "" : this.expression.source();
      }

      public void setSource(String source) {
         this.expression = Expression.parse(source);
      }
   }

   public record Names(
      Predicate<String> bone, String boneHint, Predicate<String> dof, String dofHint, List<SpringChannelSpec.Vars> vars, Predicate<String> query
   ) {
   }

   public static final class Spring {
      public final String name;
      private Expression target;
      public float frequency;
      public float damping;
      public float limit;

      public Spring(String name, String target, float frequency, float damping, float limit) {
         this.name = name;
         this.target = Expression.parse(target);
         this.frequency = frequency;
         this.damping = damping;
         this.limit = limit;
      }

      public Expression target() {
         return this.target;
      }

      public String targetSource() {
         return this.target.source() == null ? "" : this.target.source();
      }

      public void setTarget(String source) {
         this.target = Expression.parse(source);
      }
   }

   public record Vars(String prefix, Predicate<String> known, String hint) {
      public static SpringChannelSpec.Vars of(String prefix, Enum<?>[] values) {
         List<String> keys = new ArrayList<>();

         for (Enum<?> v : values) {
            keys.add(v.name().toLowerCase(Locale.ROOT));
         }

         return new SpringChannelSpec.Vars(prefix, keys::contains, String.join(" ", keys));
      }
   }
}

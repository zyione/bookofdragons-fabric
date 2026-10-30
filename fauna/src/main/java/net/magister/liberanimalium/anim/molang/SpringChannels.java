package net.magister.liberanimalium.anim.molang;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.ObjDoubleConsumer;
import net.magister.liberanimalium.anim.rig.DampedSpring;
import net.magister.liberanimalium.anim.rig.Smoothing;

public final class SpringChannels {
   private final Map<String, DampedSpring> springs = new HashMap<>();
   private final Map<String, Double> values = new HashMap<>();
   private final Map<String, Double> previous = new HashMap<>();
   private final Map<String, Double> channelValues = new HashMap<>();

   public void reset() {
      this.springs.clear();
      this.values.clear();
      this.previous.clear();
      this.channelValues.clear();
   }

   public void step(SpringChannelSpec spec, double seconds, boolean live) {
      this.previous.clear();
      this.previous.putAll(this.values);
      SpringChannelSpec.SPRINGS.bind(this::value);

      try {
         for (SpringChannelSpec.Spring s : spec.springs.values()) {
            DampedSpring spring = this.springs.computeIfAbsent(s.name, n -> new DampedSpring());
            double target = live ? s.target().eval(0.0) : 0.0;
            if (Smoothing.on) {
               spring.step(target, seconds, s.frequency, s.damping, s.limit);
            } else {
               double bound = s.limit > 0.0 ? s.limit : Double.MAX_VALUE;
               spring.reset(Double.isFinite(target) ? Math.max(-bound, Math.min(bound, target)) : 0.0);
            }

            this.values.put(s.name, spring.value());
         }
      } finally {
         SpringChannelSpec.SPRINGS.unbind();
      }
   }

   public void evaluate(SpringChannelSpec spec, double lerp, ObjDoubleConsumer<SpringChannelSpec.Channel> out) {
      SpringChannelSpec.SPRINGS.bind(name -> this.value(name, lerp));

      try {
         for (SpringChannelSpec.Channel c : spec.channels.values()) {
            double v = c.expression().eval(0.0);
            if (!Double.isFinite(v)) {
               v = 0.0;
            }

            this.channelValues.put(c.key(), v);
            out.accept(c, v);
         }
      } finally {
         SpringChannelSpec.SPRINGS.unbind();
      }
   }

   public double value(String name) {
      Double v = this.values.get(name);
      return v == null ? 0.0 : v;
   }

   public double value(String name, double lerp) {
      Double n = this.values.get(name);
      Double o = this.previous.get(name);
      double vN = n == null ? 0.0 : n;
      double vO = o == null ? vN : o;
      return vO + (vN - vO) * lerp;
   }

   public double channel(String key) {
      Double v = this.channelValues.get(key);
      return v == null ? 0.0 : v;
   }

   public Set<String> names() {
      return this.springs.keySet();
   }

   public boolean kick(String name, double unitsPerSecond) {
      DampedSpring s = this.springs.get(name);
      if (s == null) {
         return false;
      }

      s.kick(unitsPerSecond);
      return true;
   }
}

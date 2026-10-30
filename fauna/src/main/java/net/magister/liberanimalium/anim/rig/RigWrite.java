package net.magister.liberanimalium.anim.rig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public final class RigWrite {
   private final BoneSink sink;
   private final Map<String, Float[]> written = new LinkedHashMap<>();
   private final Map<String, String[]> claims = new LinkedHashMap<>();
   private final List<String> violations = new ArrayList<>();
   private String pass = "?";
   private List<String> trace;

   public RigWrite(BoneSink sink) {
      this.sink = sink;
   }

   public void reset() {
      this.written.clear();
      this.claims.clear();
      this.violations.clear();
      this.pass = "?";
   }

   public void beginPass(String name) {
      this.pass = name;
   }

   public float read(String bone, BoneAxis axis) {
      return this.sink.read(bone, axis);
   }

   public boolean has(String bone) {
      return this.sink.has(bone);
   }

   public void set(String bone, BoneAxis axis, float value) {
      if (this.sink.has(bone)) {
         String[] owners = this.claims.computeIfAbsent(bone, b -> new String[BoneAxis.COUNT]);
         String prior = owners[axis.ordinal()];
         if (prior != null) {
            this.violations.add(bone + "." + axis + " set by '" + prior + "' and again by '" + this.pass + "'");
         }

         owners[axis.ordinal()] = this.pass;
         this.apply(bone, axis, value);
      }
   }

   public void correct(String bone, BoneAxis axis) {
      if (this.sink.has(bone)) {
         this.apply(bone, axis, this.sink.read(bone, axis));
      }
   }

   public void add(String bone, BoneAxis axis, float delta) {
      if (this.sink.has(bone)) {
         this.apply(bone, axis, this.sink.read(bone, axis) + delta);
      }
   }

   private void apply(String bone, BoneAxis axis, float value) {
      if (this.trace != null) {
         this.trace.add(String.format(Locale.ROOT, "%-10s %s.%s %+.4f -> %+.4f", this.pass, bone, axis, this.sink.read(bone, axis), value));
      }

      this.sink.write(bone, axis, value);
      this.written.computeIfAbsent(bone, b -> new Float[BoneAxis.COUNT])[axis.ordinal()] = value;
   }

   public void armTrace() {
      this.trace = new ArrayList<>();
   }

   public List<String> drainTrace() {
      List<String> out = this.trace == null ? List.of() : List.copyOf(this.trace);
      this.trace = null;
      return out;
   }

   public void replay() {
      for (Entry<String, Float[]> entry : this.written.entrySet()) {
         Float[] values = entry.getValue();

         for (BoneAxis axis : BoneAxis.values()) {
            Float value = values[axis.ordinal()];
            if (value != null) {
               this.sink.write(entry.getKey(), axis, value);
            }
         }
      }
   }

   public List<String> violations() {
      return List.copyOf(this.violations);
   }

   public int channelsWritten() {
      int n = 0;

      for (Float[] values : this.written.values()) {
         for (Float value : values) {
            if (value != null) {
               n++;
            }
         }
      }

      return n;
   }

   public Float recorded(String bone, BoneAxis axis) {
      Float[] values = this.written.get(bone);
      return values == null ? null : values[axis.ordinal()];
   }
}

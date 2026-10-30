package net.magister.liberanimalium.body.grip;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public final class DrawnHips {
   private double time = Double.NaN;
   private double timeO = Double.NaN;
   private Vec3 hips = Vec3.ZERO;
   private Vec3 hipsO = Vec3.ZERO;
   private Vec3 at = Vec3.ZERO;
   private final Quaternionf turn = new Quaternionf();
   private final Quaternionf torso = new Quaternionf();
   private int notedTick = Integer.MIN_VALUE;
   private Vec3 eased = Vec3.ZERO;
   private Vec3 stood = Vec3.ZERO;
   private Vec3 stoodO = Vec3.ZERO;
   private final Quaternionf easedTorso = new Quaternionf();
   private final Quaternionf stoodTorso = new Quaternionf();
   private final Quaternionf stoodTorsoO = new Quaternionf();
   private boolean seen;
   private double springTime = Double.NaN;
   private Vec3 spring = Vec3.ZERO;
   private Vec3 springVelocity = Vec3.ZERO;
   private Vec3 springBeat = Vec3.ZERO;

   public void note(double time, int tick, Vec3 hips, Vec3 at, Quaternionfc turn, Quaternionfc torso) {
      if (time != this.time) {
         this.timeO = this.time;
         this.hipsO = this.hips;
      }

      this.time = time;
      this.hips = hips;
      this.at = at;
      this.turn.set(turn);
      this.torso.set(torso);
      this.notedTick = tick;
   }

   public void settle(int tick) {
      this.stoodO = this.stood;
      this.stoodTorsoO.set(this.stoodTorso);
      if (tick - this.notedTick > 2) {
         this.seen = false;
         this.eased = this.stood = this.stoodO = Vec3.ZERO;
         this.springTime = Double.NaN;
      } else if (!this.seen) {
         this.seen = true;
         this.eased = this.stood = this.stoodO = this.hips;
         this.easedTorso.set(this.torso);
         this.stoodTorso.set(this.torso);
         this.stoodTorsoO.set(this.torso);
      } else {
         this.eased = this.eased.lerp(this.hips, 0.1);
         this.stood = this.stood.lerp(this.eased, 0.1);
         this.easedTorso.slerp(this.torso, 0.1F);
         this.stoodTorso.slerp(this.easedTorso, 0.1F);
      }
   }

   public boolean seen() {
      return this.seen;
   }

   public Vec3 stood(float partial) {
      return this.stoodO.lerp(this.stood, partial);
   }

   public Quaternionf stoodTorso(float partial) {
      return new Quaternionf(this.stoodTorsoO).slerp(this.stoodTorso, partial);
   }

   public Vec3 hips(double time) {
      if (time != this.time && !Double.isNaN(this.timeO)) {
         double span = this.time - this.timeO;
         double ahead = time - this.time;
         return span > 1.0E-6 && !(span > 1.0) && !(ahead < 0.0) && !(ahead > 1.0)
            ? this.hips.add(this.hips.subtract(this.hipsO).scale(ahead / span))
            : this.hips;
      } else {
         return this.hips;
      }
   }

   public Vec3 at(double time, Vec3 otherwise) {
      return time == this.time ? this.at : otherwise;
   }

   public Quaternionf turn() {
      return new Quaternionf(this.turn);
   }

   public Vec3 answer(double time, Vec3 beat, double rate, double damping) {
      if (Double.isNaN(this.springTime) || time < this.springTime || time - this.springTime > 2.0) {
         this.springTime = time;
         this.spring = this.springBeat = beat;
         this.springVelocity = Vec3.ZERO;
         return this.spring;
      }

      if (time == this.springTime) {
         return this.spring;
      }

      double span = time - this.springTime;
      int steps = (int)Math.ceil(span / 0.05);
      double h = span / steps;
      double stiff = rate * rate;
      double drag = 2.0 * damping * rate;

      for (int i = 1; i <= steps; i++) {
         Vec3 driven = this.springBeat.lerp(beat, (double)i / steps);
         Vec3 pull = driven.subtract(this.spring).scale(stiff).subtract(this.springVelocity.scale(drag));
         this.springVelocity = this.springVelocity.add(pull.scale(h));
         this.spring = this.spring.add(this.springVelocity.scale(h));
      }

      this.springTime = time;
      this.springBeat = beat;
      return this.spring;
   }

   public Vec3 answered() {
      return Double.isNaN(this.springTime) ? Vec3.ZERO : this.spring;
   }
}

package net.magister.liberanimalium.attack.data.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record CurveSet(Optional<CurveSet.Curve> size, Optional<CurveSet.Curve> alpha) {
   public static final CurveSet NONE = new CurveSet(Optional.empty(), Optional.empty());
   public static final Codec<CurveSet> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            CurveSet.Curve.CODEC.optionalFieldOf("size").forGetter(CurveSet::size), CurveSet.Curve.CODEC.optionalFieldOf("alpha").forGetter(CurveSet::alpha)
         )
         .apply(i, CurveSet::new)
   );

   public boolean isEmpty() {
      return this.size.isEmpty() && this.alpha.isEmpty();
   }

   public float sizeAt(float t, float fallback) {
      return this.size.<Float>map(c -> c.at(t)).orElse(fallback);
   }

   public float alphaAt(float t) {
      return this.alpha.<Float>map(c -> c.at(t)).orElse(1.0F);
   }

   public record Curve(List<CurveSet.Point> points) {
      public static final Codec<CurveSet.Curve> CODEC = Codec.FLOAT.listOf().listOf().comapFlatMap(CurveSet.Curve::decode, CurveSet.Curve::encode);

      private static DataResult<CurveSet.Curve> decode(List<List<Float>> raw) {
         if (raw.isEmpty()) {
            return DataResult.error(() -> "curve has no points");
         }

         List<CurveSet.Point> out = new ArrayList<>(raw.size());
         float last = Float.NEGATIVE_INFINITY;

         for (List<Float> pair : raw) {
            if (pair.size() != 2) {
               return DataResult.error(() -> "curve point must be [t, value]");
            }

            float t = pair.get(0);
            if (t < last) {
               return DataResult.error(() -> "curve points must ascend in t");
            }

            last = t;
            out.add(new CurveSet.Point(t, pair.get(1)));
         }

         return DataResult.success(new CurveSet.Curve(List.copyOf(out)));
      }

      private static List<List<Float>> encode(CurveSet.Curve curve) {
         List<List<Float>> out = new ArrayList<>(curve.points.size());

         for (CurveSet.Point p : curve.points) {
            out.add(List.of(p.t(), p.v()));
         }

         return out;
      }

      public float at(float t) {
         if (this.points.isEmpty()) {
            return 1.0F;
         }

         CurveSet.Point first = this.points.get(0);
         if (t <= first.t()) {
            return first.v();
         }

         CurveSet.Point last = this.points.get(this.points.size() - 1);
         if (t >= last.t()) {
            return last.v();
         }

         for (int i = 1; i < this.points.size(); i++) {
            CurveSet.Point b = this.points.get(i);
            if (!(t > b.t())) {
               CurveSet.Point a = this.points.get(i - 1);
               float span = b.t() - a.t();
               if (span <= 1.0E-6F) {
                  return b.v();
               }

               float f = (t - a.t()) / span;
               return a.v() + (b.v() - a.v()) * f;
            }
         }

         return last.v();
      }

      public float peak() {
         float peak = 0.0F;

         for (CurveSet.Point p : this.points) {
            peak = Math.max(peak, p.v());
         }

         return peak;
      }

      public CurveSet.Curve withPoint(int index, float t, float v) {
         List<CurveSet.Point> next = new ArrayList<>(this.points);
         next.set(index, new CurveSet.Point(t, v));
         next.sort((a, b) -> Float.compare(a.t(), b.t()));
         return new CurveSet.Curve(List.copyOf(next));
      }

      public CurveSet.Curve added(float t, float v) {
         List<CurveSet.Point> next = new ArrayList<>(this.points);
         next.add(new CurveSet.Point(t, v));
         next.sort((a, b) -> Float.compare(a.t(), b.t()));
         return new CurveSet.Curve(List.copyOf(next));
      }

      public CurveSet.Curve removed(int index) {
         if (this.points.size() <= 1) {
            return this;
         }

         List<CurveSet.Point> next = new ArrayList<>(this.points);
         next.remove(index);
         return new CurveSet.Curve(List.copyOf(next));
      }

      public static CurveSet.Curve ramp(float from, float to) {
         return new CurveSet.Curve(List.of(new CurveSet.Point(0.0F, from), new CurveSet.Point(1.0F, to)));
      }
   }

   public record Point(float t, float v) {
   }
}

package net.magister.liberanimalium.anim.sample;

import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;
import java.util.List;
import mod.azure.azurelib.animation.controller.keyframe.AzAnimationPoint;
import mod.azure.azurelib.animation.controller.keyframe.AzKeyframe;
import mod.azure.azurelib.animation.easing.AzEasingType;
import mod.azure.azurelib.animation.easing.AzEasingTypes;
import mod.azure.azurelib.core.math.IValue;
import mod.azure.azurelib.core.utils.Interpolations;

public final class ClipSampler {
   private ClipSampler() {
   }

   public static double value(ClipIndex.Channel channel, double tick, boolean rotation, boolean negate, Easings.Curve override) {
      if (channel.n == 0) {
         return empty(rotation, negate, override);
      }

      int i = channel.locate(tick);
      double startTick = channel.startTick(i, tick);
      double length = channel.length[i];
      double start = channel.constStart[i] ? channel.valueStart[i] : channel.exprStart[i].get();
      double end = channel.constEnd[i] ? channel.valueEnd[i] : channel.exprEnd[i].get();
      if (rotation) {
         if (!channel.constStart[i]) {
            start = Math.toRadians(start);
            if (negate) {
               start = -start;
            }
         }

         if (!channel.constEnd[i]) {
            end = Math.toRadians(end);
            if (negate) {
               end = -end;
            }
         }
      }

      Easings.Curve curve = channel.curve[i];
      if (curve == null) {
         return withArguments(channel, i, startTick, length, start, end, override);
      }

      if (override != null) {
         curve = override;
      }

      if (curve.special()) {
         return curve.type().apply(new AzAnimationPoint(channel.frames[i], startTick, length, start, end));
      } else {
         return startTick >= length ? (float)end : Interpolations.lerp(start, end, curve.fn().get(startTick / length));
      }
   }

   private static double withArguments(ClipIndex.Channel channel, int i, double startTick, double length, double start, double end, Easings.Curve override) {
      AzKeyframe<IValue> frame = channel.frames[i];
      AzEasingType type = override != null ? override.type() : frame.easingType();
      if (Easings.special(type)) {
         return type.apply(new AzAnimationPoint(frame, startTick, length, start, end));
      }

      if (startTick >= length) {
         return (float)end;
      }

      List<IValue> args = frame.easingArgs();
      Double2DoubleFunction fn = args.isEmpty() ? Easings.curveFor(type).fn() : type.buildTransformer(args.get(0).get());
      return Interpolations.lerp(start, end, fn.get(startTick / length));
   }

   private static double empty(boolean rotation, boolean negate, Easings.Curve override) {
      double end = 0.0;
      if (rotation) {
         end = Math.toRadians(end);
         if (negate) {
            end = -end;
         }
      }

      AzEasingType type = override != null ? override.type() : AzEasingTypes.LINEAR;
      return Easings.special(type) ? type.apply(new AzAnimationPoint(new AzKeyframe(0.0, () -> 0.0, () -> 0.0), 0.0, 0.0, 0.0, end)) : (float)end;
   }
}

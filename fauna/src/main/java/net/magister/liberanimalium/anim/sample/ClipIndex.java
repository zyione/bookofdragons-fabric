package net.magister.liberanimalium.anim.sample;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import mod.azure.azurelib.animation.controller.keyframe.AzBoneAnimation;
import mod.azure.azurelib.animation.controller.keyframe.AzKeyframe;
import mod.azure.azurelib.animation.controller.keyframe.AzKeyframeStack;
import mod.azure.azurelib.animation.primitive.AzBakedAnimation;
import mod.azure.azurelib.core.math.Constant;
import mod.azure.azurelib.core.math.IValue;

public final class ClipIndex {
   public static final int ROT_X = 0;
   public static final int POS_X = 3;
   public static final int SCALE_X = 6;
   public static final int CHANNELS = 9;
   private static final Map<AzBakedAnimation, ClipIndex> CACHE = Collections.synchronizedMap(new IdentityHashMap<>());
   private final String[] boneNames;
   private final ClipIndex.Channel[] channels;
   private final boolean[] rotates;
   private final boolean[] moves;
   private final boolean[] scales;

   public static ClipIndex of(AzBakedAnimation clip) {
      ClipIndex index = CACHE.get(clip);
      if (index == null) {
         index = new ClipIndex(clip);
         CACHE.put(clip, index);
      }

      return index;
   }

   public static void clear() {
      CACHE.clear();
   }

   public static int cached() {
      return CACHE.size();
   }

   private ClipIndex(AzBakedAnimation clip) {
      AzBoneAnimation[] tracks = clip.boneAnimations();
      this.boneNames = new String[tracks.length];
      this.channels = new ClipIndex.Channel[tracks.length * 9];
      this.rotates = new boolean[tracks.length];
      this.moves = new boolean[tracks.length];
      this.scales = new boolean[tracks.length];

      for (int t = 0; t < tracks.length; t++) {
         AzBoneAnimation track = tracks[t];
         this.boneNames[t] = track.boneName();
         AzKeyframeStack<AzKeyframe<IValue>> rot = track.rotationKeyframes();
         AzKeyframeStack<AzKeyframe<IValue>> pos = track.positionKeyframes();
         AzKeyframeStack<AzKeyframe<IValue>> scale = track.scaleKeyframes();
         this.rotates[t] = !rot.xKeyframes().isEmpty();
         this.moves[t] = !pos.xKeyframes().isEmpty();
         this.scales[t] = !scale.xKeyframes().isEmpty();
         int base = t * 9;
         this.channels[base + 0] = new ClipIndex.Channel(rot.xKeyframes());
         this.channels[base + 0 + 1] = new ClipIndex.Channel(rot.yKeyframes());
         this.channels[base + 0 + 2] = new ClipIndex.Channel(rot.zKeyframes());
         this.channels[base + 3] = new ClipIndex.Channel(pos.xKeyframes());
         this.channels[base + 3 + 1] = new ClipIndex.Channel(pos.yKeyframes());
         this.channels[base + 3 + 2] = new ClipIndex.Channel(pos.zKeyframes());
         this.channels[base + 6] = new ClipIndex.Channel(scale.xKeyframes());
         this.channels[base + 6 + 1] = new ClipIndex.Channel(scale.yKeyframes());
         this.channels[base + 6 + 2] = new ClipIndex.Channel(scale.zKeyframes());
      }
   }

   public int trackCount() {
      return this.boneNames.length;
   }

   public String boneName(int track) {
      return this.boneNames[track];
   }

   public ClipIndex.Channel channel(int track, int slot) {
      return this.channels[track * 9 + slot];
   }

   public boolean rotates(int track) {
      return this.rotates[track];
   }

   public boolean moves(int track) {
      return this.moves[track];
   }

   public boolean scales(int track) {
      return this.scales[track];
   }

   public static final class Channel {
      final int n;
      final double[] cumulative;
      final double[] length;
      final boolean[] constStart;
      final boolean[] constEnd;
      final double[] valueStart;
      final double[] valueEnd;
      final IValue[] exprStart;
      final IValue[] exprEnd;
      final AzKeyframe<IValue>[] frames;
      final Easings.Curve[] curve;
      final boolean monotone;

      Channel(List<AzKeyframe<IValue>> keyframes) {
         this.n = keyframes.size();
         this.cumulative = new double[this.n];
         this.length = new double[this.n];
         this.constStart = new boolean[this.n];
         this.constEnd = new boolean[this.n];
         this.valueStart = new double[this.n];
         this.valueEnd = new double[this.n];
         this.exprStart = new IValue[this.n];
         this.exprEnd = new IValue[this.n];
         this.frames = new AzKeyframe[this.n];
         this.curve = new Easings.Curve[this.n];
         double total = 0.0;
         boolean rising = true;

         for (int i = 0; i < this.n; i++) {
            AzKeyframe<IValue> frame = keyframes.get(i);
            this.frames[i] = frame;
            double len = frame.length();
            if (len < 0.0) {
               rising = false;
            }

            total += len;
            this.length[i] = len;
            this.cumulative[i] = total;
            IValue start = frame.startValue();
            IValue end = frame.endValue();
            this.constStart[i] = start instanceof Constant;
            this.constEnd[i] = end instanceof Constant;
            if (this.constStart[i]) {
               this.valueStart[i] = start.get();
            } else {
               this.exprStart[i] = start;
            }

            if (this.constEnd[i]) {
               this.valueEnd[i] = end.get();
            } else {
               this.exprEnd[i] = end;
            }

            this.curve[i] = frame.easingArgs().isEmpty() ? Easings.curveFor(frame.easingType()) : null;
         }

         this.monotone = rising;
      }

      public int size() {
         return this.n;
      }

      int locate(double tick) {
         if (this.n == 0) {
            return -1;
         }

         int lo = 0;
         int hi = this.n - 1;
         int result = this.n - 1;

         while (lo <= hi) {
            int mid = lo + hi >>> 1;
            if (this.cumulative[mid] > tick) {
               result = mid;
               hi = mid - 1;
            } else {
               lo = mid + 1;
            }
         }

         return result;
      }

      double startTick(int frame, double tick) {
         return frame == 0 ? tick : tick - this.cumulative[frame - 1];
      }
   }
}

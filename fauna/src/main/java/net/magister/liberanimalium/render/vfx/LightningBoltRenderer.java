package net.magister.liberanimalium.render.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.magister.liberanimalium.compat.Verts;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class LightningBoltRenderer {
   private static final Map<UUID, LightningBoltRenderer.LightningStrike> ACTIVE_STRIKES = new ConcurrentHashMap<>();
   private static final int STRIKE_LIFESPAN = 2;

   public static void forget(UUID ownerUUID) {
      ACTIVE_STRIKES.remove(ownerUUID);
   }

   public static void renderLightningTargeting(
      UUID ownerUUID,
      PoseStack poseStack,
      MultiBufferSource buffer,
      Vec3 startWorld,
      Vec3 endWorld,
      Vec3 renderOriginWorld,
      float[] innerColor,
      float[] outerColor,
      long currentTick
   ) {
      LightningBoltRenderer.LightningStrike strike = ACTIVE_STRIKES.get(ownerUUID);
      if (strike != null && currentTick - strike.birthTick <= 2L && !strike.segments.isEmpty()) {
         Vec3 currentStart = strike.segments.get(0).start;
         Vec3 currentEnd = null;

         for (int i = strike.segments.size() - 1; i >= 0; i--) {
            if (!strike.segments.get(i).isBranch) {
               currentEnd = strike.segments.get(i).end;
               break;
            }
         }

         if (currentEnd == null) {
            currentEnd = strike.segments.get(strike.segments.size() - 1).end;
         }

         Vec3 startDiff = startWorld.subtract(currentStart);
         Vec3 endDiff = endWorld.subtract(currentEnd);

         for (int i = 0; i < strike.segments.size(); i++) {
            LightningBoltRenderer.LightningSegment seg = strike.segments.get(i);
            double progressStart = (double)i / strike.segments.size();
            double progressEnd = (double)(i + 1) / strike.segments.size();
            Vec3 startOffset = startDiff.scale(1.0 - progressStart).add(endDiff.scale(progressStart));
            Vec3 endOffset = startDiff.scale(1.0 - progressEnd).add(endDiff.scale(progressEnd));
            seg.start = seg.start.add(startOffset);
            seg.end = seg.end.add(endOffset);
         }
      } else {
         strike = new LightningBoltRenderer.LightningStrike(startWorld, endWorld, currentTick);
         ACTIVE_STRIKES.put(ownerUUID, strike);
      }

      VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
      Matrix4f matrix = poseStack.last().pose();
      float baseWidth = 0.3F;
      float[] transparentOuter = new float[]{outerColor[0], outerColor[1], outerColor[2], outerColor[3] * 0.35F};
      LightningBoltRenderer.QuadCache cache = null;

      for (LightningBoltRenderer.LightningSegment segment : strike.segments) {
         float width = baseWidth * 1.0F * segment.widthScale;
         cache = drawSegmentQuad(matrix, consumer, segment.start, segment.end, width, transparentOuter, renderOriginWorld, cache);
      }

      cache = null;

      for (LightningBoltRenderer.LightningSegment segment : strike.segments) {
         float width = baseWidth * 0.4F * segment.widthScale;
         cache = drawSegmentQuad(matrix, consumer, segment.start, segment.end, width, innerColor, renderOriginWorld, cache);
      }
   }

   private static LightningBoltRenderer.QuadCache drawSegmentQuad(
      Matrix4f matrix,
      VertexConsumer consumer,
      Vec3 startWorld,
      Vec3 endWorld,
      float width,
      float[] color,
      Vec3 renderOriginWorld,
      LightningBoltRenderer.QuadCache cache
   ) {
      Vec3 start = startWorld.subtract(renderOriginWorld);
      Vec3 end = endWorld.subtract(renderOriginWorld);
      float r = color[0];
      float g = color[1];
      float b = color[2];
      float a = color[3];
      Vec3 segmentDir = end.subtract(start);
      if (segmentDir.lengthSqr() < 1.0E-4) {
         return cache;
      }

      segmentDir = segmentDir.normalize();
      Vec3 upRef = new Vec3(0.0, 1.0, 0.0);
      if (Math.abs(segmentDir.dot(upRef)) > 0.99) {
         upRef = new Vec3(1.0, 0.0, 0.0);
      }

      Vec3 right = segmentDir.cross(upRef).normalize().scale(width);
      Vec3 up = segmentDir.cross(right).normalize().scale(width);
      boolean useCache = cache != null && cache.endPos.distanceToSqr(startWorld) < 1.0E-4;
      Vec3 s1;
      Vec3 s2;
      Vec3 s3;
      Vec3 s4;
      if (useCache) {
         s1 = cache.e1;
         s2 = cache.e2;
         s3 = cache.e3;
         s4 = cache.e4;
      } else {
         s1 = start.add(right).add(up);
         s2 = start.add(right).subtract(up);
         s3 = start.subtract(right).subtract(up);
         s4 = start.subtract(right).add(up);
      }

      Vec3 e1 = end.add(right).add(up);
      Vec3 e2 = end.add(right).subtract(up);
      Vec3 e3 = end.subtract(right).subtract(up);
      Vec3 e4 = end.subtract(right).add(up);
      addVertex(matrix, consumer, s1.x, s1.y, s1.z, r, g, b, a);
      addVertex(matrix, consumer, e1.x, e1.y, e1.z, r, g, b, a);
      addVertex(matrix, consumer, e2.x, e2.y, e2.z, r, g, b, a);
      addVertex(matrix, consumer, s2.x, s2.y, s2.z, r, g, b, a);
      addVertex(matrix, consumer, s2.x, s2.y, s2.z, r, g, b, a);
      addVertex(matrix, consumer, e2.x, e2.y, e2.z, r, g, b, a);
      addVertex(matrix, consumer, e3.x, e3.y, e3.z, r, g, b, a);
      addVertex(matrix, consumer, s3.x, s3.y, s3.z, r, g, b, a);
      addVertex(matrix, consumer, s3.x, s3.y, s3.z, r, g, b, a);
      addVertex(matrix, consumer, e3.x, e3.y, e3.z, r, g, b, a);
      addVertex(matrix, consumer, e4.x, e4.y, e4.z, r, g, b, a);
      addVertex(matrix, consumer, s4.x, s4.y, s4.z, r, g, b, a);
      addVertex(matrix, consumer, s4.x, s4.y, s4.z, r, g, b, a);
      addVertex(matrix, consumer, e4.x, e4.y, e4.z, r, g, b, a);
      addVertex(matrix, consumer, e1.x, e1.y, e1.z, r, g, b, a);
      addVertex(matrix, consumer, s1.x, s1.y, s1.z, r, g, b, a);
      return new LightningBoltRenderer.QuadCache(endWorld, e1, e2, e3, e4);
   }

   private static void addVertex(Matrix4f matrix, VertexConsumer consumer, double x, double y, double z, float r, float g, float b, float a) {
      Verts.posColor(consumer, matrix, (float)x, (float)y, (float)z, r, g, b, a);
   }

   public static class LightningSegment {
      public Vec3 start;
      public Vec3 end;
      public final float widthScale;
      public final boolean isBranch;

      public LightningSegment(Vec3 start, Vec3 end, float widthScale, boolean isBranch) {
         this.start = start;
         this.end = end;
         this.widthScale = widthScale;
         this.isBranch = isBranch;
      }
   }

   public static class LightningStrike {
      public final List<LightningBoltRenderer.LightningSegment> segments = new ArrayList<>();
      public final long birthTick;

      public LightningStrike(Vec3 start, Vec3 end, long birthTick) {
         this.birthTick = birthTick;
         this.generateFractal(start, end);
      }

      private void generateFractal(Vec3 start, Vec3 end) {
         RandomSource random = RandomSource.create();
         List<LightningBoltRenderer.LightningSegment> initialQueue = new LinkedList<>();
         initialQueue.add(new LightningBoltRenderer.LightningSegment(start, end, 1.0F, false));
         int iterations = 5;
         double spreadFactor = 0.4;

         for (int i = 0; i < iterations; i++) {
            List<LightningBoltRenderer.LightningSegment> nextQueue = new LinkedList<>();

            for (LightningBoltRenderer.LightningSegment seg : initialQueue) {
               Vec3 diff = seg.end.subtract(seg.start);
               Vec3 mid = seg.start.add(diff.scale(0.5));
               double length = diff.length();
               Vec3 perp = this.findOrthogonal(diff, random);
               double displacementAmt = length * spreadFactor * (random.nextDouble() - 0.5);
               mid = mid.add(perp.scale(displacementAmt));
               LightningBoltRenderer.LightningSegment seg1 = new LightningBoltRenderer.LightningSegment(seg.start, mid, seg.widthScale, seg.isBranch);
               LightningBoltRenderer.LightningSegment seg2 = new LightningBoltRenderer.LightningSegment(mid, seg.end, seg.widthScale, seg.isBranch);
               nextQueue.add(seg1);
               nextQueue.add(seg2);
               if (!seg.isBranch && i > 0 && i < iterations - 1 && random.nextFloat() < 0.25F) {
                  Vec3 branchDir = diff.normalize().add(this.findOrthogonal(diff, random).scale(1.2)).normalize();
                  Vec3 branchEnd = mid.add(branchDir.scale(length * 0.6));
                  nextQueue.add(new LightningBoltRenderer.LightningSegment(mid, branchEnd, seg.widthScale * 0.5F, true));
               }
            }

            initialQueue = nextQueue;
            spreadFactor *= 0.5;
         }

         this.segments.addAll(initialQueue);
      }

      private Vec3 findOrthogonal(Vec3 vec, RandomSource random) {
         if (vec.lengthSqr() < 0.001) {
            return new Vec3(0.0, 1.0, 0.0);
         }

         Vec3 crossCandidate = new Vec3(-0.5 + random.nextDouble(), -0.5 + random.nextDouble(), -0.5 + random.nextDouble());
         return vec.cross(crossCandidate).normalize();
      }
   }

   public static class QuadCache {
      public final Vec3 endPos;
      public final Vec3 e1;
      public final Vec3 e2;
      public final Vec3 e3;
      public final Vec3 e4;

      public QuadCache(Vec3 endPos, Vec3 e1, Vec3 e2, Vec3 e3, Vec3 e4) {
         this.endPos = endPos;
         this.e1 = e1;
         this.e2 = e2;
         this.e3 = e3;
         this.e4 = e4;
      }
   }
}

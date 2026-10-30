package net.magister.liberanimalium.body.anim.data;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.attack.runtime.MuzzleAnchor;
import net.magister.liberanimalium.body.part.FaunaPartEntity;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.render.BonePalettes;
import net.minecraft.world.phys.Vec3;

public class PerEntityHeadMatrixExtractor {
   private static final Map<Integer, PerEntityHeadMatrixExtractor.HeadPositionData> headPositionCache = new ConcurrentHashMap<>();
   private static long lastCleanupTime = 0L;
   private static final long CLEANUP_INTERVAL = 200L;
   private static final long MAX_CACHE_AGE = 100L;

   public static void extractAndStore(FaunaBase body, AzBone headBone, @Nullable AzBone secondHeadBone, float partialTick) {
      long currentTime = body.level().getGameTime();
      Vec3 primaryHeadPos = extractBoneWorldPosition(body, headBone, partialTick);
      if (primaryHeadPos != null) {
         Vec3 secondaryHeadPos = null;
         if (secondHeadBone != null) {
            secondaryHeadPos = extractBoneWorldPosition(body, secondHeadBone, partialTick);
         }

         PerEntityHeadMatrixExtractor.HeadPositionData previous = headPositionCache.get(body.getId());
         Vec3 carriedMouth = previous != null && previous.isValid(currentTime) ? previous.mouthPosition : null;
         PerEntityHeadMatrixExtractor.HeadPositionData data = new PerEntityHeadMatrixExtractor.HeadPositionData(
            body.getId(), primaryHeadPos, secondaryHeadPos, carriedMouth, false, currentTime
         );
         headPositionCache.put(body.getId(), data);
         performCacheCleanupIfNeeded(currentTime);
      }
   }

   public static void storeMouthPosition(int entityId, Vec3 mouthPosition, boolean exact, long currentTime) {
      PerEntityHeadMatrixExtractor.HeadPositionData existing = headPositionCache.get(entityId);
      if (existing != null) {
         boolean sameFrame = existing.extractionTime == currentTime;
         if (!sameFrame || existing.mouthPosition == null || existing.mouthExact || exact) {
            if (!sameFrame || !existing.mouthExact || exact) {
               headPositionCache.put(
                  entityId,
                  new PerEntityHeadMatrixExtractor.HeadPositionData(
                     entityId, existing.primaryHeadPosition, existing.secondaryHeadPosition, mouthPosition, exact, existing.extractionTime
                  )
               );
            }
         }
      }
   }

   @Nullable
   public static PerEntityHeadMatrixExtractor.HeadPositionData getHeadPositionData(int entityId, long currentTime) {
      PerEntityHeadMatrixExtractor.HeadPositionData data = headPositionCache.get(entityId);
      if (data == null) {
         return null;
      } else if (!data.isValid(currentTime)) {
         headPositionCache.remove(entityId);
         return null;
      } else {
         return data;
      }
   }

   @Nullable
   public static Vec3 resolveHeadPosition(FaunaBase body) {
      PerEntityHeadMatrixExtractor.HeadPositionData data = getHeadPositionData(body.getId(), body.level().getGameTime());
      if (data != null && data.primaryHeadPosition != null) {
         return data.primaryHeadPosition;
      }

      List<FaunaPartEntity> heads = body.headParts();
      return heads.isEmpty() ? null : heads.get(0).centre();
   }

   @Nullable
   public static Vec3 mouthPositionOrNull(FaunaBase body) {
      PerEntityHeadMatrixExtractor.HeadPositionData data = getHeadPositionData(body.getId(), body.level().getGameTime());
      return data != null && data.mouthPosition != null ? data.mouthPosition : body.trackedMouth();
   }

   public static Vec3 resolveMouthPosition(FaunaBase body) {
      Vec3 resolved = MuzzleAnchor.resolve(() -> mouthPositionOrNull(body), () -> resolveHeadPosition(body), body::getEyePosition);
      return resolved != null ? resolved : body.getEyePosition();
   }

   public static String mouthSource(FaunaBase body) {
      return MuzzleAnchor.source(mouthPositionOrNull(body), resolveHeadPosition(body));
   }

   @Nullable
   public static Vec3 extractBoneWorldPosition(FaunaBase body, AzBone bone, float partialTick) {
      if (bone == null) {
         return null;
      }

      Vec3 drawnPos = body.getPosition(partialTick);
      return BonePalettes.worldPosition(body.getId(), bone.getName(), drawnPos);
   }

   private static void performCacheCleanupIfNeeded(long currentTime) {
      if (currentTime - lastCleanupTime >= 200L) {
         lastCleanupTime = currentTime;

         for (Entry<Integer, PerEntityHeadMatrixExtractor.HeadPositionData> entry : headPositionCache.entrySet()) {
            if (currentTime - entry.getValue().extractionTime > 100L) {
               headPositionCache.remove(entry.getKey());
            }
         }
      }
   }

   public static void clearCache() {
      headPositionCache.clear();
   }

   public static int getCacheSize() {
      return headPositionCache.size();
   }

   public static boolean hasDataFor(int entityId) {
      return headPositionCache.containsKey(entityId);
   }

   public static class HeadPositionData {
      public final int entityId;
      public final Vec3 primaryHeadPosition;
      public final Vec3 secondaryHeadPosition;
      public final Vec3 mouthPosition;
      public final boolean mouthExact;
      public final long extractionTime;

      public HeadPositionData(int entityId, Vec3 primaryHeadPosition, Vec3 secondaryHeadPosition, Vec3 mouthPosition, boolean mouthExact, long extractionTime) {
         this.entityId = entityId;
         this.primaryHeadPosition = primaryHeadPosition;
         this.secondaryHeadPosition = secondaryHeadPosition;
         this.mouthPosition = mouthPosition;
         this.mouthExact = mouthExact;
         this.extractionTime = extractionTime;
      }

      public boolean isValid(long currentTime) {
         return currentTime - this.extractionTime < 10L;
      }
   }
}

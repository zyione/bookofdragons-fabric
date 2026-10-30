package net.magister.liberanimalium.body.anim.extraction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.render.BonePalettes;
import net.minecraft.world.phys.Vec3;

public class PerEntityWingBoneExtractor {
   private static final Map<Integer, PerEntityWingBoneExtractor.WingBoneData> wingDataCache = new ConcurrentHashMap<>();
   private static final Map<Integer, PerEntityWingBoneExtractor.WingBoneData> previousDataCache = new ConcurrentHashMap<>();
   private static long lastCleanupTime = 0L;
   private static final long CLEANUP_INTERVAL = 200L;
   private static final long MAX_CACHE_AGE = 100L;

   public static void extractAndStore(int entityId, String entityName, AzBone leftWingBone, AzBone rightWingBone, long currentTime) {
      if (leftWingBone != null || rightWingBone != null) {
         float leftRotY = leftWingBone == null ? 0.0F : leftWingBone.getRotY();
         float rightRotY = rightWingBone == null ? 0.0F : rightWingBone.getRotY();
         store(entityId, leftRotY, rightRotY, currentTime);
      }
   }

   static void store(int entityId, float leftRotY, float rightRotY, long currentTime) {
      PerEntityWingBoneExtractor.WingBoneData existingCurrent = wingDataCache.get(entityId);
      if (existingCurrent != null && existingCurrent.timestamp < currentTime) {
         previousDataCache.put(entityId, existingCurrent);
      }

      float leftVelocityY = 0.0F;
      float rightVelocityY = 0.0F;
      PerEntityWingBoneExtractor.WingBoneData previousData = previousDataCache.get(entityId);
      if (previousData != null) {
         long gap = currentTime - previousData.timestamp;
         leftVelocityY = velocityOverGap(leftRotY, previousData.leftRotationY, gap);
         rightVelocityY = velocityOverGap(rightRotY, previousData.rightRotationY, gap);
      }

      wingDataCache.put(entityId, new PerEntityWingBoneExtractor.WingBoneData(entityId, leftRotY, rightRotY, leftVelocityY, rightVelocityY, currentTime));
      performCacheCleanupIfNeeded(currentTime);
   }

   static float velocityOverGap(float now, float before, long gap) {
      return gap >= 1L && gap <= 5L ? (now - before) / (float)gap : 0.0F;
   }

   @Nullable
   public static PerEntityWingBoneExtractor.WingBoneData getWingBoneData(int entityId, long currentTime) {
      PerEntityWingBoneExtractor.WingBoneData data = wingDataCache.get(entityId);
      if (data == null) {
         return null;
      } else if (!data.isValid(currentTime)) {
         wingDataCache.remove(entityId);
         previousDataCache.remove(entityId);
         return null;
      } else {
         return data;
      }
   }

   private static void performCacheCleanupIfNeeded(long currentTime) {
      if (currentTime - lastCleanupTime >= 200L) {
         lastCleanupTime = currentTime;
         wingDataCache.entrySet().removeIf(entry -> currentTime - entry.getValue().timestamp > 100L);
         previousDataCache.entrySet().removeIf(entry -> currentTime - entry.getValue().timestamp > 100L);
      }
   }

   public static void clearCache() {
      wingDataCache.clear();
      previousDataCache.clear();
   }

   public static int getCacheSize() {
      return wingDataCache.size();
   }

   public static boolean hasDataFor(int entityId) {
      return wingDataCache.containsKey(entityId);
   }

   @Nullable
   public static PerEntityWingBoneExtractor.WingTipData extractWorldTipPositions(FaunaBase body, AzBakedModel model, float partialTick) {
      AzBone leftTip = tipBone(model, "L");
      AzBone rightTip = tipBone(model, "R");
      if (leftTip != null && rightTip != null) {
         Vec3 bodyAt = body.position();
         Vec3 left = BonePalettes.worldPosition(body.getId(), leftTip.getName(), bodyAt);
         Vec3 right = BonePalettes.worldPosition(body.getId(), rightTip.getName(), bodyAt);
         return left != null && right != null ? new PerEntityWingBoneExtractor.WingTipData(left, right) : null;
      } else {
         return null;
      }
   }

   @Nullable
   static AzBone tipBone(AzBakedModel model, String side) {
      AzBone membrane = model.getBoneOrNull(side + "_wing_fingertip_membrane");
      return membrane != null ? membrane : model.getBoneOrNull(side + "_wing_fingertip");
   }

   public static class WingBoneData {
      public final int entityId;
      public final float leftRotationY;
      public final float rightRotationY;
      public final float leftAngularVelocityY;
      public final float rightAngularVelocityY;
      public final long timestamp;

      public WingBoneData(int entityId, float leftRotY, float rightRotY, float leftVelY, float rightVelY, long timestamp) {
         this.entityId = entityId;
         this.leftRotationY = leftRotY;
         this.rightRotationY = rightRotY;
         this.leftAngularVelocityY = leftVelY;
         this.rightAngularVelocityY = rightVelY;
         this.timestamp = timestamp;
      }

      public boolean isValid(long currentTime) {
         return currentTime - this.timestamp < 10L;
      }

      public PerEntityWingBoneExtractor.WingData getWingData(String side) {
         return side.equals("left")
            ? new PerEntityWingBoneExtractor.WingData(this.leftRotationY, this.leftAngularVelocityY)
            : new PerEntityWingBoneExtractor.WingData(this.rightRotationY, this.rightAngularVelocityY);
      }
   }

   public static class WingData {
      public final float rotationY;
      public final float angularVelocityY;

      public WingData(float rotY, float velY) {
         this.rotationY = rotY;
         this.angularVelocityY = velY;
      }
   }

   public record WingTipData(Vec3 leftTip, Vec3 rightTip) {
   }
}

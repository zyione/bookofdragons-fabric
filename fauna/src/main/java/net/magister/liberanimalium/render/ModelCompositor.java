package net.magister.liberanimalium.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.minecraft.resources.ResourceLocation;

public class ModelCompositor {
   private static final Map<String, AzBakedModel> COMPOSITE_CACHE = new ConcurrentHashMap<>();

   public static AzBakedModel getOrComposite(ResourceLocation baseId, AzBakedModel baseModel, ResourceLocation attachmentId, AzBakedModel attachmentModel) {
      String cacheKey = baseId.toString() + ":" + attachmentId.toString();
      return COMPOSITE_CACHE.computeIfAbsent(cacheKey, k -> composite(baseModel, attachmentModel));
   }

   private static AzBakedModel composite(AzBakedModel base, AzBakedModel attachment) {
      AzBakedModel result = base.deepCopy();

      for (AzBone attachTopBone : attachment.getTopLevelBones()) {
         graftRecursively(result, attachTopBone);
      }

      return new AzBakedModel(result.getTopLevelBones(), base.getModelUUID());
   }

   private static void graftRecursively(AzBakedModel targetModel, AzBone sourceBone) {
      AzBone targetBone = targetModel.getBoneOrNull(sourceBone.getName());
      if (targetBone != null) {
         targetBone.getCubes().addAll(sourceBone.getCubes());

         for (AzBone sourceChild : sourceBone.getChildBones()) {
            if (targetModel.getBoneOrNull(sourceChild.getName()) != null) {
               graftRecursively(targetModel, sourceChild);
            } else {
               targetBone.getChildBones().add(sourceChild.deepCopy());
            }
         }
      }
   }

   public static void clearCache() {
      COMPOSITE_CACHE.clear();
   }
}

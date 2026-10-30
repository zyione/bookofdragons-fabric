package net.magister.liberanimalium.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.layer.AzRenderLayer;
import net.magister.liberanimalium.anim.rig.CarrySpec;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class CarriedItemLayer<T extends FaunaBase> implements AzRenderLayer<UUID, T>, BoneAudience {
   public static volatile CarrySpec preview;
   public static volatile String previewSpecies;

   public void preRender(AzRendererPipelineContext<UUID, T> context) {
   }

   public void render(AzRendererPipelineContext<UUID, T> context) {
   }

   public static boolean isHead(String boneName) {
      return boneName.equals("head") || boneName.contains("head0");
   }

   public static boolean isHead(FaunaBase body, String boneName) {
      List<String> named = body.animRig().bones().head();
      return named.isEmpty() ? isHead(boneName) : named.contains(boneName);
   }

   public static String headKey(FaunaBase body) {
      List<String> named = body.animRig().bones().head();
      return named.isEmpty() ? "head" : "head:" + String.join(",", named);
   }

   public static CarrySpec carryOf(FaunaBase body) {
      CarrySpec live = preview;
      return live != null && body.speciesId().equals(previewSpecies) ? live : body.animRig().carry();
   }

   @Override
   public void wantedBones(AzRendererPipelineContext<UUID, ?> context, Set<String> into) {
      CarrySpec carry = context.animatable() instanceof FaunaBase body ? carryOf(body) : CarrySpec.DEFAULT;
      if (carry.bone() == null) {
         if (context.animatable() instanceof FaunaBase body) {
            into.addAll(BoneNames.matching(context.bakedModel(), headKey(body), name -> isHead(body, name)));
         } else {
            into.addAll(BoneNames.matching(context.bakedModel(), "head", CarriedItemLayer::isHead));
         }
      } else {
         into.addAll(BoneNames.matching(context.bakedModel(), carry.bone(), carry.bone()::equals));
      }
   }

   public void renderForBone(@NotNull AzRendererPipelineContext<UUID, T> context, @NotNull AzBone bone) {
      T body = (T)context.animatable();
      CarrySpec carry = carryOf(body);
      String boneName = bone.getName();
      if (carry.bone() == null ? isHead(body, boneName) : carry.bone().equals(boneName)) {
         ItemStack carried = body.getCarriedItem();
         if (!carried.isEmpty()) {
            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
            poseStack.translate((float)carry.offset().x / 16.0F, (float)carry.offset().y / 16.0F, (float)carry.offset().z / 16.0F);
            if (carry.rotation().x != 0.0) {
               poseStack.mulPose(Axis.XP.rotationDegrees((float)carry.rotation().x));
            }

            if (carry.rotation().y != 0.0) {
               poseStack.mulPose(Axis.YP.rotationDegrees((float)carry.rotation().y));
            }

            if (carry.rotation().z != 0.0) {
               poseStack.mulPose(Axis.ZP.rotationDegrees((float)carry.rotation().z));
            }

            Vector3f scaled = poseStack.last().pose().getScale(new Vector3f());
            float sx = scaled.x() > 1.0E-4F ? carry.scale() / scaled.x() : carry.scale();
            float sy = scaled.y() > 1.0E-4F ? carry.scale() / scaled.y() : carry.scale();
            float sz = scaled.z() > 1.0E-4F ? carry.scale() / scaled.z() : carry.scale();
            poseStack.scale(sx, sy, sz);
            MultiBufferSource buffers = context.multiBufferSource();
            ItemRenderer items = Minecraft.getInstance().getItemRenderer();
            items.renderStatic(
               carried, ItemDisplayContext.GROUND, context.packedLight(), OverlayTexture.NO_OVERLAY, poseStack, buffers, body.level(), body.getId()
            );
            poseStack.popPose();
         }
      }
   }
}

package net.magister.liberanimalium.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.layer.AzRenderLayer;
import net.magister.liberanimalium.attack.client.render.BlendMode;
import net.magister.liberanimalium.attack.client.render.QuadSink;
import net.magister.liberanimalium.attack.client.render.RenderInput;
import net.magister.liberanimalium.attack.client.render.builtin.PixelQuadStrategy;
import net.magister.liberanimalium.attack.data.AttackDefinition;
import net.magister.liberanimalium.attack.data.MuzzleConfig;
import net.magister.liberanimalium.attack.data.color.PaletteLUT;
import net.magister.liberanimalium.attack.runtime.MuzzleAnchor;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.magister.liberanimalium.compat.AzColor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MuzzleGlowLayer<T extends FaunaBase> implements AzRenderLayer<UUID, T>, BoneAudience {
   private final ThreadLocal<MuzzleGlowLayer.Frame> frame = new ThreadLocal<>();

   public void preRender(AzRendererPipelineContext<UUID, T> context) {
      this.frame.set(null);
      T body = (T)context.animatable();
      OmniAttackHandler breath = body.componentRegistry.get(OmniAttackHandler.class);
      AttackDefinition def = breath != null ? breath.definition() : null;
      if (def != null) {
         float glow = breath.phaseClock().glow();
         if (!(glow <= 0.0F)) {
            MuzzleConfig muzzle = def.muzzle();
            if (!muzzle.glow().isEmpty() || muzzle.hasTextureGlow()) {
               this.frame.set(new MuzzleGlowLayer.Frame(muzzle, glow));
            }
         }
      }
   }

   @Override
   public void wantedBones(AzRendererPipelineContext<UUID, ?> context, Set<String> into) {
      MuzzleGlowLayer.Frame f = this.frame.get();
      if (f != null && !f.muzzle.glow().isEmpty()) {
         String wanted = f.muzzle.bone();
         into.addAll(BoneNames.matching(context.bakedModel(), "muzzle:" + wanted, name -> MuzzleAnchor.matches(name, wanted)));
      }
   }

   public void renderForBone(@NotNull AzRendererPipelineContext<UUID, T> context, @NotNull AzBone bone) {
      MuzzleGlowLayer.Frame f = this.frame.get();
      if (f != null && !f.muzzle.glow().isEmpty()) {
         String wanted = f.muzzle.bone();
         if (MuzzleAnchor.matches(bone.getName(), wanted)) {
            boolean exact = MuzzleAnchor.isExact(bone.getName(), wanted);
            if (f.anchorPose == null || !f.anchorExact && exact) {
               PoseStack poseStack = context.poseStack();
               poseStack.pushPose();
               poseStack.translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
               poseStack.translate(f.muzzle.offsetX(), f.muzzle.offsetY(), f.muzzle.offsetZ());
               f.anchorPose = new Matrix4f(poseStack.last().pose());
               f.anchorExact = exact;
               poseStack.popPose();
            }
         }
      }
   }

   public void render(AzRendererPipelineContext<UUID, T> context) {
      MuzzleGlowLayer.Frame f = this.frame.get();
      if (f != null) {
         if (f.muzzle.hasTextureGlow()) {
            this.drawOverlay(context, f);
         }

         if (f.anchorPose != null && f.muzzle.glow().isPresent()) {
            this.drawSquares(context, f);
         }
      }
   }

   private void drawOverlay(AzRendererPipelineContext<UUID, T> context, MuzzleGlowLayer.Frame f) {
      RenderType renderType = RenderType.entityTranslucentEmissive(f.muzzle.glowTexture().get());
      RenderType prevType = context.renderType();
      VertexConsumer prevBuffer = context.vertexConsumer();
      int prevLight = context.packedLight();
      float pr = AzColor.r(context);
      float pg = AzColor.g(context);
      float pb = AzColor.b(context);
      float pa = AzColor.a(context);
      AzColor.set(context, 1.0F, 1.0F, 1.0F, f.muzzle.glowAlphaAt(f.glow));
      context.setRenderType(renderType);
      context.setVertexConsumer(context.multiBufferSource().getBuffer(renderType));
      context.setPackedLight(15728880);
      context.rendererPipeline().reRender(context);
      context.setRenderType(prevType);
      context.setVertexConsumer(prevBuffer);
      context.setPackedLight(prevLight);
      AzColor.set(context, pr, pg, pb, pa);
   }

   private void drawSquares(final AzRendererPipelineContext<UUID, T> context, MuzzleGlowLayer.Frame f) {
      MuzzleConfig.Glow glow = f.muzzle.glow().get();
      Vector3f view = f.anchorPose.transformPosition(new Vector3f(0.0F, 0.0F, 0.0F));
      final PoseStack flat = new PoseStack();
      flat.translate(view.x(), view.y(), view.z());
      QuadSink sink = new QuadSink() {
         @Override
         public VertexConsumer getBuffer(RenderType type) {
            return context.multiBufferSource().getBuffer(type);
         }

         @Override
         public PoseStack poseStack() {
            return flat;
         }

         @Override
         public MultiBufferSource multiBufferSource() {
            return context.multiBufferSource();
         }
      };
      List<PaletteLUT.ColorBand> bands = glow.palette().bands();
      int n = bands.size();
      float outer = glow.sizeAt(f.glow);
      int packed = 15728880;
      int uLight = packed & 65535;
      int vLight = packed >> 16 & 65535;

      for (int k = n - 1; k >= 0; k--) {
         PaletteLUT.ColorBand band = bands.get(k);
         float size = outer * (k + 1) / n;
         float r = band.color().size() >= 3 ? band.color().get(0).intValue() / 255.0F : 1.0F;
         float g = band.color().size() >= 3 ? band.color().get(1).intValue() / 255.0F : 1.0F;
         float b = band.color().size() >= 3 ? band.color().get(2).intValue() / 255.0F : 1.0F;
         RenderInput input = new RenderInput(
            0.0F,
            0.0F,
            0.0F,
            size,
            1.0F,
            r,
            g,
            b,
            band.alpha() * f.glow,
            new Quaternionf(),
            BlendMode.ADDITIVE,
            uLight,
            vLight,
            null,
            null,
            new float[0],
            new float[0],
            ((FaunaBase)context.animatable()).getUUID(),
            ((FaunaBase)context.animatable()).level().getGameTime()
         );
         PixelQuadStrategy.INSTANCE.emit(sink, input);
      }
   }

   private static final class Frame {
      final MuzzleConfig muzzle;
      final float glow;
      Matrix4f anchorPose;
      boolean anchorExact;

      Frame(MuzzleConfig muzzle, float glow) {
         this.muzzle = muzzle;
         this.glow = glow;
      }
   }
}

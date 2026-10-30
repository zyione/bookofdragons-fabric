package net.magister.liberanimalium.attack.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.magister.liberanimalium.attack.client.FireColorModel;
import net.magister.liberanimalium.attack.client.ParticleSim;
import net.magister.liberanimalium.attack.client.render.builtin.BeamStrategy;
import net.magister.liberanimalium.attack.client.render.builtin.BillboardStrategy;
import net.magister.liberanimalium.attack.client.render.builtin.CubeStrategy;
import net.magister.liberanimalium.attack.client.render.builtin.ModelBodyRenderer;
import net.magister.liberanimalium.attack.client.render.builtin.PixelQuadStrategy;
import net.magister.liberanimalium.attack.data.EmitterSim;
import net.magister.liberanimalium.attack.data.ProjectileEmitter;
import net.magister.liberanimalium.attack.data.color.PaletteLUT;
import net.magister.liberanimalium.attack.data.render.BeamRender;
import net.magister.liberanimalium.attack.data.render.BillboardRender;
import net.magister.liberanimalium.attack.data.render.CubeRender;
import net.magister.liberanimalium.attack.data.render.ModelRender;
import net.magister.liberanimalium.attack.data.render.PixelQuadRender;
import net.magister.liberanimalium.attack.entity.AttackProjectileEntity;
import net.magister.liberanimalium.body.anim.data.PerEntityHeadMatrixExtractor;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.compat.ClientFrame;
import net.magister.liberanimalium.render.gpu.FaunaShaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class ProjectileRenderer extends EntityRenderer<AttackProjectileEntity> {
   private final Context context;

   public ProjectileRenderer(Context ctx) {
      super(ctx);
      this.context = ctx;
   }

   public ResourceLocation getTextureLocation(AttackProjectileEntity entity) {
      return null;
   }

   public void render(
      AttackProjectileEntity entity, float pEntityYaw, float pPartialTicks, final PoseStack pPoseStack, final MultiBufferSource pBuffer, int pPackedLight
   ) {
      if (!entity.getAttackId().isEmpty()) {
         ProjectileEmitter emitter = entity.getProjectileEmitter();
         if (emitter != null) {
            EmitterSim sim = emitter.sim();
            if (emitter.render() instanceof ModelRender model) {
               ModelBodyRenderer.of(model, this.context).render(entity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
            } else {
               QuadSink sink = new QuadSink() {
                  @Override
                  public VertexConsumer getBuffer(RenderType type) {
                     return pBuffer.getBuffer(type);
                  }

                  @Override
                  public PoseStack poseStack() {
                     return pPoseStack;
                  }

                  @Override
                  public MultiBufferSource multiBufferSource() {
                     return pBuffer;
                  }
               };
               if (emitter.render() instanceof CubeRender cfg) {
                  this.renderCube(entity, cfg, sim, pEntityYaw, pPartialTicks, sink);
               } else if (emitter.render() instanceof BeamRender cfg) {
                  this.renderBeam(entity, cfg, pPartialTicks, sink);
               } else if (emitter.render() instanceof BillboardRender cfg) {
                  this.renderBillboard(entity, cfg, sim, pPartialTicks, sink);
               } else if (emitter.render() instanceof PixelQuadRender cfg) {
                  this.renderPixelQuad(entity, cfg, sim, pEntityYaw, pPartialTicks, sink);
               }

               super.render(entity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
            }
         }
      }
   }

   private void renderCube(AttackProjectileEntity entity, CubeRender cfg, EmitterSim sim, float yaw, float pPartialTicks, QuadSink sink) {
      float chargeScale = entity.getChargeScale();
      float grow = ParticleSim.growScale(entity.getFlightTime(), sim.growInTicks());
      float size = lerp2(chargeScale, cfg.size(), 0.5F, 1.0F) * grow;
      float elongation = lerp2(chargeScale, cfg.elongation(), 1.0F, 1.0F);
      float r = cfg.color().size() >= 3 ? cfg.color().get(0).intValue() / 255.0F : 1.0F;
      float g = cfg.color().size() >= 3 ? cfg.color().get(1).intValue() / 255.0F : 1.0F;
      float b = cfg.color().size() >= 3 ? cfg.color().get(2).intValue() / 255.0F : 1.0F;
      if (cfg.pulseColor() && cfg.endColor().size() >= 3) {
         float pulse = (float)(Math.sin(entity.level().getGameTime() * 0.5) * 0.5 + 0.5);
         float er = cfg.endColor().get(0).intValue() / 255.0F;
         float eg = cfg.endColor().get(1).intValue() / 255.0F;
         float eb = cfg.endColor().get(2).intValue() / 255.0F;
         float[] hsv1 = CubeStrategy.rgbToHsv(r, g, b);
         float[] hsv2 = CubeStrategy.rgbToHsv(er, eg, eb);
         float h = CubeStrategy.lerpHue(hsv1[0], hsv2[0], pulse);
         float s = Mth.lerp(pulse, hsv1[1], hsv2[1]);
         float v = Mth.lerp(pulse, hsv1[2], hsv2[2]);
         float[] rgb = CubeStrategy.hsvToRgb(h, s, v);
         r = rgb[0];
         g = rgb[1];
         b = rgb[2];
      }

      float pitch = Mth.lerp(pPartialTicks, entity.xRotO, entity.getXRot());
      Quaternionf rotation = new Quaternionf().rotationY(yaw * (float) (Math.PI / 180.0)).rotateX(-pitch * (float) (Math.PI / 180.0));
      BlendMode blend = "additive".equals(cfg.blend()) ? BlendMode.ADDITIVE : BlendMode.ALPHA;
      int packed = 15728880;
      int uLight = packed & 65535;
      int vLight = packed >> 16 & 65535;
      RenderInput input = new RenderInput(
         0.0F,
         0.0F,
         0.0F,
         size,
         elongation,
         r,
         g,
         b,
         cfg.alpha(),
         rotation,
         blend,
         uLight,
         vLight,
         null,
         null,
         new float[0],
         new float[0],
         entity.getUUID(),
         entity.level().getGameTime()
      );
      CubeStrategy.INSTANCE.emit(sink, input);
   }

   private void renderBeam(AttackProjectileEntity entity, BeamRender cfg, float pPartialTicks, QuadSink sink) {
      Entity owner = entity.getOwner();
      Vec3 beamStart = null;
      if (owner instanceof FaunaBase body) {
         beamStart = PerEntityHeadMatrixExtractor.resolveHeadPosition(body);
      }

      if (beamStart == null) {
         beamStart = owner != null ? owner.getPosition(pPartialTicks).add(0.0, owner.getEyeHeight() * 0.8, 0.0) : entity.getPosition(pPartialTicks);
      }

      Vec3 beamEnd = entity.getPosition(pPartialTicks);
      float[] innerColor = toFloatColor(cfg.innerColor(), 1.0F);
      float[] outerColor = toFloatColor(cfg.outerColor(), 0.8F);
      RenderInput input = new RenderInput(
         0.0F,
         0.0F,
         0.0F,
         1.0F,
         1.0F,
         1.0F,
         1.0F,
         1.0F,
         1.0F,
         new Quaternionf(),
         BlendMode.ADDITIVE,
         0,
         0,
         beamStart,
         beamEnd,
         innerColor,
         outerColor,
         entity.getUUID(),
         entity.level().getGameTime()
      );
      BeamStrategy.INSTANCE.emit(sink, input);
   }

   private void renderBillboard(AttackProjectileEntity entity, BillboardRender cfg, EmitterSim sim, float pPartialTicks, QuadSink sink) {
      Quaternionf cameraRot = ClientFrame.billboard(Minecraft.getInstance().gameRenderer.getMainCamera());
      if (cfg.spin()) {
         cameraRot.rotateZ(entity.getId() * 1.7F + (entity.tickCount + pPartialTicks) * 0.15F);
      }

      int flightTime = entity.getFlightTime();
      float t = sim.lifetimeTicks() <= 0 ? 0.0F : Math.min(1.0F, (float)flightTime / sim.lifetimeTicks());
      int packed = cfg.litAt(t) ? 15728880 : LevelRenderer.getLightColor(entity.level(), entity.blockPosition());
      int uLight = packed & 65535;
      int vLight = packed >> 16 & 65535;
      float baseSize = cfg.size().isEmpty() ? 0.5F : cfg.size().get(0);
      float size = cfg.curves().size().isPresent() ? baseSize * cfg.curves().sizeAt(t, 1.0F) : baseSize * ParticleSim.growScale(flightTime, sim.growInTicks());
      float alpha = cfg.curves().alphaAt(t) * entity.getSlideAlpha() * (FaunaShaders.packActive() ? cfg.shaderAlpha() : 1.0F);
      BlendMode blend = "additive".equals(cfg.blend()) ? BlendMode.ADDITIVE : BlendMode.ALPHA;
      RenderInput input = new RenderInput(
         0.0F,
         0.0F,
         0.0F,
         entity.getChargeScale() * size,
         1.0F,
         1.0F,
         1.0F,
         1.0F,
         alpha,
         cameraRot,
         blend,
         uLight,
         vLight,
         null,
         null,
         new float[0],
         new float[0],
         entity.getUUID(),
         entity.level().getGameTime()
      );
      new BillboardStrategy(cfg.frameFile(cfg.frameAt(flightTime, sim.lifetimeTicks())), cfg.litAt(t)).emit(sink, input);
   }

   private void renderPixelQuad(AttackProjectileEntity entity, PixelQuadRender cfg, EmitterSim sim, float yaw, float pPartialTicks, QuadSink sink) {
      int flightTime = entity.getFlightTime();
      float temperature = Math.max(0.0F, sim.initialTemperature() - sim.coolingRate() * flightTime);
      PaletteLUT.ColorBand band = cfg.palette().map(p -> FireColorModel.sample(p, temperature)).orElse(new PaletteLUT.ColorBand(List.of(255, 255, 255), 1.0F));
      float r = band.color().size() >= 3 ? band.color().get(0).intValue() / 255.0F : 1.0F;
      float g = band.color().size() >= 3 ? band.color().get(1).intValue() / 255.0F : 1.0F;
      float b = band.color().size() >= 3 ? band.color().get(2).intValue() / 255.0F : 1.0F;
      float baseSize = cfg.size().isEmpty() ? 0.25F : cfg.size().get(0);
      float size = ParticleSim.shrinkSize(baseSize, sim, temperature) * ParticleSim.growScale(flightTime, sim.growInTicks());
      float spin = entity.getId() * 1.7F + (entity.tickCount + pPartialTicks) * 0.15F;
      Quaternionf rotation = ClientFrame.billboard(Minecraft.getInstance().gameRenderer.getMainCamera()).rotateZ(spin);
      int packed = cfg.emissive() ? 15728880 : LevelRenderer.getLightColor(entity.level(), entity.blockPosition());
      int uLight = packed & 65535;
      int vLight = packed >> 16 & 65535;
      BlendMode blend = "additive".equals(cfg.blend()) ? BlendMode.ADDITIVE : BlendMode.ALPHA;
      float alpha = band.alpha() * entity.getSlideAlpha() * (FaunaShaders.packActive() ? cfg.shaderAlpha() : 1.0F);
      RenderInput input = new RenderInput(
         0.0F,
         0.0F,
         0.0F,
         size,
         1.0F,
         r,
         g,
         b,
         alpha,
         rotation,
         blend,
         uLight,
         vLight,
         null,
         null,
         new float[0],
         new float[0],
         entity.getUUID(),
         entity.level().getGameTime()
      );
      PixelQuadStrategy.INSTANCE.emit(sink, input);
   }

   private static float lerp2(float t, List<Float> list, float defaultMin, float defaultMax) {
      float min = list.size() >= 1 ? list.get(0) : defaultMin;
      float max = list.size() >= 2 ? list.get(1) : min;
      return Mth.lerp(t, min, max);
   }

   private static float[] toFloatColor(List<Integer> rgb, float alpha) {
      return rgb.size() >= 3
         ? new float[]{rgb.get(0).intValue() / 255.0F, rgb.get(1).intValue() / 255.0F, rgb.get(2).intValue() / 255.0F, alpha}
         : new float[]{1.0F, 1.0F, 1.0F, alpha};
   }
}

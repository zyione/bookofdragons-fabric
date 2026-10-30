package net.magister.liberanimalium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.magister.liberanimalium.platform.event.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class RenderStageEventsMixin {
    @Shadow private Frustum cullingFrustum;
    @Shadow private Frustum capturedFrustum;
    @Unique private PoseStack liberanimalium$poses;
    @Unique private float liberanimalium$partial;
    @Unique private Camera liberanimalium$camera;
    @Unique private Matrix4f liberanimalium$projection;
    @Unique private void liberanimalium$stage(RenderStage stage) {
        StageContext event = new StageContext(stage, liberanimalium$poses, liberanimalium$projection, liberanimalium$partial,
            liberanimalium$camera, capturedFrustum != null ? capturedFrustum : cullingFrustum);
        ClientEvents.RENDER_STAGE.allEach(l -> l.accept(event));
    }
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void liberanimalium$begin(PoseStack poses, float partial, long deadline, boolean outlines, Camera camera, GameRenderer renderer, LightTexture light, Matrix4f projection, CallbackInfo ci) {
        liberanimalium$poses = poses; liberanimalium$partial = partial; liberanimalium$camera = camera; liberanimalium$projection = projection;
    }
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V", shift = At.Shift.AFTER))
    private void liberanimalium$sky(CallbackInfo ci) { liberanimalium$stage(RenderStage.AFTER_SKY); }
    @Inject(method = "renderChunkLayer", at = @At("RETURN"))
    private void liberanimalium$terrain(RenderType type, PoseStack poses, double x, double y, double z, Matrix4f projection, CallbackInfo ci) {
        if (type == RenderType.solid()) liberanimalium$stage(RenderStage.AFTER_SOLID_BLOCKS);
        else if (type == RenderType.cutoutMipped()) liberanimalium$stage(RenderStage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS);
        else if (type == RenderType.cutout()) liberanimalium$stage(RenderStage.AFTER_CUTOUT_BLOCKS);
        else if (type == RenderType.translucent()) liberanimalium$stage(RenderStage.AFTER_TRANSLUCENT_BLOCKS);
        else if (type == RenderType.tripwire()) liberanimalium$stage(RenderStage.AFTER_TRIPWIRE_BLOCKS);
    }
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void liberanimalium$blocks(CallbackInfo ci) { liberanimalium$stage(RenderStage.AFTER_BLOCK_ENTITIES); }
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V", shift = At.Shift.AFTER))
    private void liberanimalium$particles(CallbackInfo ci) { liberanimalium$stage(RenderStage.AFTER_PARTICLES); }
    @Inject(method = "renderSnowAndRain", at = @At("RETURN"))
    private void liberanimalium$weather(LightTexture light, float partial, double x, double y, double z, CallbackInfo ci) { liberanimalium$stage(RenderStage.AFTER_WEATHER); }
}

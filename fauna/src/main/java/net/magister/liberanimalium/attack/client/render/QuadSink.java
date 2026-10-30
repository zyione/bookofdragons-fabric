package net.magister.liberanimalium.attack.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public interface QuadSink {
   VertexConsumer getBuffer(RenderType var1);

   PoseStack poseStack();

   MultiBufferSource multiBufferSource();
}

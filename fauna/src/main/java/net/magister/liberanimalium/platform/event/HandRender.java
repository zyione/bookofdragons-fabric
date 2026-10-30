package net.magister.liberanimalium.platform.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;

public final class HandRender {
   private final InteractionHand hand;
   private final PoseStack poseStack;
   private final MultiBufferSource multiBufferSource;
   private final int packedLight;
   private final float partialTick;

   public HandRender(InteractionHand hand, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, float partialTick) {
      this.hand = hand;
      this.poseStack = poseStack;
      this.multiBufferSource = multiBufferSource;
      this.packedLight = packedLight;
      this.partialTick = partialTick;
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public PoseStack getPoseStack() {
      return this.poseStack;
   }

   public MultiBufferSource getMultiBufferSource() {
      return this.multiBufferSource;
   }

   public int getPackedLight() {
      return this.packedLight;
   }

   public float getPartialTick() {
      return this.partialTick;
   }
}

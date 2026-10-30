package net.magister.liberanimalium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.magister.liberanimalium.platform.fabric.FabricClientServices;
import net.magister.liberanimalium.platform.client.HandPose;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.event.HandRender;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class HandEventsMixin {
    @Shadow public abstract void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean left, PoseStack poses, MultiBufferSource buffers, int light);
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void liberanimalium$hand(AbstractClientPlayer player, float partial, float pitch, InteractionHand hand, float swing, ItemStack stack, float equip, PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo ci) {
        HandRender event = new HandRender(hand, poses, buffers, light, partial);
        for (ClientEvents.RenderHand listener : ClientEvents.RENDER_HAND.all()) {
            if (listener.render(event)) { ci.cancel(); return; }
        }
    }
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0), cancellable = true)
    private void liberanimalium$itemPose(AbstractClientPlayer player, float partial, float pitch, InteractionHand hand, float swing, ItemStack stack, float equip, PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo ci) {
        HandPose pose = FabricClientServices.ITEM_HANDS.get(stack.getItem());
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        if (pose != null && player instanceof LocalPlayer local && pose.applyHandTransform(poses, local, arm, stack, partial, equip, swing)) {
            boolean left = arm == HumanoidArm.LEFT;
            renderItem(player, stack, left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, left, poses, buffers, light);
            poses.popPose();
            ci.cancel();
        }
    }
}

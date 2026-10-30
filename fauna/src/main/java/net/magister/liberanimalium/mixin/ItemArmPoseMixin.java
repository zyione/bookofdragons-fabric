package net.magister.liberanimalium.mixin;
import net.magister.liberanimalium.platform.fabric.FabricClientServices;
import net.magister.liberanimalium.platform.client.HandPose;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public abstract class ItemArmPoseMixin {
    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void liberanimalium$pose(AbstractClientPlayer player, InteractionHand hand, CallbackInfoReturnable<ArmPose> cir) {
        var stack = player.getItemInHand(hand);
        HandPose pose = FabricClientServices.ITEM_HANDS.get(stack.getItem());
        if (pose != null) {
            ArmPose custom = pose.getArmPose(player, hand, stack);
            if (custom != null) cir.setReturnValue(custom);
        }
    }
}

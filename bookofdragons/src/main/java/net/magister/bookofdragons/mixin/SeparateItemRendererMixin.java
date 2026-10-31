package net.magister.bookofdragons.mixin;

import net.magister.bookofdragons.platform.fabric.SeparateItemModels.SeparateModel;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public abstract class SeparateItemRendererMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private BakedModel bookofdragons$displayModel(BakedModel model, ItemStack stack, ItemDisplayContext display) {
        return model instanceof SeparateModel separate ? separate.forDisplay(display) : model;
    }
}

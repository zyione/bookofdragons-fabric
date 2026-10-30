package net.magister.liberanimalium.mixin;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockPlacedEventsMixin {
    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void liberanimalium$placed(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) CommonEvents.BLOCK_PLACED.allEach(l -> l.onBlock(context.getLevel(), context.getClickedPos()));
    }
}

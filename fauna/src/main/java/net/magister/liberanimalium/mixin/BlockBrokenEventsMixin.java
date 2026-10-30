package net.magister.liberanimalium.mixin;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class BlockBrokenEventsMixin {
    @Shadow protected ServerLevel level;
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void liberanimalium$broken(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) CommonEvents.BLOCK_BROKEN.allEach(l -> l.onBlock(level, pos));
    }
}

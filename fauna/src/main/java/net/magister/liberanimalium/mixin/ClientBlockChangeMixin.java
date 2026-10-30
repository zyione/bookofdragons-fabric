package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.anim.rig.BlockChanges;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class ClientBlockChangeMixin {
   @Inject(method = "setBlockState", at = @At("RETURN"))
   private void liberanimalium$recordChange(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> cir) {
      if (cir.getReturnValue() != null) {
         if (((LevelChunk)(Object)this).getLevel().isClientSide()) {
            BlockChanges.record(pos.getX(), pos.getY(), pos.getZ());
         }
      }
   }
}

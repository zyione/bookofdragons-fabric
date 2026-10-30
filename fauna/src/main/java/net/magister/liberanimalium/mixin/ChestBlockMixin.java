package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin {
   @Inject(method = "isChestBlockedAt", at = @At("HEAD"), cancellable = true)
   private static void liberanimalium$loafingAnimalBlocksTheChest(LevelAccessor level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      if (FaunaBase.anyLoafing()) {
         AABB above = new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
         if (!level.getEntitiesOfClass(FaunaBase.class, above, animal -> animal.hasFaunaPose(PoseFlag.LOAFING)).isEmpty()) {
            cir.setReturnValue(true);
         }
      }
   }
}

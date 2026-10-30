package net.magister.liberanimalium.mixin;

import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderInstance.class)
public interface ShaderInstanceAccessor {
   @Accessor("lastProgramId")
   static void liberanimalium$setLastProgramId(int id) {
      throw new AssertionError();
   }
}

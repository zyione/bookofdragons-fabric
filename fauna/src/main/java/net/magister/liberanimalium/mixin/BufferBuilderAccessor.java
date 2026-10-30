package net.magister.liberanimalium.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
   @Accessor("nextElementByte")
   int liberanimalium$nextElementByte();

   @Accessor("nextElementByte")
   void liberanimalium$setNextElementByte(int var1);
}

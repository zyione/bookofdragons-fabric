package net.magister.liberanimalium.mixin;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("key") InputConstants.Key liberanimalium$key();
    @Accessor("clickCount") int liberanimalium$clicks();
    @Accessor("clickCount") void liberanimalium$clicks(int clicks);
}

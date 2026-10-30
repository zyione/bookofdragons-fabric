package net.magister.bookofdragons.mixin;

import net.magister.bookofdragons.datagen.data.DragonDiscoveryData;
import net.magister.bookofdragons.platform.fabric.DiscoveryCarrier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerDiscoveryMixin implements DiscoveryCarrier {
    @Unique private final DragonDiscoveryData bookofdragons$discovery = new DragonDiscoveryData();
    @Override public DragonDiscoveryData bookofdragons$discovery() { return bookofdragons$discovery; }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void bookofdragons$save(CompoundTag tag, CallbackInfo ci) {
        CompoundTag data = new CompoundTag();
        bookofdragons$discovery.saveToNBT(data);
        tag.put("bookofdragons:dragon_discovery", data);
    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void bookofdragons$load(CompoundTag tag, CallbackInfo ci) {
        CompoundTag data = tag.contains("bookofdragons:dragon_discovery", 10)
            ? tag.getCompound("bookofdragons:dragon_discovery")
            : tag.getCompound("ForgeCaps").getCompound("bookofdragons:dragon_discovery");
        bookofdragons$discovery.loadFromNBT(data);
    }
}

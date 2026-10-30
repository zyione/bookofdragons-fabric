package net.magister.liberanimalium.mixin;
import net.magister.liberanimalium.platform.fabric.FabricPackSources;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Minecraft.class)
public abstract class ClientPackSourcesMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;<init>([Lnet/minecraft/server/packs/repository/RepositorySource;)V"), index = 0)
    private RepositorySource[] liberanimalium$packs(RepositorySource[] sources) { return FabricPackSources.add(PackType.CLIENT_RESOURCES, sources); }
}

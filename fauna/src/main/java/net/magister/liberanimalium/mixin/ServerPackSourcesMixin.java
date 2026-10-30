package net.magister.liberanimalium.mixin;
import net.magister.liberanimalium.platform.fabric.FabricPackSources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ServerPacksSource.class)
public abstract class ServerPackSourcesMixin {
    @ModifyArg(method = "createPackRepository(Ljava/nio/file/Path;)Lnet/minecraft/server/packs/repository/PackRepository;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;<init>([Lnet/minecraft/server/packs/repository/RepositorySource;)V"), index = 0)
    private static RepositorySource[] liberanimalium$packs(RepositorySource[] sources) { return FabricPackSources.add(PackType.SERVER_DATA, sources); }
}

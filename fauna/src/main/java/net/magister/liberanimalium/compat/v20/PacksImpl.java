package net.magister.liberanimalium.compat.v20;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier;

public final class PacksImpl {
   private PacksImpl() {
   }

   public static ResourcesSupplier fixed(Supplier<PackResources> open) {
      return id -> open.get();
   }

   @Nullable
   public static Pack create(String id, Component title, boolean required, ResourcesSupplier resources, PackType type, Position position, PackSource source) {
      return Pack.readMetaAndCreate(id, title, required, resources, type, position, source);
   }

   public static void discover(Path folder, BiConsumer<Path, ResourcesSupplier> found) throws IOException {
      FolderRepositorySource.discoverPacks(folder, false, found);
   }
}

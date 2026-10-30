package net.magister.liberanimalium.compat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.compat.v20.PacksImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier;

public final class Packs {
   private Packs() {
   }

   public static ResourcesSupplier fixed(Supplier<PackResources> open) {
      return PacksImpl.fixed(open);
   }

   @Nullable
   public static Pack create(String id, Component title, boolean required, ResourcesSupplier resources, PackType type, Position position, PackSource source) {
      return PacksImpl.create(id, title, required, resources, type, position, source);
   }

   public static void discover(Path folder, BiConsumer<Path, ResourcesSupplier> found) throws IOException {
      PacksImpl.discover(folder, found);
   }
}

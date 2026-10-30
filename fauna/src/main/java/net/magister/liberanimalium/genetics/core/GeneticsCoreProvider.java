package net.magister.liberanimalium.genetics.core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;
import net.magister.liberanimalium.compat.Packs;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources.ResourceOutput;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GeneticsCoreProvider implements PackResources {
   private final String namespace;

   public GeneticsCoreProvider(String namespace) {
      this.namespace = namespace;
   }

   public static String packId(String namespace) {
      return namespace + "_genetics_core";
   }

   @Nullable
   public static Pack pack(String namespace) {
      return Packs.create(
         packId(namespace),
         title(namespace),
         true,
         Packs.fixed(() -> new GeneticsCoreProvider(namespace)),
         PackType.CLIENT_RESOURCES,
         Position.TOP,
         PackSource.BUILT_IN
      );
   }

   private static Component title(String namespace) {
      return Component.translatable("liberanimalium.pack.genetics_core", new Object[]{namespace});
   }

   private boolean serves(ResourceLocation location) {
      return location.getNamespace().equals(this.namespace) && GeneticsSequenceBuffer.has(this.namespace, location.getPath());
   }

   private IoSupplier<InputStream> opener(String path) {
      return () -> new ByteArrayInputStream(GeneticsSequenceBuffer.read(this.namespace, path));
   }

   @Nullable
   public IoSupplier<InputStream> getRootResource(@NotNull String... paths) {
      return null;
   }

   @Nullable
   public IoSupplier<InputStream> getResource(@NotNull PackType packType, @NotNull ResourceLocation location) {
      return packType == PackType.CLIENT_RESOURCES && this.serves(location) ? this.opener(location.getPath()) : null;
   }

   public void listResources(@NotNull PackType packType, @NotNull String namespace, @NotNull String path, @NotNull ResourceOutput resourceOutput) {
      if (packType == PackType.CLIENT_RESOURCES && namespace.equals(this.namespace)) {
         String folder = path.endsWith("/") ? path : path + "/";

         for (String entry : GeneticsSequenceBuffer.paths(namespace)) {
            if (entry.startsWith(folder)) {
               resourceOutput.accept(new ResourceLocation(namespace, entry), this.opener(entry));
            }
         }
      }
   }

   @NotNull
   public Set<String> getNamespaces(@NotNull PackType packType) {
      return packType == PackType.CLIENT_RESOURCES && GeneticsSequenceBuffer.namespaces().contains(this.namespace) ? Set.of(this.namespace) : Set.of();
   }

   @Nullable
   public <T> T getMetadataSection(@NotNull MetadataSectionSerializer<T> deserializer) {
      return (T)(deserializer.getMetadataSectionName().equals("pack")
         ? new PackMetadataSection(title(this.namespace), SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES))
         : null);
   }

   @NotNull
   public String packId() {
      return packId(this.namespace);
   }

   public void close() {
   }
}

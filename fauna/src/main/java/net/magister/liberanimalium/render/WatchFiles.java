package net.magister.liberanimalium.render;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

public final class WatchFiles {
   private WatchFiles() {
   }

   @Nullable
   public static WatchFiles.Opened open(String folder, String name, int entityId, String header, Logger log, String tag) {
      try {
         Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("logs").resolve(folder);
         Files.createDirectories(dir);
         String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
         Path path = dir.resolve(name.replaceAll("[^A-Za-z0-9_-]", "_") + "-" + entityId + "-" + stamp + ".csv");
         BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
         writer.write(header + "\n");
         return new WatchFiles.Opened(writer, path);
      } catch (IOException e) {
         log.warn(tag + " could not open the watch file", e);
         return null;
      }
   }

   public static void close(@Nullable BufferedWriter writer) {
      if (writer != null) {
         try {
            writer.close();
         } catch (IOException var2) {
         }
      }
   }

   public record Opened(BufferedWriter writer, Path path) {
   }
}

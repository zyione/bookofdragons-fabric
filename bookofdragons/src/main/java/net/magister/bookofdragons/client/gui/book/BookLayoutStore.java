package net.magister.bookofdragons.client.gui.book;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import net.magister.liberanimalium.platform.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class BookLayoutStore {
   private static final Logger LOGGER = LogManager.getLogger("BookLayoutStore");
   private static final Gson GSON = new GsonBuilder().create();
   private static final String SHIPPED = "/assets/bookofdragons/book_layout.json";
   private static final Map<String, Map<String, float[]>> SAVED = new TreeMap<>();
   private static final Map<String, BookLayoutEditor> EDITORS = new LinkedHashMap<>();
   private static boolean loaded;

   private BookLayoutStore() {
   }

   public static void attach(String name, BookLayoutEditor editor) {
      load();
      EDITORS.put(name, editor);
      Map<String, float[]> slots = SAVED.get(name);
      if (slots != null) {
         for (BookLayoutEditor.Slot slot : editor.slots()) {
            float[] v = slots.get(slot.name);
            if (v != null && v.length >= 4) {
               slot.x = Math.round(v[0]);
               slot.y = Math.round(v[1]);
               slot.w = Math.round(v[2]);
               slot.h = Math.round(v[3]);
               if (v.length > 4) {
                  slot.scale = Math.max(slot.minScale, v[4]);
               }
            }
         }
      }
   }

   public static void save() {
      for (Entry<String, BookLayoutEditor> e : EDITORS.entrySet()) {
         Map<String, float[]> slots = SAVED.computeIfAbsent(e.getKey(), k -> new TreeMap<>());

         for (BookLayoutEditor.Slot s : e.getValue().slots()) {
            slots.put(s.name, new float[]{s.x, s.y, s.w, s.h, Math.round(s.scale * 100.0F) / 100.0F});
         }
      }

      String text = write();
      write(configFile(), text);
      Path dev = devFile();
      if (dev != null) {
         write(dev, text);
      }
   }

   private static void load() {
      if (!loaded) {
         loaded = true;

         try (InputStream in = BookLayoutStore.class.getResourceAsStream("/assets/bookofdragons/book_layout.json")) {
            if (in != null) {
               read(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
         } catch (IOException | RuntimeException e) {
            LOGGER.warn("[BookLayoutStore] shipped layout unreadable: {}", e.getMessage());
         }

         Path config = configFile();
         if (Files.isRegularFile(config)) {
            try {
               read(JsonParser.parseString(Files.readString(config)));
            } catch (IOException | RuntimeException e) {
               LOGGER.warn("[BookLayoutStore] {} unreadable: {}", config, e.getMessage());
            }
         }
      }
   }

   private static void read(JsonElement root) {
      if (root.isJsonObject()) {
         for (Entry<String, JsonElement> editor : root.getAsJsonObject().entrySet()) {
            if (editor.getValue().isJsonObject()) {
               Map<String, float[]> slots = SAVED.computeIfAbsent(editor.getKey(), k -> new TreeMap<>());

               for (Entry<String, JsonElement> slot : editor.getValue().getAsJsonObject().entrySet()) {
                  if (slot.getValue().isJsonArray()) {
                     JsonArray a = slot.getValue().getAsJsonArray();
                     float[] v = new float[a.size()];

                     for (int i = 0; i < a.size(); i++) {
                        v[i] = a.get(i).getAsFloat();
                     }

                     slots.put(slot.getKey(), v);
                  }
               }
            }
         }
      }
   }

   private static String write() {
      StringBuilder out = new StringBuilder("{\n");
      int e = 0;

      for (Entry<String, Map<String, float[]>> editor : SAVED.entrySet()) {
         out.append("  ").append(GSON.toJson(editor.getKey())).append(": {\n");
         int s = 0;

         for (Entry<String, float[]> slot : editor.getValue().entrySet()) {
            JsonArray a = new JsonArray();
            float[] v = slot.getValue();

            for (int i = 0; i < v.length; i++) {
               if (i < 4) {
                  a.add(Math.round(v[i]));
               } else {
                  a.add(v[i]);
               }
            }

            out.append("    ").append(GSON.toJson(slot.getKey())).append(": ").append(GSON.toJson(a));
            s++;
            out.append(s < editor.getValue().size() ? ",\n" : "\n");
         }

         StringBuilder var10000 = out.append("  }");
         e++;
         var10000.append(e < SAVED.size() ? ",\n" : "\n");
      }

      return out.append("}\n").toString();
   }

   private static void write(Path file, String text) {
      try {
         Files.createDirectories(file.getParent());
         Files.writeString(file, text);
      } catch (IOException e) {
         LOGGER.warn("[BookLayoutStore] cannot write {}: {}", file, e.getMessage());
      }
   }

   private static Path configFile() {
      return Services.platform().configDir().resolve("bookofdragons").resolve("book_layout.json");
   }

   private static Path devFile() {
      if (Services.platform().isProduction()) {
         return null;
      }

      Path assets = Services.platform().gameDir().toAbsolutePath().normalize().resolve("../../src/main/resources/assets/bookofdragons").normalize();
      return Files.isDirectory(assets) ? assets.resolve("book_layout.json") : null;
   }

   public static boolean editable() {
      return !Services.platform().isProduction();
   }
}

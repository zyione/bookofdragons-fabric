package net.magister.bookofdragons.entity.component;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.magister.liberanimalium.genetics.core.GeneticsSequenceBuffer;

public final class EatWindow {
   public static final int FALLBACK_TICKS = 47;
   public static final int BLEND_TICKS = 6;
   public static final int LEAD_TICKS = 8;
   private static final String ANIMATIONS = "/assets/bookofdragons/animations/";
   private static final String CLIP = "eat";
   private static final float TICKS_PER_SECOND = 20.0F;
   private static final Map<String, Integer> CACHE = new HashMap<>();

   private EatWindow() {
   }

   public static synchronized int ticks(String species) {
      return CACHE.computeIfAbsent(species, EatWindow::read);
   }

   private static InputStream open(String species) {
      String file = species + ".animation.json";
      byte[] sealed = GeneticsSequenceBuffer.read("bookofdragons", "animations/" + file);
      return sealed != null ? new ByteArrayInputStream(sealed) : EatWindow.class.getResourceAsStream("/assets/bookofdragons/animations/" + file);
   }

   private static int read(String species) {
      try (InputStream in = open(species)) {
         if (in == null) {
            return 47;
         }

         JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
         JsonObject animations = root.getAsJsonObject("animations");
         if (animations == null) {
            return 47;
         }

         JsonObject clip = animations.getAsJsonObject("animation." + species + ".eat");
         return clip != null && clip.has("animation_length") ? Math.max(1, Math.round(clip.get("animation_length").getAsFloat() * 20.0F)) + 8 : 47;
      } catch (IOException | RuntimeException e) {
         return 47;
      }
   }
}

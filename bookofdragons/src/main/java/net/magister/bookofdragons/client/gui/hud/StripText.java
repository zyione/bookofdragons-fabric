package net.magister.bookofdragons.client.gui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

public final class StripText {
   private static final ResourceLocation INKS = new ResourceLocation("bookofdragons", "strip_inks.json");
   private static final String FACT = "fact";
   private static final Logger LOGGER = LogUtils.getLogger();

   private StripText() {
   }

   public static Component style(String key, Component... args) {
      Map<String, Integer> inks = inks();
      return style(Language.getInstance().getOrDefault(key), inks, inks.getOrDefault("fact", 10035230), args);
   }

   static Component style(String pattern, Map<String, Integer> inks, int fact, Component... args) {
      MutableComponent out = Component.empty();
      StringBuilder run = new StringBuilder();
      Integer ink = null;
      int next = 0;

      for (int i = 0; i < pattern.length(); i++) {
         char c = pattern.charAt(i);
         if (c == '{') {
            flush(out, run, ink);
            ink = fact;
            int colon = pattern.indexOf(58, i);
            int close = pattern.indexOf(125, i);
            if (colon > i && (close < 0 || colon < close)) {
               Integer named = inks.get(pattern.substring(i + 1, colon));
               if (named != null) {
                  ink = named;
                  i = colon;
               }
            }
         } else if (c == '}') {
            flush(out, run, ink);
            ink = null;
         } else if (c == '%' && i + 1 < pattern.length()) {
            int end = i + 1;

            while (end < pattern.length() && Character.isDigit(pattern.charAt(end))) {
               end++;
            }

            boolean indexed = end > i + 1 && end + 1 < pattern.length() && pattern.charAt(end) == '$' && isArgument(pattern.charAt(end + 1));
            if (pattern.charAt(i + 1) == '%') {
               run.append('%');
               i++;
            } else if (!indexed && !isArgument(pattern.charAt(i + 1))) {
               run.append(c);
            } else {
               int index = indexed ? Integer.parseInt(pattern.substring(i + 1, end)) - 1 : next++;
               flush(out, run, ink);
               if (index >= 0 && index < args.length) {
                  int color = ink == null ? fact : ink;
                  out.append(args[index].copy().withStyle(s -> s.withColor(color)));
               }

               i = indexed ? end + 1 : i + 1;
            }
         } else {
            run.append(c);
         }
      }

      flush(out, run, ink);
      return out;
   }

   private static boolean isArgument(char conversion) {
      return conversion == 's' || conversion == 'd';
   }

   private static void flush(MutableComponent out, StringBuilder run, Integer ink) {
      if (run.length() != 0) {
         MutableComponent part = Component.literal(run.toString());
         out.append(ink == null ? part : part.withStyle(s -> s.withColor(ink)));
         run.setLength(0);
      }
   }

   private static Map<String, Integer> inks() {
      Map<String, Integer> out = new HashMap<>();
      Optional<Resource> file = Minecraft.getInstance().getResourceManager().getResource(INKS);
      if (file.isEmpty()) {
         return out;
      }

      try (Reader reader = file.get().openAsReader()) {
         JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

         for (Entry<String, JsonElement> entry : json.entrySet()) {
            String hex = entry.getValue().getAsString().replace("#", "");
            out.put(entry.getKey().toLowerCase(Locale.ROOT), Integer.parseInt(hex, 16) & 16777215);
         }
      } catch (Exception malformed) {
         LOGGER.warn("strip_inks.json is not a map of ink names to hex colours: {}", malformed.toString());
         out.clear();
      }

      return out;
   }
}

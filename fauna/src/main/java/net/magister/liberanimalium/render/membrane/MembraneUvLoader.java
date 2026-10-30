package net.magister.liberanimalium.render.membrane;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MembraneUvLoader extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("MembraneUvLoader");
   private static final Gson GSON = new Gson();

   public MembraneUvLoader() {
      super(GSON, "membrane_uv");
   }

   protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
      MembraneDefinitions.clear();
      int speciesLoaded = 0;
      int panelTotal = 0;

      for (Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
         String species = entry.getKey().getPath();
         Map<String, List<MembranePanel>> byChain = parseChains(entry.getValue().getAsJsonObject());
         List<MembranePanel> all = new ArrayList<>();

         for (List<MembranePanel> list : byChain.values()) {
            all.addAll(list);
         }

         for (Entry<String, List<MembranePanel>> e : byChain.entrySet()) {
            if (!byChain.containsKey(siblingChain(e.getKey()))) {
               for (MembranePanel p : e.getValue()) {
                  all.add(
                     new MembranePanel(
                        swapLR(p.corner()),
                        p.uvTop(),
                        p.uvBottom(),
                        p.localDorsal(),
                        p.restRatio(),
                        swapLR(p.foldRef()),
                        p.tip(),
                        swapLR(p.dorsalAnchor()),
                        p.creasePeak(),
                        p.uStart(),
                        p.uEnd()
                     )
                  );
               }
            }
         }

         MembraneDefinitions.put(species, all);
         speciesLoaded++;
         panelTotal += all.size();
      }

      LOGGER.debug("[Membrane] Loaded {} panels across {} species.", panelTotal, speciesLoaded);
   }

   private static Map<String, List<MembranePanel>> parseChains(JsonObject root) {
      Map<String, List<MembranePanel>> result = new HashMap<>();

      for (Entry<String, JsonElement> chainEntry : root.entrySet()) {
         List<MembranePanel> list = new ArrayList<>();

         for (JsonElement el : chainEntry.getValue().getAsJsonArray()) {
            JsonObject po = el.getAsJsonObject();
            JsonArray cs = po.getAsJsonArray("corners");
            String[] corners = new String[cs.size()];

            for (int k = 0; k < cs.size(); k++) {
               corners[k] = cs.get(k).getAsString();
            }

            JsonArray ds = po.getAsJsonArray("dorsal");
            float[] dorsal = new float[]{ds.get(0).getAsFloat(), ds.get(1).getAsFloat(), ds.get(2).getAsFloat()};
            float restRatio = po.has("restRatio") ? po.get("restRatio").getAsFloat() : 1.0F;
            String[] foldRef = null;
            if (po.has("foldRef")) {
               JsonArray frj = po.getAsJsonArray("foldRef");
               foldRef = new String[frj.size()];

               for (int k = 0; k < frj.size(); k++) {
                  foldRef[k] = frj.get(k).getAsString();
               }
            }

            boolean tipFlag = isTip(corners);
            int creasePeak = po.has("creasePeak") ? po.get("creasePeak").getAsInt() : (tipFlag ? 1 : 0);
            String dorsalAnchor = po.has("dorsalAnchor") && !po.get("dorsalAnchor").isJsonNull() ? po.get("dorsalAnchor").getAsString() : corners[0];
            float uStart = 0.0F;
            float uEnd = 1.0F;
            if (po.has("uSpan")) {
               JsonArray s = po.getAsJsonArray("uSpan");
               uStart = s.get(0).getAsFloat();
               uEnd = s.get(1).getAsFloat();
            }

            list.add(
               new MembranePanel(
                  corners,
                  parseQuad(po.getAsJsonArray("top")),
                  parseQuad(po.getAsJsonArray("bottom")),
                  dorsal,
                  restRatio,
                  foldRef,
                  tipFlag,
                  dorsalAnchor,
                  creasePeak,
                  uStart,
                  uEnd
               )
            );
         }

         result.put(chainEntry.getKey(), list);
      }

      return result;
   }

   private static boolean isTip(String[] corners) {
      for (String c : corners) {
         if (c.toLowerCase().contains("fingertip")) {
            return true;
         }
      }

      return false;
   }

   private static float[][] parseQuad(JsonArray quad) {
      float[][] uv = new float[4][2];

      for (int k = 0; k < 4; k++) {
         JsonArray pair = quad.get(k).getAsJsonArray();
         uv[k][0] = pair.get(0).getAsFloat();
         uv[k][1] = pair.get(1).getAsFloat();
      }

      return uv;
   }

   private static String siblingChain(String chain) {
      if (chain.startsWith("l_")) {
         return "r_" + chain.substring(2);
      } else {
         return chain.startsWith("r_") ? "l_" + chain.substring(2) : chain;
      }
   }

   private static String[] swapLR(String[] corners) {
      if (corners == null) {
         return null;
      }

      String[] out = new String[corners.length];

      for (int i = 0; i < corners.length; i++) {
         out[i] = swapLR(corners[i]);
      }

      return out;
   }

   private static String swapLR(String name) {
      if (name.length() >= 2 && name.charAt(1) == '_') {
         char c = name.charAt(0);

         char s = switch (c) {
            case 'L' -> 'R';
            case 'R' -> 'L';
            case 'l' -> 'r';
            case 'r' -> 'l';
            default -> c;
         };
         return s + name.substring(1);
      } else {
         return name;
      }
   }
}

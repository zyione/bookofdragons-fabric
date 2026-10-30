package net.magister.liberanimalium.body;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.genetics.core.GeneticsSequenceBuffer;
import net.magister.liberanimalium.platform.registry.Regs;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;

public final class SpeciesAssets {
   private static final List<String> HOSTS = new CopyOnWriteArrayList<>();
   private static final Map<String, Class<?>> ANCHORS = new ConcurrentHashMap<>();
   private static final Map<String, ResourceLocation> LOCATED = new ConcurrentHashMap<>();
   private static final Map<EntityType<?>, String> TYPE_HOSTS = new ConcurrentHashMap<>();
   private static final Map<String, String> SPECIES_HOSTS = new ConcurrentHashMap<>();

   private SpeciesAssets() {
   }

   public static void host(String namespace, Class<?> anchor) {
      ANCHORS.put(namespace, anchor);
      if (!HOSTS.contains(namespace)) {
         HOSTS.add(namespace);
      }
   }

   public static void host(String namespace) {
      if (!HOSTS.contains(namespace)) {
         HOSTS.add(namespace);
      }
   }

   public static Class<?> anchorFor(String namespace, Class<?> caller) {
      return ANCHORS.getOrDefault(namespace, caller);
   }

   public static List<String> hosts() {
      return List.copyOf(HOSTS);
   }

   public static String hostOf(Entity creature) {
      return hostOf(creature.getType());
   }

   public static String hostOf(EntityType<?> type) {
      String known = TYPE_HOSTS.get(type);
      if (known != null) {
         return known;
      }

      ResourceLocation id = Regs.entityTypeKey(type);
      String host = id == null ? "liberanimalium" : id.getNamespace();
      TYPE_HOSTS.put(type, host);
      return host;
   }

   public static void noteSpecies(String species, String host) {
      SPECIES_HOSTS.putIfAbsent(species, host);
   }

   @Nullable
   public static String hostOfSpecies(String species) {
      return SPECIES_HOSTS.get(species);
   }

   public static List<String> namespaces() {
      List<String> out = new ArrayList<>(HOSTS);
      if (!out.contains("liberanimalium")) {
         out.add("liberanimalium");
      }

      return out;
   }

   public static <T> Map<String, Entry<ResourceLocation, T>> byId(Map<ResourceLocation, T> files, String what, Logger log) {
      List<String> order = namespaces();
      List<Entry<ResourceLocation, T>> sorted = new ArrayList<>(files.entrySet());
      sorted.sort(Comparator.<Entry<ResourceLocation, T>>comparingInt(e -> {
         int rank = order.indexOf(e.getKey().getNamespace());
         return rank < 0 ? order.size() : rank;
      }).thenComparing(e -> e.getKey().getNamespace()).thenComparing(e -> e.getKey().getPath()));
      Map<String, Entry<ResourceLocation, T>> out = new LinkedHashMap<>();

      for (Entry<ResourceLocation, T> file : sorted) {
         String id = file.getKey().getPath();
         Entry<ResourceLocation, T> winner = out.get(id);
         if (winner != null) {
            log.warn(
               "{} {} is ignored: {} already defines '{}'. Override a host's file in the host's namespace.",
               new Object[]{what, file.getKey(), winner.getKey(), id}
            );
         } else {
            out.put(id, file);
         }
      }

      return out;
   }

   @Nullable
   public static ResourceLocation find(String path, Predicate<ResourceLocation> exists) {
      for (String namespace : namespaces()) {
         ResourceLocation candidate = new ResourceLocation(namespace, path);
         if (exists.test(candidate)) {
            return candidate;
         }
      }

      return null;
   }

   public static ResourceLocation locate(String path) {
      return LOCATED.computeIfAbsent(path, p -> {
         ResourceLocation found = find(p, SpeciesAssets::clientHas);
         return found != null ? found : new ResourceLocation("liberanimalium", p);
      });
   }

   public static Optional<ResourceLocation> findClient(String path) {
      ResourceLocation located = locate(path);
      return clientHas(located) ? Optional.of(located) : Optional.empty();
   }

   static boolean clientHas(ResourceLocation location) {
      try {
         return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
      } catch (Exception e) {
         return false;
      }
   }

   @Nullable
   public static InputStream open(Class<?> anchor, String path) {
      for (String namespace : namespaces()) {
         InputStream in = openIn(namespace, anchor, path);
         if (in != null) {
            return in;
         }
      }

      return null;
   }

   @Nullable
   private static InputStream openIn(String namespace, Class<?> anchor, String path) {
      byte[] sealed = GeneticsSequenceBuffer.read(namespace, path);
      return sealed != null ? new ByteArrayInputStream(sealed) : anchorFor(namespace, anchor).getResourceAsStream("/assets/" + namespace + "/" + path);
   }

   public static ResourceLocation locateFor(@Nullable String host, String path) {
      if (host == null) {
         return locate(path);
      }

      String key = host + ":" + path;
      ResourceLocation known = LOCATED.get(key);
      if (known != null) {
         return known;
      }

      ResourceLocation found = findFor(host, path, SpeciesAssets::clientHas);
      if (found == null) {
         found = locate(path);
      }

      LOCATED.put(key, found);
      return found;
   }

   @Nullable
   public static ResourceLocation findFor(@Nullable String host, String path, Predicate<ResourceLocation> exists) {
      if (host != null) {
         ResourceLocation own = new ResourceLocation(host, path);
         if (exists.test(own)) {
            return own;
         }
      }

      return find(path, exists);
   }

   @Nullable
   public static InputStream openFor(@Nullable String host, Class<?> anchor, String path) {
      if (host != null) {
         InputStream in = openIn(host, anchor, path);
         if (in != null) {
            return in;
         }
      }

      return open(anchor, path);
   }

   public static void invalidate() {
      LOCATED.clear();
      BabyArt.invalidate();
   }
}

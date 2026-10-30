package net.magister.liberanimalium.genetics.core;

import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class GeneticsSequenceBuffer {
   private static final Map<String, SequenceStore> STORES = new ConcurrentHashMap<>();
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Set<String> NAMESPACES = ConcurrentHashMap.newKeySet();

   public static synchronized void install(String namespace, byte[] seed, Supplier<InputStream> container) {
      if (NAMESPACES.add(namespace)) {
         try (InputStream in = container.get()) {
            if (in != null) {
               SequenceStore store = SequenceStore.read(in.readAllBytes(), seed, namespace);
               STORES.put(namespace, store);
               LOGGER.debug("[Genetics] Loaded " + store.paths().size() + " genetic sequences for " + namespace + ".");
            } else {
               LOGGER.warn("[Genetics] Genetic Sequence Data not found for " + namespace + ". Using default datasets (normal in dev).");
            }
         } catch (Exception e) {
            LOGGER.error("[Genetics] Failed to initialize sequence buffers for " + namespace, e);
         }
      }
   }

   public static Set<String> namespaces() {
      return Collections.unmodifiableSet(NAMESPACES);
   }

   public static Set<String> paths(String namespace) {
      SequenceStore store = STORES.get(namespace);
      return store == null ? Set.of() : store.paths();
   }

   public static boolean has(String namespace, String path) {
      SequenceStore store = STORES.get(namespace);
      return store != null && store.has(path);
   }

   @Nullable
   public static byte[] read(String namespace, String path) {
      SequenceStore store = STORES.get(namespace);
      return store == null ? null : store.open(path);
   }
}

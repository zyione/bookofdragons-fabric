package net.magister.liberanimalium.network;

import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public final class DataSync {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final int NEAR_LIMIT = 921600;
   private static final Map<String, DataSync.DataSet> SETS = new LinkedHashMap<>();

   private DataSync() {
   }

   public static void register(String id, Supplier<Map<String, String>> raw, Consumer<Map<String, String>> install) {
      if (SETS.putIfAbsent(id, new DataSync.DataSet(raw, install)) != null) {
         throw new IllegalStateException("data set " + id + " is registered twice");
      }
   }

   public static void send(ServerPlayer player) {
      int total = 0;
      int largest = 0;
      String largestId = "";

      for (Entry<String, DataSync.DataSet> set : SETS.entrySet()) {
         SyncDataPacket packet = new SyncDataPacket(set.getKey(), set.getValue().raw().get());
         int bytes = SyncDataPacket.size(packet);
         total += bytes;
         if (bytes > largest) {
            largest = bytes;
            largestId = set.getKey();
         }

         if (bytes > 921600) {
            LOGGER.warn("[DataSync] data set {} is {} bytes, near the game's packet limit", set.getKey(), bytes);
         }

         FaunaNetwork.sendToPlayer(packet, player);
      }

      LOGGER.debug(
         "[DataSync] sent {} data sets to {}: {} bytes, the largest {} at {} bytes", new Object[]{SETS.size(), player.getScoreboardName(), total, largestId, largest}
      );
   }

   static void install(String id, Map<String, String> files) {
      DataSync.DataSet set = SETS.get(id);
      if (set == null) {
         LOGGER.warn("[DataSync] the server sent data set {}, which this client does not know", id);
      } else {
         set.install().accept(files);
      }
   }

   private record DataSet(Supplier<Map<String, String>> raw, Consumer<Map<String, String>> install) {
   }
}

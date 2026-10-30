package net.magister.liberanimalium.debug.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.debug.FaunaSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class FaunaDebugClient {
   private static final long LIVE_WINDOW_MS = 1500L;
   private static volatile Map<UUID, FaunaSnapshot> snapshots = Map.of();
   private static volatile long lastPacketAt;
   private static volatile boolean panel = true;

   private FaunaDebugClient() {
   }

   public static void accept(List<FaunaSnapshot> received) {
      Map<UUID, FaunaSnapshot> next = new ConcurrentHashMap<>();

      for (FaunaSnapshot s : received) {
         next.put(s.animalId(), s);
      }

      snapshots = next;
      lastPacketAt = System.currentTimeMillis();
   }

   public static boolean enabled() {
      return System.currentTimeMillis() - lastPacketAt < 1500L;
   }

   public static void setEnabled(boolean value) {
      if (!value) {
         snapshots = Map.of();
         lastPacketAt = 0L;
      }
   }

   public static boolean panelShown() {
      return panel;
   }

   public static void togglePanel() {
      panel = !panel;
   }

   public static List<FaunaSnapshot> snapshots() {
      return List.copyOf(snapshots.values());
   }

   @Nullable
   public static FaunaSnapshot get(UUID id) {
      return snapshots.get(id);
   }

   @Nullable
   public static FaunaSnapshot lookedAt() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && !snapshots.isEmpty()) {
         Vec3 eye = mc.player.getEyePosition();
         Vec3 look = mc.player.getLookAngle();
         FaunaSnapshot best = null;
         double bestAlong = Double.MAX_VALUE;

         for (Entity e : mc.level.entitiesForRendering()) {
            FaunaSnapshot snap = snapshots.get(e.getUUID());
            if (snap != null) {
               Vec3 toEntity = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0).subtract(eye);
               double along = toEntity.dot(look);
               if (!(along <= 0.0)) {
                  double perpendicular = toEntity.subtract(look.scale(along)).length();
                  if (!(perpendicular > e.getBbWidth() * 0.5 + 2.0) && along < bestAlong) {
                     bestAlong = along;
                     best = snap;
                  }
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   @Nullable
   public static Entity entityOf(FaunaSnapshot snap) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return null;
      }

      for (Entity e : mc.level.entitiesForRendering()) {
         if (e.getUUID().equals(snap.animalId())) {
            return e;
         }
      }

      return null;
   }
}

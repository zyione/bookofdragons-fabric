package net.magister.liberanimalium.body.anim.extraction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CameraBoneExtractor {
   private static final Map<Integer, Vec3> CAMERA_POSITIONS = new ConcurrentHashMap<>();

   public static void store(int entityId, Vec3 position) {
      CAMERA_POSITIONS.put(entityId, position);
   }

   @Nullable
   public static Vec3 get(int entityId) {
      return CAMERA_POSITIONS.get(entityId);
   }

   public static void remove(int entityId) {
      CAMERA_POSITIONS.remove(entityId);
   }

   public static void clearAll() {
      CAMERA_POSITIONS.clear();
   }
}

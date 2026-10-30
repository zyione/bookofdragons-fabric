package net.magister.liberanimalium.sound.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

final class TakeLengths {
   private static final Map<ResourceLocation, Float> SECONDS = new HashMap<>();

   private TakeLengths() {
   }

   static float seconds(SoundInstance instance) {
      if (instance.getSound() != null && instance.getSound() != SoundManager.EMPTY_SOUND) {
         ResourceLocation path = instance.getSound().getPath();
         float length = SECONDS.computeIfAbsent(path, TakeLengths::read);
         float pitch = instance.getPitch();
         return !(length <= 0.0F) && !(pitch <= 0.0F) ? length / pitch : -1.0F;
      } else {
         return -1.0F;
      }
   }

   private static float read(ResourceLocation path) {
      Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(path);
      if (resource.isEmpty()) {
         return -1.0F;
      }

      try (InputStream in = resource.get().open()) {
         return length(in.readAllBytes());
      } catch (IOException e) {
         return -1.0F;
      }
   }

   static float length(byte[] ogg) {
      int rate = -1;

      for (int i = 0; i + 15 < Math.min(ogg.length, 256); i++) {
         if (ogg[i] == 1 && ogg[i + 1] == 118 && ogg[i + 2] == 111 && ogg[i + 3] == 114 && ogg[i + 4] == 98 && ogg[i + 5] == 105 && ogg[i + 6] == 115) {
            rate = int32(ogg, i + 12);
            break;
         }
      }

      for (int i = ogg.length - 27; i >= 0 && rate > 0; i--) {
         if (ogg[i] == 79 && ogg[i + 1] == 103 && ogg[i + 2] == 103 && ogg[i + 3] == 83) {
            long granule = int32(ogg, i + 6) & 4294967295L | (long)int32(ogg, i + 10) << 32;
            return granule > 0L ? (float)granule / rate : -1.0F;
         }
      }

      return -1.0F;
   }

   private static int int32(byte[] b, int at) {
      return b[at] & 0xFF | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
   }
}

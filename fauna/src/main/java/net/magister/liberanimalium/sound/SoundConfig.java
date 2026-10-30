package net.magister.liberanimalium.sound;

import com.google.gson.JsonObject;
import net.magister.liberanimalium.platform.registry.Regs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;

public record SoundConfig(ResourceLocation soundId, float volume, float pitch, float variance, int range) {
   public static final SoundConfig SILENT = new SoundConfig(null, 0.0F, 1.0F, 0.0F, 0);

   public static SoundConfig parse(JsonObject json) {
      int range = json.has("range") ? json.get("range").getAsInt() : 0;
      if (range < 0) {
         throw new IllegalArgumentException("range is 0 (the event's own) or a number of blocks");
      } else {
         return new SoundConfig(
            ResourceLocation.tryParse(json.get("sound").getAsString()),
            json.has("volume") ? json.get("volume").getAsFloat() : 1.0F,
            json.has("pitch") ? json.get("pitch").getAsFloat() : 1.0F,
            json.has("variance") ? json.get("variance").getAsFloat() : 0.0F,
            range
         );
      }
   }

   public SoundEvent sound() {
      return this.soundId == null ? null : Regs.sound(this.soundId);
   }

   public float pitchFor(RandomSource random) {
      return this.variance <= 0.0F ? this.pitch : this.pitch + (random.nextFloat() - 0.5F) * this.variance * 2.0F;
   }
}

package net.magister.liberanimalium.sound.client;

import java.util.HashMap;
import java.util.Map;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.sound.FaunaSoundHelper;
import net.magister.liberanimalium.sound.SoundConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

final class ChannelVoice {
   private static final int SWAP_BEFORE_END_TICKS = 2;
   private static final int RELEASE_TICKS = 3;
   private final Map<Integer, ChannelVoice.Held> held = new HashMap<>();

   void tick(FaunaBase body, boolean on, @Nullable String start, String loop, boolean looping, @Nullable String end) {
      SoundManager sounds = Minecraft.getInstance().getSoundManager();
      ChannelVoice.Held now = this.held.get(body.getId());
      long tick = body.level().getGameTime();
      if (on) {
         if (now == null) {
            ChannelVoice.Take opening = ChannelVoice.Take.of(body, start, false, Float.NaN);
            if (opening == null) {
               this.held.put(body.getId(), new ChannelVoice.Held(null, play(ChannelVoice.Take.of(body, loop, looping, Float.NaN), 1.0F), tick));
            } else {
               play(opening, 1.0F);
               ChannelVoice.Take silent = play(ChannelVoice.Take.of(body, loop, looping, opening.getPitch()), 0.0F);
               float seconds = TakeLengths.seconds(opening);
               long swapAt = seconds < 0.0F ? Long.MAX_VALUE : tick + Math.max(0, Math.round(seconds * 20.0F) - 2);
               this.held.put(body.getId(), new ChannelVoice.Held(opening, silent, swapAt));
            }
         } else if (now.start() != null && (tick >= now.swapAt() || !playing(sounds, now.start()))) {
            if (now.loop() != null) {
               now.loop().fadeTo(1.0F, 0);
            }

            sounds.stop(now.start());
            this.held.put(body.getId(), new ChannelVoice.Held(null, now.loop(), now.swapAt()));
         } else if (now.start() == null && !playing(sounds, now.loop())) {
            this.held.put(body.getId(), new ChannelVoice.Held(null, play(ChannelVoice.Take.of(body, loop, looping, Float.NaN), 1.0F), tick));
         }
      } else if (now != null) {
         this.held.remove(body.getId());
         if (now.start() != null) {
            sounds.stop(now.start());
         }

         ChannelVoice.Take ending = ChannelVoice.Take.of(body, end, false, Float.NaN);
         if (now.loop() != null) {
            if (ending != null && now.start() == null) {
               now.loop().fadeTo(0.0F, 3);
            } else {
               sounds.stop(now.loop());
            }
         }

         play(ending, 1.0F);
      }
   }

   void remove(FaunaBase body) {
      ChannelVoice.Held now = this.held.remove(body.getId());
      if (now != null) {
         SoundManager sounds = Minecraft.getInstance().getSoundManager();
         if (now.start() != null) {
            sounds.stop(now.start());
         }

         if (now.loop() != null) {
            sounds.stop(now.loop());
         }
      }
   }

   private static boolean playing(SoundManager sounds, @Nullable SoundInstance instance) {
      return instance != null && sounds.isActive(instance);
   }

   @Nullable
   private static ChannelVoice.Take play(@Nullable ChannelVoice.Take take, float share) {
      if (take == null) {
         return null;
      }

      take.fadeTo(share, 0);
      Minecraft.getInstance().getSoundManager().play(take);
      return take;
   }

   private record Held(@Nullable ChannelVoice.Take start, @Nullable ChannelVoice.Take loop, long swapAt) {
   }

   private static final class Take extends EntityBoundSoundInstance {
      private final boolean looping;
      private final float full;
      private float target;
      private float step;

      private Take(SoundEvent sound, FaunaBase body, float full, float pitch, boolean looping) {
         super(sound, body.getSoundSource(), full, pitch, body, body.getRandom().nextLong());
         this.looping = looping;
         this.full = full;
      }

      @Nullable
      static ChannelVoice.Take of(FaunaBase body, @Nullable String voiceName, boolean looping, float pitch) {
         if (voiceName == null) {
            return null;
         }

         SoundConfig voice = FaunaSoundHelper.voice(body, voiceName);
         SoundEvent sound = voice == null ? null : voice.sound();
         return sound == null
            ? null
            : new ChannelVoice.Take(sound, body, voice.volume(), Float.isNaN(pitch) ? voice.pitchFor(body.getRandom()) : pitch, looping);
      }

      void fadeTo(float share, int ticks) {
         this.target = this.full * share;
         if (ticks <= 0) {
            this.volume = this.target;
            this.step = 0.0F;
         } else {
            this.step = (this.target - this.volume) / ticks;
         }
      }

      public boolean isLooping() {
         return this.looping;
      }

      public boolean canStartSilent() {
         return true;
      }

      public void tick() {
         super.tick();
         if (this.step != 0.0F) {
            this.volume = this.volume + this.step;
            if (this.step > 0.0F ? this.volume >= this.target : this.volume <= this.target) {
               this.volume = this.target;
               this.step = 0.0F;
               if (this.target <= 0.0F) {
                  this.stop();
               }
            }
         }
      }
   }
}

package net.magister.liberanimalium.fauna.behavior;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Gullet;
import net.minecraft.world.phys.Vec3;

public final class Pilfer implements Behavior, Laddered {
   @Nullable
   private UUID victim;
   @Nullable
   private Vec3 victimAt;
   private Pilfer.Rung rung = Pilfer.Rung.SIDLE;
   private final Flee escape = new Flee();

   public void target(@Nullable UUID victim, @Nullable Vec3 victimAt) {
      this.victim = victim;
      this.victimAt = victimAt;
   }

   @Override
   public String rungName() {
      return this.rung.name().toLowerCase(Locale.ROOT);
   }

   @Override
   public void start(AnimalHandle handle, MoveVerbs move, long now) {
      this.rung = Pilfer.Rung.SIDLE;
   }

   @Override
   public void tick(AnimalHandle handle, MoveVerbs move, long now) {
      Optional<Gullet> gullet = handle.endowed(Gullet.class);
      if (!gullet.isEmpty()) {
         if (gullet.get().loot() != null) {
            if (this.victimAt != null && handle.bodyPosition().distanceTo(this.victimAt) < 12.0) {
               this.rung = Pilfer.Rung.ESCAPE;
               this.escape.avoid(List.of(this.victimAt));
               this.escape.tick(handle, move, now);
            } else {
               if (this.rung == Pilfer.Rung.ESCAPE) {
                  this.escape.stop(handle, move, now);
               }

               this.rung = Pilfer.Rung.FEAST;
               move.stop();
               gullet.get().eatLoot();
            }
         } else if (this.victim != null && this.victimAt != null) {
            handle.applyLook(this.victimAt);
            double reach = handle.bounds().getXsize() + 1.0;
            double ring = reach * 2.0;
            if (handle.bodyPosition().distanceTo(this.victimAt) <= ring) {
               this.rung = Pilfer.Rung.SNATCH;
               if (!gullet.get().snatch(this.victim)) {
                  move.chase(this.victim, 0.0, reach, 1.0F);
               }
            } else {
               this.rung = Pilfer.Rung.SIDLE;
               move.stalk(this.victim, reach, ring, 0.45F);
            }
         }
      }
   }

   @Override
   public void stop(AnimalHandle handle, MoveVerbs move, long now) {
      this.escape.stop(handle, move, now);
      handle.applyLook(null);
      this.victim = null;
      this.victimAt = null;
      this.rung = Pilfer.Rung.SIDLE;
   }

   public enum Rung {
      SIDLE,
      SNATCH,
      ESCAPE,
      FEAST;
   }
}

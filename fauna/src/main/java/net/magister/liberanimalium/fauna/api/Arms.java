package net.magister.liberanimalium.fauna.api;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;

public interface Arms {
   Arms UNARMED = new Arms() {
      @Override
      public double strikeRange() {
         return 0.0;
      }

      @Override
      public boolean strike(UUID targetId) {
         return false;
      }

      @Override
      public Armament armament() {
         return Armament.NONE;
      }

      @Override
      public void holdTrigger(boolean pressed) {
      }

      @Override
      public boolean canAimAt(Vec3 point) {
         return false;
      }

      @Override
      public boolean electAbility(@Nullable UUID engagedWith, boolean spent, boolean fleeing, int committedTicks) {
         return false;
      }
   };

   static Arms of(AnimalHandle handle) {
      return handle.endowed(Arms.class).orElse(UNARMED);
   }

   double strikeRange();

   boolean strike(UUID var1);

   default boolean shove(UUID targetId) {
      return false;
   }

   Armament armament();

   void holdTrigger(boolean var1);

   default void dropTrigger() {
      this.holdTrigger(false);
   }

   boolean canAimAt(Vec3 var1);

   boolean electAbility(@Nullable UUID var1, boolean var2, boolean var3, int var4);
}

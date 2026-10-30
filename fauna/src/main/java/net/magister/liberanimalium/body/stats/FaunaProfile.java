package net.magister.liberanimalium.body.stats;

import java.util.EnumMap;
import java.util.Map;
import net.magister.liberanimalium.body.locomotion.LocomotionMode;

public final class FaunaProfile {
   public final boolean canFly;
   public final boolean canBeRidden;
   public final boolean canJump;
   public final boolean opensDoors;
   public final boolean canPerch;
   public final boolean tameable;
   public final boolean armed;
   public final boolean eats;
   private final EnumMap<LocomotionMode, Float> locomotion;

   private FaunaProfile(FaunaProfile.Builder b) {
      this.canFly = b.canFly;
      this.canBeRidden = b.canBeRidden;
      this.canJump = b.canJump;
      this.opensDoors = b.opensDoors;
      this.canPerch = b.canPerch;
      this.tameable = b.tameable;
      this.armed = b.armed;
      this.eats = b.eats;
      this.locomotion = new EnumMap<>(b.locomotion);
   }

   public boolean hasMode(LocomotionMode mode) {
      return this.locomotion.containsKey(mode);
   }

   public float proficiency(LocomotionMode mode) {
      Float v = this.locomotion.get(mode);
      return v != null ? v : 0.0F;
   }

   public static FaunaProfile.Builder builder() {
      return new FaunaProfile.Builder();
   }

   public static final class Builder {
      private boolean canFly = false;
      private boolean canBeRidden = false;
      private boolean canJump = false;
      private boolean opensDoors = false;
      private boolean canPerch = false;
      private boolean tameable = false;
      private boolean armed = true;
      private boolean eats = true;
      private final Map<LocomotionMode, Float> locomotion = new EnumMap<>(LocomotionMode.class);

      private Builder() {
      }

      public FaunaProfile.Builder canFly(boolean v) {
         this.canFly = v;
         return this;
      }

      public FaunaProfile.Builder canBeRidden(boolean v) {
         this.canBeRidden = v;
         return this;
      }

      public FaunaProfile.Builder canJump(boolean v) {
         this.canJump = v;
         return this;
      }

      public FaunaProfile.Builder opensDoors(boolean v) {
         this.opensDoors = v;
         return this;
      }

      public FaunaProfile.Builder canPerch(boolean v) {
         this.canPerch = v;
         return this;
      }

      public FaunaProfile.Builder tameable(boolean v) {
         this.tameable = v;
         return this;
      }

      public FaunaProfile.Builder armed(boolean v) {
         this.armed = v;
         return this;
      }

      public FaunaProfile.Builder eats(boolean v) {
         this.eats = v;
         return this;
      }

      public FaunaProfile.Builder locomotion(LocomotionMode mode, float proficiency) {
         this.locomotion.put(mode, proficiency);
         return this;
      }

      public FaunaProfile build() {
         if (this.locomotion.isEmpty()) {
            this.locomotion.put(LocomotionMode.WALK, 1.0F);
            this.locomotion.put(LocomotionMode.SWIM, 0.5F);
         }

         if (this.canFly) {
            this.locomotion.putIfAbsent(LocomotionMode.FLY, 1.0F);
         }

         if (this.locomotion.containsKey(LocomotionMode.DIVE)) {
            this.locomotion.putIfAbsent(LocomotionMode.SWIM, this.locomotion.get(LocomotionMode.DIVE));
         }

         return new FaunaProfile(this);
      }
   }
}

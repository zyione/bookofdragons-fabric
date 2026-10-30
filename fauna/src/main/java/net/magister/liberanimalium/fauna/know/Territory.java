package net.magister.liberanimalium.fauna.know;

import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.identity.Temper;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Territory implements Faculty {
   private static final Set<String> DWELLING_BEHAVIORS = Set.of("wander", "rest", "social", "idle", "forage");
   @Nullable
   private Vec3 homeCenter = null;
   @Nullable
   private Vec3 homeNow = null;
   private float radius = 0.0F;
   @Nullable
   private BlockPos roostAnchor = null;
   @Nullable
   private Vec3 avoidedCenter = null;
   @Nullable
   private Vec3 avoidedNow = null;
   @Nullable
   private AnimalHandle body;
   private float avoidedRadius = 0.0F;
   private long exileExpiryTick = 0L;

   @Override
   public String id() {
      return "territory";
   }

   @Override
   public void integrate(FacultyContext ctx) {
      this.tick(ctx.handle(), ctx.temper(), null, ctx.now());
   }

   public static float territoryRadius(float dominance) {
      float t = Mth.clamp(dominance, 0.0F, 1.0F);
      return Mth.lerp(t, 16.0F, 64.0F);
   }

   public static float homeAdvantage(double distance, float radius) {
      return radius <= 0.0F ? 0.0F : (float)Mth.clamp(1.0 - distance / radius, 0.0, 1.0);
   }

   public static boolean withinClaim(Vec3 home, Vec3 pos, float radius) {
      return pos.distanceToSqr(home) <= (double)radius * radius;
   }

   public static Vec3 drift(Vec3 home, Vec3 pos, boolean dwelling) {
      return !dwelling
         ? home
         : new Vec3(
            Mth.lerp(0.05F, home.x, pos.x),
            Mth.lerp(0.05F, home.y, pos.y),
            Mth.lerp(0.05F, home.z, pos.z)
         );
   }

   public static boolean isExiled(boolean hasRecord, double distanceToCenter, float radius, long expiry, long now) {
      return hasRecord && now < expiry && distanceToCenter < radius;
   }

   public void tick(AnimalHandle handle, Temper temper, @Nullable String activeBehavior, long gameTime) {
      this.body = handle;
      this.resolve(handle);
      boolean isTerritorial = handle.species().social().territorial();
      if (!isTerritorial) {
         this.radius = 0.0F;
         this.homeCenter = this.roostAnchor == null ? null : Vec3.atCenterOf(this.roostAnchor);
         this.homeNow = this.homeCenter;
      } else {
         float baseRadius = territoryRadius(temper.get(Trait.DOMINANCE));
         if (this.avoidedCenter != null) {
            if (gameTime >= this.exileExpiryTick) {
               this.avoidedCenter = null;
               this.avoidedNow = null;
               this.avoidedRadius = 0.0F;
               this.exileExpiryTick = 0L;
               this.settle(handle, handle.bodyPosition());
               this.radius = baseRadius;
            } else {
               this.radius = 0.0F;
            }
         }

         if (this.homeCenter == null && this.avoidedCenter == null) {
            this.settle(handle, handle.bodyPosition());
            this.radius = baseRadius;
         }

         if (this.roostAnchor != null) {
            this.homeCenter = Vec3.atCenterOf(this.roostAnchor);
            this.homeNow = this.homeCenter;
            this.radius = baseRadius;
         } else {
            if (this.homeNow != null && this.radius > 0.0F && this.avoidedCenter == null) {
               boolean dwelling = activeBehavior != null && DWELLING_BEHAVIORS.contains(activeBehavior.toLowerCase());
               boolean inside = withinClaim(this.homeNow, handle.bodyPosition(), this.radius);
               if (dwelling && inside) {
                  this.settle(handle, drift(this.homeNow, handle.bodyPosition(), true));
               }
            }
         }
      }
   }

   private void settle(AnimalHandle handle, Vec3 home) {
      this.homeCenter = handle.keepPlace(home);
      this.homeNow = home;
   }

   private void resolve(AnimalHandle handle) {
      this.homeNow = this.homeCenter == null ? null : handle.placeNow(this.homeCenter);
      if (this.homeNow == null) {
         this.homeCenter = null;
      }

      this.avoidedNow = this.avoidedCenter == null ? null : handle.placeNow(this.avoidedCenter);
      if (this.avoidedCenter != null && this.avoidedNow == null) {
         this.avoidedCenter = null;
         this.avoidedRadius = 0.0F;
         this.exileExpiryTick = 0L;
      }
   }

   public float homeAdvantage(Vec3 pos) {
      return this.homeNow != null && !(this.radius <= 0.0F) ? homeAdvantage(this.homeNow.distanceTo(pos), this.radius) : 0.0F;
   }

   public boolean isExiledFrom(Vec3 pos, long now) {
      return this.avoidedNow != null && now < this.exileExpiryTick
         ? isExiled(true, this.avoidedNow.distanceTo(pos), this.avoidedRadius, this.exileExpiryTick, now)
         : false;
   }

   public boolean isExileActive(long now) {
      return this.avoidedCenter != null && now < this.exileExpiryTick;
   }

   public void setExile(Vec3 winnerCenter, float winnerRadius, long expiryTick) {
      this.avoidedCenter = this.body == null ? winnerCenter : this.body.keepPlace(winnerCenter);
      this.avoidedNow = winnerCenter;
      this.avoidedRadius = winnerRadius;
      this.exileExpiryTick = expiryTick;
      this.radius = 0.0F;
   }

   @Nullable
   public Vec3 homeCenter() {
      return this.homeNow;
   }

   public float radius() {
      return this.radius;
   }

   @Nullable
   public BlockPos roostAnchor() {
      return this.roostAnchor;
   }

   public void setRoostAnchor(@Nullable BlockPos anchor) {
      this.roostAnchor = anchor;
   }

   @Nullable
   public Vec3 avoidedCenter() {
      return this.avoidedNow;
   }

   public float avoidedRadius() {
      return this.avoidedRadius;
   }

   public long exileExpiryTick() {
      return this.exileExpiryTick;
   }

   public long exileRemaining(long now) {
      return this.avoidedCenter == null ? 0L : Math.max(0L, this.exileExpiryTick - now);
   }
}

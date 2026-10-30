package net.magister.liberanimalium.body;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.MoveIntent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public final class GroundGait {
   private final Mob self;
   private final AnimalHandle handle;
   @Nullable
   private MoveIntent intent;

   public GroundGait(Mob self, AnimalHandle handle) {
      this.self = self;
      this.handle = handle;
   }

   public void apply(MoveIntent intent) {
      this.intent = intent;
   }

   @Nullable
   public MoveIntent intent() {
      return this.intent;
   }

   public Vec3 drive(Vec3 riderInput) {
      MoveIntent asked = this.intent;
      if (asked != null && asked.moving()) {
         Vec3 flat = new Vec3(asked.direction().x, 0.0, asked.direction().z);
         if (flat.lengthSqr() > 1.0E-6) {
            float wanted = (float)(Mth.atan2(flat.z, flat.x) * 180.0F / (float)Math.PI) - 90.0F;
            float cap = (float)this.handle.turnRateDegrees(this.self.getAttributeValue(Attributes.MOVEMENT_SPEED) * asked.speed());
            float step = Mth.clamp(Mth.wrapDegrees(wanted - this.self.getYRot()), -cap, cap);
            this.self.setYRot(this.self.getYRot() + step);
            this.self.yBodyRot = this.self.getYRot();
            this.self.yHeadRot = this.self.getYRot();
         }

         this.self.setSpeed((float)(this.self.getAttributeValue(Attributes.MOVEMENT_SPEED) * asked.speed()));
         return new Vec3(0.0, riderInput.y, 1.0);
      } else {
         this.self.setSpeed(0.0F);
         return Vec3.ZERO;
      }
   }
}

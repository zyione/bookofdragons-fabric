package net.magister.liberanimalium.body.grip;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.FaunaBase;

public interface Held {
   @Nullable
   FaunaBase liberanimalium$holder();

   void liberanimalium$setHolder(@Nullable FaunaBase var1);

   Held.Pin liberanimalium$pin();

   final class Pin {
      public double x;
      public double y;
      public double z;
      public double xO;
      public double yO;
      public double zO;
      public double vx;
      public double vy;
      public double vz;
      public float yaw;
      public float yawO;
      public boolean placed;
      public int thrown;
      public float drag = 1.0F;
      public double flyX;
      public double flyZ;
   }
}

package net.magister.liberanimalium.ride.render;

import java.util.List;
import java.util.Locale;

public final class ArmPose {
   public static final List<String> FIELDS = List.of("x", "y", "z", "yaw", "pitch", "roll");
   private final float[] defaults;
   public float x;
   public float y;
   public float z;
   public float yaw;
   public float pitch;
   public float roll;

   public ArmPose(float x, float y, float z, float yaw, float pitch, float roll) {
      this.defaults = new float[]{x, y, z, yaw, pitch, roll};
      this.reset();
   }

   public void reset() {
      this.x = this.defaults[0];
      this.y = this.defaults[1];
      this.z = this.defaults[2];
      this.yaw = this.defaults[3];
      this.pitch = this.defaults[4];
      this.roll = this.defaults[5];
   }

   public String describe() {
      return String.format(Locale.ROOT, "x %.3f  y %.3f  z %.3f  yaw %.1f  pitch %.1f  roll %.1f", this.x, this.y, this.z, this.yaw, this.pitch, this.roll);
   }

   public float get(String field) {
      return switch (field) {
         case "x" -> this.x;
         case "y" -> this.y;
         case "z" -> this.z;
         case "yaw" -> this.yaw;
         case "pitch" -> this.pitch;
         case "roll" -> this.roll;
         default -> Float.NaN;
      };
   }

   public boolean set(String field, float value) {
      switch (field) {
         case "x":
            this.x = value;
            break;
         case "y":
            this.y = value;
            break;
         case "z":
            this.z = value;
            break;
         case "yaw":
            this.yaw = value;
            break;
         case "pitch":
            this.pitch = value;
            break;
         case "roll":
            this.roll = value;
            break;
         default:
            return false;
      }

      return true;
   }
}

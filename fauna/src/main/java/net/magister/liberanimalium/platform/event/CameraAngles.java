package net.magister.liberanimalium.platform.event;

import net.minecraft.client.Camera;

public final class CameraAngles {
   private final Camera camera;
   private final float partialTick;
   private float yaw;
   private float pitch;
   private float roll;

   public CameraAngles(Camera camera, float partialTick, float yaw, float pitch, float roll) {
      this.camera = camera;
      this.partialTick = partialTick;
      this.yaw = yaw;
      this.pitch = pitch;
      this.roll = roll;
   }

   public Camera getCamera() {
      return this.camera;
   }

   public float getPartialTick() {
      return this.partialTick;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public float getRoll() {
      return this.roll;
   }

   public void setRoll(float roll) {
      this.roll = roll;
   }
}

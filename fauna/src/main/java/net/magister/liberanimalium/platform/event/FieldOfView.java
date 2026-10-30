package net.magister.liberanimalium.platform.event;

import net.minecraft.client.Camera;

public final class FieldOfView {
   private final Camera camera;
   private final float partialTick;
   private double fov;

   public FieldOfView(Camera camera, float partialTick, double fov) {
      this.camera = camera;
      this.partialTick = partialTick;
      this.fov = fov;
   }

   public Camera getCamera() {
      return this.camera;
   }

   public float getPartialTick() {
      return this.partialTick;
   }

   public double getFOV() {
      return this.fov;
   }

   public void setFOV(double fov) {
      this.fov = fov;
   }
}

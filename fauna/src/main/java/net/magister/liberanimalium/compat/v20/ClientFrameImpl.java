package net.magister.liberanimalium.compat.v20;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

public final class ClientFrameImpl {
   private ClientFrameImpl() {
   }

   public static float partialTick() {
      return Minecraft.getInstance().getFrameTime();
   }

   public static float frameTime() {
      return Minecraft.getInstance().getFrameTime();
   }

   public static float frameDeltaTicks() {
      return Minecraft.getInstance().getDeltaFrameTime();
   }

   public static Matrix3f inverseViewRotation() {
      return RenderSystem.getInverseViewRotationMatrix();
   }

   public static Quaternionf billboard(Camera camera) {
      return new Quaternionf(camera.rotation());
   }

   public static float billboardRoll(float roll) {
      return roll;
   }
}

package net.magister.liberanimalium.compat;

import net.magister.liberanimalium.compat.v20.ClientFrameImpl;
import net.minecraft.client.Camera;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

public final class ClientFrame {
   private ClientFrame() {
   }

   public static float partialTick() {
      return ClientFrameImpl.partialTick();
   }

   public static float frameTime() {
      return ClientFrameImpl.frameTime();
   }

   public static float frameDeltaTicks() {
      return ClientFrameImpl.frameDeltaTicks();
   }

   public static Matrix3f inverseViewRotation() {
      return ClientFrameImpl.inverseViewRotation();
   }

   public static Quaternionf billboard(Camera camera) {
      return ClientFrameImpl.billboard(camera);
   }

   public static float billboardRoll(float roll) {
      return ClientFrameImpl.billboardRoll(roll);
   }
}

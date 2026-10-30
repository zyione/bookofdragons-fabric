package net.magister.liberanimalium.render.gpu;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;

public abstract class GpuParticle extends Particle {
   protected GpuParticle(ClientLevel level, double x, double y, double z) {
      super(level, x, y, z);
   }

   protected GpuParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
      super(level, x, y, z, xd, yd, zd);
   }

   protected abstract void write(GpuParticles.Quad var1);

   public final void render(VertexConsumer buffer, Camera camera, float partialTicks) {
   }
}

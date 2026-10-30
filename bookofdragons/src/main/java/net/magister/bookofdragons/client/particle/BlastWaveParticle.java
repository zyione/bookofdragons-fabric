package net.magister.bookofdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.magister.liberanimalium.compat.ClientFrame;
import net.magister.liberanimalium.compat.Verts;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BlastWaveParticle extends TextureSheetParticle {
   private final SpriteSet sprites;

   BlastWaveParticle(ClientLevel world, double x, double y, double z, SpriteSet spriteProvider, double power) {
      super(world, x, y, z, 0.0, 0.0, 0.0);
      this.sprites = spriteProvider;
      this.lifetime = 8;
      this.quadSize = (float)power;
      this.setSpriteFromAge(spriteProvider);
   }

   public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
      Vec3 camPos = camera.getPosition();
      float particleX = (float)(Mth.lerp(partialTicks, this.xo, this.x) - camPos.x());
      float particleY = (float)(Mth.lerp(partialTicks, this.yo, this.y) - camPos.y());
      float particleZ = (float)(Mth.lerp(partialTicks, this.zo, this.z) - camPos.z());
      Quaternionf cameraInverseRotation = ClientFrame.billboard(camera);
      cameraInverseRotation.conjugate();
      Vector3f[] corners = new Vector3f[]{
         new Vector3f(-1.0F, 0.0F, -1.0F), new Vector3f(-1.0F, 0.0F, 1.0F), new Vector3f(1.0F, 0.0F, 1.0F), new Vector3f(1.0F, 0.0F, -1.0F)
      };
      float size = this.getQuadSize(partialTicks);

      for (int i = 0; i < 4; i++) {
         Vector3f corner = corners[i];
         corner.mul(size);
         corner.rotate(cameraInverseRotation);
         float u = i != 2 && i != 3 ? this.getU0() : this.getU1();
         float v = i != 1 && i != 2 ? this.getV0() : this.getV1();
         Verts.particle(
            buffer,
            particleX + corner.x(),
            particleY + corner.y(),
            particleZ + corner.z(),
            u,
            v,
            this.rCol,
            this.gCol,
            this.bCol,
            this.alpha,
            240,
            240
         );
      }
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.setSpriteFromAge(this.sprites);
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet spriteSet) {
         this.sprites = spriteSet;
      }

      public Particle createParticle(SimpleParticleType particleType, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
         return new BlastWaveParticle(level, x, y, z, this.sprites, dx);
      }
   }
}

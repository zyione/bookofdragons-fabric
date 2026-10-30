package net.magister.liberanimalium.attack.sim;

import net.magister.liberanimalium.attack.data.EmitterSim;
import net.magister.liberanimalium.attack.data.render.RenderConfig;

public final class BreathParticle {
   public double x;
   public double y;
   public double z;
   public double px;
   public double py;
   public double pz;
   public double vx;
   public double vy;
   public double vz;
   public int age;
   public int life;
   public float temperature;
   public float rot;
   public float oRot;
   public float rotSpeed;
   public float scale = 1.0F;
   public int emitterIndex;
   public long seed;
   public boolean removed;
   public float delay;
   public float initialDelay;
   public float activeFraction = 1.0F;
   public EmitterSim sim;
   public RenderConfig render;

   public void setPos(double nx, double ny, double nz) {
      this.x = this.px = nx;
      this.y = this.py = ny;
      this.z = this.pz = nz;
   }

   public void setVel(double nvx, double nvy, double nvz) {
      this.vx = nvx;
      this.vy = nvy;
      this.vz = nvz;
   }

   public BreathRandom random() {
      return new BreathRandom(this.seed);
   }

   public float lifeFraction() {
      return this.life <= 0 ? 1.0F : Math.min(1.0F, (float)this.age / this.life);
   }
}

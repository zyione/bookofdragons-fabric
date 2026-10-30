package net.magister.liberanimalium.body.grip;

import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class GripHang {
   private Vec3 bob = Vec3.ZERO;
   private Vec3 bobO = Vec3.ZERO;
   private Vec3 velocity = Vec3.ZERO;
   private Vec3 hips = Vec3.ZERO;
   private Vec3 hipsO = Vec3.ZERO;
   private Vec3 rest = Vec3.ZERO;
   private Vec3 restO = Vec3.ZERO;
   private final Quaternionf restTurn = new Quaternionf();
   private final Quaternionf restTurnO = new Quaternionf();
   private final Vector3f limitDown = new Vector3f(0.0F, -1.0F, 0.0F);
   private final Vector3f limitRight = new Vector3f(1.0F, 0.0F, 0.0F);
   private boolean started;
   private int stepped;

   public void start(Vec3 grip, Vec3 hips) {
      this.bob = this.bobO = grip;
      this.velocity = Vec3.ZERO;
      this.hips = this.hipsO = hips;
      this.rest = this.restO = grip;
      this.stepped = 0;
      this.started = true;
   }

   public boolean started() {
      return this.started;
   }

   public Vec3 pivot(float partial) {
      return this.bobO.lerp(this.bob, partial);
   }

   public Vec3 hips(float partial) {
      return this.hipsO.lerp(this.hips, partial);
   }

   public Quaternionf turn(float partial) {
      Quaternionf turn = new Quaternionf(this.restTurnO).slerp(this.restTurn, partial);
      Vec3 h = this.hips(partial);
      Vec3 still = this.restO.lerp(this.rest, partial).subtract(h);
      Vec3 line = this.pivot(partial).subtract(h);
      if (!(line.lengthSqr() < 1.0E-8) && !(still.lengthSqr() < 1.0E-8)) {
         Quaternionf swing = new Quaternionf()
            .rotationTo(
               new Vector3f((float)still.x, (float)still.y, (float)still.z),
               new Vector3f((float)line.x, (float)line.y, (float)line.z)
            );
         return swing.mul(turn);
      } else {
         return turn;
      }
   }

   public Vec3 carrier() {
      return this.hips.subtract(this.hipsO);
   }

   public Vec3 velocity() {
      return this.velocity;
   }

   public void step(Vec3 hips, Vec3 rest, Quaternionfc restTurn, Vector3fc down, Vector3fc right, PhysicsSheet.Grip physics, GripSpec limits) {
      if (!this.started) {
         this.start(rest, hips);
      }

      boolean jumped = this.stepped > 0 && hips.distanceToSqr(this.hips) > 256.0;
      if (jumped) {
         this.start(rest, hips);
      }

      Vec3 hipsWas = this.hips;
      Vec3 restWas = this.rest;
      Vector3f downWas = new Vector3f(this.limitDown);
      Vector3f rightWas = new Vector3f(this.limitRight);
      if (this.stepped == 0) {
         hipsWas = hips;
         restWas = rest;
         this.restTurn.set(restTurn);
         downWas.set(down);
         rightWas.set(right);
      } else if (this.stepped == 1) {
         this.velocity = hips.subtract(hipsWas);
      }

      this.stepped++;
      this.bobO = this.bob;
      this.hipsO = hipsWas;
      this.restO = restWas;
      this.restTurnO.set(this.restTurn);
      this.hips = hips;
      this.rest = rest;
      this.restTurn.set(restTurn);
      this.limitDown.set(down);
      this.limitRight.set(right);
      Vec3 carrier = hips.subtract(hipsWas);
      double rate = physics.swingRate();
      double stiff = rate * rate;
      double damp = 2.0 * physics.swingDamping() * rate;
      double dt = 0.25;
      Vector3f stepDown = new Vector3f();
      Vector3f stepRight = new Vector3f();

      for (int i = 0; i < 4; i++) {
         double share = (i + 1) * dt;
         Vec3 from = hipsWas.lerp(hips, share);
         Vec3 held = restWas.lerp(rest, share);
         downWas.lerp(down, (float)share, stepDown);
         rightWas.lerp(right, (float)share, stepRight);
         Vec3 pull = restWas.lerp(rest, i * dt).subtract(this.bob).scale(stiff).subtract(this.velocity.subtract(carrier).scale(damp));
         this.velocity = this.velocity.add(pull.scale(dt));
         Vec3 next = this.bob.add(this.velocity.scale(dt));
         double length = held.distanceTo(from);
         Vec3 line = next.subtract(from);
         if (length > 1.0E-6 && line.lengthSqr() > 1.0E-12) {
            next = from.add(limited(line.normalize(), stepDown, stepRight, limits).scale(length));
         } else {
            next = held;
         }

         this.velocity = next.subtract(this.bob).scale(4.0);
         this.bob = next;
      }
   }

   static Vec3 limited(Vec3 direction, Vector3fc down, Vector3fc right, GripSpec limits) {
      Vec3 d = new Vec3(down.x(), down.y(), down.z()).normalize();
      Vec3 t = d.cross(new Vec3(right.x(), right.y(), right.z()));
      if (t.lengthSqr() < 1.0E-12) {
         return direction;
      }

      t = t.normalize();
      Vec3 s = t.cross(d);
      double a = direction.dot(d);
      double b = direction.dot(t);
      double c = direction.dot(s);
      double pitch = Math.atan2(b, a);
      double side = Math.atan2(c, Math.sqrt(a * a + b * b));
      double p = Math.max(-Math.toRadians(limits.forward()), Math.min(Math.toRadians(limits.back()), pitch));
      double widest = Math.toRadians(limits.side());
      double q = Math.max(-widest, Math.min(widest, side));
      return p == pitch && q == side
         ? direction
         : d.scale(Math.cos(q) * Math.cos(p)).add(t.scale(Math.cos(q) * Math.sin(p))).add(s.scale(Math.sin(q)));
   }
}

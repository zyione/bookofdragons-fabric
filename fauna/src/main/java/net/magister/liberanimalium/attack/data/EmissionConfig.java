package net.magister.liberanimalium.attack.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public record EmissionConfig(String shape, float radius, List<Float> spread, int count, int rateTicks, String direction, float flare, String axis) {
   public static final String AXIS_FIRE = "fire";
   public static final String AXIS_UP = "up";
   public static final String DIR_FORWARD = "forward";
   public static final String DIR_INWARD = "inward";
   public static final String DIR_OUTWARD = "outward";
   public static final String DIR_NONE = "none";
   public static final EmissionConfig POINT_SINGLE = new EmissionConfig("point", 0.0F, List.of(0.0F, 0.0F, 0.0F), 1, 1, "forward");
   public static final Codec<EmissionConfig> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.STRING.optionalFieldOf("shape", "point").forGetter(EmissionConfig::shape),
            Codec.FLOAT.optionalFieldOf("radius", 0.5F).forGetter(EmissionConfig::radius),
            Codec.FLOAT.listOf().optionalFieldOf("spread", List.of(0.0F, 0.0F, 0.0F)).forGetter(EmissionConfig::spread),
            Codec.INT.optionalFieldOf("count", 1).forGetter(EmissionConfig::count),
            Codec.INT.optionalFieldOf("rate_ticks", 1).forGetter(EmissionConfig::rateTicks),
            Codec.STRING.optionalFieldOf("direction", "forward").forGetter(EmissionConfig::direction),
            Codec.FLOAT.optionalFieldOf("flare", 0.0F).forGetter(EmissionConfig::flare),
            Codec.STRING.optionalFieldOf("axis", "fire").forGetter(EmissionConfig::axis)
         )
         .apply(i, EmissionConfig::new)
   );

   public EmissionConfig(String shape, float radius, List<Float> spread, int count, int rateTicks, String direction) {
      this(shape, radius, spread, count, rateTicks, direction, 0.0F);
   }

   public EmissionConfig(String shape, float radius, List<Float> spread, int count, int rateTicks, String direction, float flare) {
      this(shape, radius, spread, count, rateTicks, direction, flare, "fire");
   }

   private Vec3 axisOf(Vec3 fireDirection) {
      return "up".equals(this.axis) ? new Vec3(0.0, 1.0, 0.0) : fireDirection;
   }

   public EmissionConfig.OffsetSample sample(Vec3 fireDirection, RandomSource random, float speed) {
      Vec3 direction = this.axisOf(fireDirection);

      Vec3 base = switch (this.shape) {
         case "sphere" -> this.sampleSphere(random);
         case "disc" -> this.sampleDisc(direction, random);
         case "ring" -> this.sampleRing(direction, random);
         case "line" -> this.sampleLine(direction, random);
         default -> Vec3.ZERO;
      };
      float sx = this.spread.size() > 0 ? this.spread.get(0) : 0.0F;
      float sy = this.spread.size() > 1 ? this.spread.get(1) : 0.0F;
      float sz = this.spread.size() > 2 ? this.spread.get(2) : 0.0F;
      double jx = (random.nextDouble() - 0.5) * 2.0 * sx;
      double jy = (random.nextDouble() - 0.5) * 2.0 * sy;
      double r3 = random.nextDouble();
      Vec3[] frame = basis(direction);
      if ("box".equals(this.shape)) {
         double jz = (r3 - 0.5) * 2.0 * sz;
         Vec3 offset = base.add(frame[0].scale(jx)).add(frame[1].scale(jy)).add(frame[2].scale(jz));
         return new EmissionConfig.OffsetSample(offset, 0.0F);
      } else {
         float delay = sz > 0.0F && speed > 0.001F ? (float)(r3 * sz / speed) : 0.0F;
         Vec3 offset = base.add(frame[0].scale(jx)).add(frame[1].scale(jy));
         return new EmissionConfig.OffsetSample(offset, delay);
      }
   }

   public Vec3 sampleOffset(Vec3 direction, RandomSource random) {
      return this.sample(direction, random, 0.0F).offset();
   }

   public Vec3 sampleVelocity(Vec3 offset, Vec3 fireDirection, float speed) {
      fireDirection = this.axisOf(fireDirection);

      Vec3 axis = switch (this.direction) {
         case "inward" -> offset.scale(-1.0);
         case "outward" -> offset;
         case "none" -> Vec3.ZERO;
         default -> fireDirection;
      };
      Vec3 launch = axis.lengthSqr() < 1.0E-8 ? Vec3.ZERO : axis.normalize().scale(speed);
      if (this.flare != 0.0F && "forward".equals(this.direction) && launch != Vec3.ZERO) {
         Vec3 fwd = axis.normalize();
         Vec3 radial = offset.subtract(fwd.scale(offset.dot(fwd)));
         return radial.lengthSqr() < 1.0E-8 ? launch : launch.add(radial.normalize().scale(speed * this.flare));
      } else {
         return launch;
      }
   }

   private Vec3 sampleSphere(RandomSource random) {
      double theta = random.nextDouble() * Math.PI * 2.0;
      double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
      double r = this.radius * Math.cbrt(random.nextDouble());
      double sinPhi = Math.sin(phi);
      return new Vec3(r * sinPhi * Math.cos(theta), r * sinPhi * Math.sin(theta), r * Math.cos(phi));
   }

   private Vec3 sampleLine(Vec3 direction, RandomSource random) {
      return basis(direction)[0].scale((random.nextDouble() * 2.0 - 1.0) * this.radius);
   }

   private Vec3 sampleRing(Vec3 direction, RandomSource random) {
      Vec3[] frame = basis(direction);
      double angle = random.nextDouble() * Math.PI * 2.0;
      return frame[0].scale(Math.cos(angle) * this.radius).add(frame[1].scale(Math.sin(angle) * this.radius));
   }

   private Vec3 sampleDisc(Vec3 direction, RandomSource random) {
      Vec3[] frame = basis(direction);
      double angle = random.nextDouble() * Math.PI * 2.0;
      double r = this.radius * Math.sqrt(random.nextDouble());
      return frame[0].scale(Math.cos(angle) * r).add(frame[1].scale(Math.sin(angle) * r));
   }

   private static Vec3[] basis(Vec3 direction) {
      Vec3 fwd = direction.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
      Vec3 ref = Math.abs(fwd.y) > 0.99 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 across = fwd.cross(ref).normalize();
      return new Vec3[]{across, across.cross(fwd).normalize(), fwd};
   }

   public record OffsetSample(Vec3 offset, float delay) {
   }
}

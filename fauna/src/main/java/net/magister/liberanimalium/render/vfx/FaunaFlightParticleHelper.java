package net.magister.liberanimalium.render.vfx;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.magister.liberanimalium.attack.client.particle.FaunaParticles;
import net.magister.liberanimalium.body.state.RiderMotion;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.interop.SubLevels;
import net.magister.liberanimalium.render.waves.FaunaWaveManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class FaunaFlightParticleHelper {
   private static final double MAX_GROUND_DISTANCE = 16.0;
   private static final double MAX_WATER_DISTANCE = 8.0;
   private static final int BASE_PARTICLE_COUNT = 12;
   private static final float WASH_AMOUNT = 1.3F;
   private static final double WASH_RADIUS = 1.1;
   private static final Int2ObjectOpenHashMap<Vec3> PREV_WATER_POS = new Int2ObjectOpenHashMap();
   private static final double BURST_GROUND_REACH = 3.0;
   public static final Map<Integer, Float> BODY_SPEED_SCALES = new ConcurrentHashMap<>();

   public static void spawnWaterTrailParticles(FaunaBase body) {
      Level level = body.level();
      if (level.isClientSide() && body.animRig().vfx().flightWake()) {
         if (body.isFlying()) {
            Vec3 velocity = RiderMotion.of(body);
            double speed = velocity.horizontalDistance();
            Vec3 bodyPos = body.position().add(0.0, body.getBbHeight() * 0.5, 0.0);
            FaunaFlightParticleHelper.SurfaceDetectionResult waterResult = detectSurfaceBelow(level, bodyPos, 8.0, FaunaFlightParticleHelper.SurfaceType.WATER);
            if (!waterResult.foundSurface) {
               PREV_WATER_POS.remove(body.getId());
            } else {
               Vec3 curWaterPos = waterResult.hitPos;
               if (speed > 0.08) {
                  Vec3 prevWaterPos = (Vec3)PREV_WATER_POS.get(body.getId());
                  if (prevWaterPos != null && prevWaterPos.distanceTo(curWaterPos) < 24.0) {
                     float swathWidth = body.getBbWidth() * 0.9F;
                     float closeness = (float)Math.max(0.15, 1.0 - waterResult.distance / 8.0);
                     float depth = 0.45F * closeness;
                     FaunaWaveManager.getInstance().addWakeSlice(prevWaterPos, curWaterPos, swathWidth, depth, speed);
                     WaterSpray.wake(level, body, curWaterPos, velocity, speed, (float)Math.max(0.0, 1.0 - waterResult.distance / 8.0));
                  } else {
                     FaunaWaveManager.getInstance().addWaveForce(curWaterPos.x, curWaterPos.y, curWaterPos.z, 0.2F);
                  }

                  PREV_WATER_POS.put(body.getId(), curWaterPos);
               } else {
                  PREV_WATER_POS.put(body.getId(), curWaterPos);
                  if (body.tickCount % 6 == 0) {
                     FaunaWaveManager.getInstance()
                        .addRadialForce(curWaterPos.x, curWaterPos.y, curWaterPos.z, body.getBbWidth() * 0.35F, 0.25F);
                  }
               }

               boolean isFastEnough = false;
               if (body.isVehicle()) {
                  isFastEnough = body.getThrottleLevel() >= 0.85F;
               } else {
                  float baseSpeed = (float)body.getAttributeValue(Attributes.FLYING_SPEED);
                  isFastEnough = speed >= baseSpeed * 0.85F;
               }

               if (isFastEnough) {
                  spawnWaterTrail(level, waterResult.hitPos, body, velocity);
                  spawnSonicBarrierWaves(level, waterResult.hitPos, body, velocity, speed);
               }
            }
         }
      }
   }

   public static void spawnWingWashParticles(FaunaBase body, String side) {
      Level level = body.level();
      if (level.isClientSide() && body.animRig().vfx().flightWake()) {
         if (body.isFlying()) {
            Vec3 bodyPos = body.position().add(0.0, body.getBbHeight() * 0.5, 0.0);
            FaunaFlightParticleHelper.SurfaceDetectionResult waterResult = detectSurfaceBelow(level, bodyPos, 8.0, FaunaFlightParticleHelper.SurfaceType.WATER);
            if (waterResult.foundSurface) {
               spawnSurfaceParticles(level, waterResult, body, side);
               spawnWingFlapCircularWave(body, waterResult.hitPos, side);
               WaterSpray.beat(level, body, underWing(body, waterResult.hitPos, side), (float)Math.max(0.0, 1.0 - waterResult.distance / 8.0));
            } else {
               FaunaFlightParticleHelper.SurfaceDetectionResult groundResult = detectSurfaceBelow(
                  level, bodyPos, 16.0, FaunaFlightParticleHelper.SurfaceType.GROUND
               );
               if (groundResult.foundSurface) {
                  spawnSurfaceParticles(level, groundResult, body, side);
               }
            }
         }
      }
   }

   private static Vec3 underWing(FaunaBase body, Vec3 waterPos, String side) {
      float yawRad = (float)Math.toRadians(body.getYRot());
      Vec3 right = new Vec3(Math.cos(yawRad), 0.0, Math.sin(yawRad));
      float wingOffsetDist = body.getBbWidth() * 0.45F;
      return "left".equals(side) ? waterPos.subtract(right.scale(wingOffsetDist)) : waterPos.add(right.scale(wingOffsetDist));
   }

   private static void spawnWingFlapCircularWave(FaunaBase body, Vec3 waterPos, String side) {
      Vec3 wingWaterPos = underWing(body, waterPos, side);
      float bodyScale = 1.0F + body.getGrowthStage() * 0.35F;
      float radius = Math.max(1.0F, body.getBbWidth() * 0.4F);
      FaunaWaveManager.getInstance().addRadialForce(wingWaterPos.x, wingWaterPos.y, wingWaterPos.z, radius, 1.4F * bodyScale);
   }

   private static FaunaFlightParticleHelper.SurfaceDetectionResult detectSurfaceBelow(
      Level level, Vec3 startPos, double maxDistance, FaunaFlightParticleHelper.SurfaceType surfaceType
   ) {
      Vec3 endPos = startPos.add(0.0, -maxDistance, 0.0);
      Fluid fluidMode = surfaceType == FaunaFlightParticleHelper.SurfaceType.WATER ? Fluid.SOURCE_ONLY : Fluid.NONE;
      ClipContext context = new ClipContext(startPos, endPos, Block.COLLIDER, fluidMode, null);
      BlockHitResult hitResult = level.clip(context);
      if (hitResult.getType() == Type.BLOCK) {
         BlockPos hitPos = hitResult.getBlockPos();
         BlockState blockState = level.getBlockState(hitPos);
         boolean isValidSurface = false;
         if (surfaceType == FaunaFlightParticleHelper.SurfaceType.WATER) {
            isValidSurface = blockState.getFluidState().is(FluidTags.WATER);
         } else {
            isValidSurface = !blockState.isAir()
               && (!blockState.getCollisionShape(level, hitPos).isEmpty() || blockState.is(BlockTags.LEAVES) || blockState.is(BlockTags.SNOW));
         }

         if (isValidSurface) {
            Vec3 surface = SubLevels.toWorld(level, hitResult.getLocation());
            double distance = startPos.distanceTo(surface);
            if (distance <= maxDistance) {
               return new FaunaFlightParticleHelper.SurfaceDetectionResult(true, surface, surfaceType, distance, maxDistance);
            }
         }
      }

      return new FaunaFlightParticleHelper.SurfaceDetectionResult(false, null, surfaceType, 0.0, maxDistance);
   }

   private static void spawnSurfaceParticles(Level level, FaunaFlightParticleHelper.SurfaceDetectionResult result, FaunaBase body, String side) {
      double distance = result.distance;
      float intensity = Mth.clamp((float)(1.0 - distance / result.maxDistance), 0.0F, 1.0F);
      intensity *= intensity;
      int particleCount = (int)(12.0F * intensity);
      if (particleCount > 0) {
         float stageMultiplier = 1.0F + body.getGrowthStage() * 0.3F;
         particleCount = (int)(particleCount * stageMultiplier);
         if (result.surfaceType == FaunaFlightParticleHelper.SurfaceType.WATER) {
            spawnWaterParticles(level, result.hitPos, particleCount, body);
         } else {
            spawnGroundParticles(level, result.hitPos, Math.round(particleCount * 1.3F), body);
         }
      }
   }

   private static void spawnGroundParticles(Level level, Vec3 surfacePos, int count, FaunaBase body) {
      WingWashGround ground = WingWashGround.of(level, BlockPos.containing(surfacePos.x, surfacePos.y - 0.1, surfacePos.z));
      RandomSource random = body.getRandom();
      double radius = body.getBbWidth() * 1.1;
      int dust = Math.round(count * ground.dustShare);

      for (int i = 0; i < dust; i++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         double distance = (0.25 + random.nextDouble() * 0.75) * radius * 0.6;
         double offsetX = Math.cos(angle) * distance;
         double offsetZ = Math.sin(angle) * distance;
         level.addParticle(
            (ParticleOptions)FaunaParticles.WING_DUST.get(),
            surfacePos.x + offsetX,
            surfacePos.y + 0.4,
            surfacePos.z + offsetZ,
            offsetX * 0.42 + (random.nextDouble() - 0.5) * 0.08,
            0.0,
            offsetZ * 0.42 + (random.nextDouble() - 0.5) * 0.08
         );
      }

      int debris = Math.round(count * ground.debrisShare);

      for (int i = 0; i < debris; i++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         double distance = random.nextDouble() * radius;
         double dirX = Math.cos(angle);
         double dirZ = Math.sin(angle);
         double out = ground.outMin + random.nextDouble() * ground.outSpan;
         level.addParticle(
            (ParticleOptions)FaunaParticles.WING_DEBRIS.get(),
            surfacePos.x + dirX * distance,
            surfacePos.y + 0.15,
            surfacePos.z + dirZ * distance,
            dirX * out,
            ground.upMin + random.nextDouble() * ground.upSpan,
            dirZ * out
         );
      }
   }

   public static void burst(FaunaBase body, int count) {
      Level level = body.level();
      if (level.isClientSide() && count > 0) {
         Vec3 feet = body.position();
         FaunaFlightParticleHelper.SurfaceDetectionResult ground = detectSurfaceBelow(
            level, feet.add(0.0, 0.5, 0.0), 3.0, FaunaFlightParticleHelper.SurfaceType.GROUND
         );
         if (ground.foundSurface) {
            spawnGroundParticles(level, ground.hitPos, count, body);
         }

         double half = body.getBbWidth() * 0.5;
         Vec3 centre = feet.add(0.0, body.getBbHeight() * 0.5, 0.0);
         float yaw = body.getYRot() * (float) (Math.PI / 180.0);
         Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
         Vec3[] sides = new Vec3[]{ahead, new Vec3(ahead.z, 0.0, -ahead.x), new Vec3(-ahead.z, 0.0, ahead.x), ahead.scale(-1.0)};

         for (Vec3 side : sides) {
            BlockPos at = BlockPos.containing(centre.add(side.scale(half + 0.5)));
            BlockState state = level.getBlockState(at);
            if (!state.isAir() && !state.getCollisionShape(level, at).isEmpty()) {
               spawnWallParticles(level, centre.add(side.scale(half)), side.scale(-1.0), count, body, WingWashGround.of(level, at));
               return;
            }
         }
      }
   }

   private static void spawnWallParticles(Level level, Vec3 face, Vec3 out, int count, FaunaBase body, WingWashGround ground) {
      RandomSource random = body.getRandom();
      double across = body.getBbWidth() * 0.5;
      double tall = body.getBbHeight() * 0.5;
      Vec3 along = new Vec3(out.z, 0.0, -out.x);
      int dust = Math.round(count * ground.dustShare);
      int debris = Math.round(count * ground.debrisShare);

      for (int i = 0; i < dust + debris; i++) {
         double a = (random.nextDouble() * 2.0 - 1.0) * across;
         double h = (random.nextDouble() * 2.0 - 1.0) * tall;
         Vec3 at = face.add(along.scale(a)).add(0.0, h, 0.0);
         double speed = ground.outMin + random.nextDouble() * ground.outSpan;
         if (i < dust) {
            level.addParticle(
               (ParticleOptions)FaunaParticles.WING_DUST.get(),
               at.x,
               at.y,
               at.z,
               out.x * speed * 0.6 + along.x * a * 0.1,
               0.0,
               out.z * speed * 0.6 + along.z * a * 0.1
            );
         } else {
            level.addParticle(
               (ParticleOptions)FaunaParticles.WING_DEBRIS.get(),
               at.x,
               at.y,
               at.z,
               out.x * speed + along.x * a * 0.1,
               ground.upMin + random.nextDouble() * ground.upSpan,
               out.z * speed + along.z * a * 0.1
            );
         }
      }
   }

   private static void spawnWaterParticles(Level level, Vec3 surfacePos, int count, FaunaBase body) {
      int waterCount = (int)(count * 1.5);
      spawnRadialParticles(level, surfacePos, waterCount, body, ParticleTypes.SPLASH, 0.35, 0.25, 0.2);
   }

   private static void spawnWaterTrail(Level level, Vec3 waterPos, FaunaBase body, Vec3 velocity) {
      float bodyWidth = body.getBbWidth();
      Vec3 normalizedVelocity = velocity.normalize();
      Vec3 perpendicular = new Vec3(-normalizedVelocity.z, 0.0, normalizedVelocity.x);
      int particlesAcrossWidth = 7;

      for (int i = 0; i < particlesAcrossWidth; i++) {
         double t = (double)i / (particlesAcrossWidth - 1) - 0.5;
         double offsetDistance = t * bodyWidth * 0.8;
         Vec3 offset = perpendicular.scale(offsetDistance);
         double spawnX = waterPos.x + offset.x;
         double spawnY = waterPos.y + 0.1;
         double spawnZ = waterPos.z + offset.z;
         double velocityX = -normalizedVelocity.x * 0.1 + (body.getRandom().nextDouble() - 0.5) * 0.1;
         double velocityY = 0.15 + body.getRandom().nextDouble() * 0.1;
         double velocityZ = -normalizedVelocity.z * 0.1 + (body.getRandom().nextDouble() - 0.5) * 0.1;
         if (i % 2 == 0) {
            level.addParticle(ParticleTypes.SPLASH, spawnX, spawnY, spawnZ, velocityX, velocityY, velocityZ);
         } else {
            level.addParticle(ParticleTypes.SPLASH, spawnX, spawnY, spawnZ, velocityX, velocityY, velocityZ);
         }
      }

      for (int i = 0; i < 5; i++) {
         double randomOffset = (body.getRandom().nextDouble() - 0.5) * bodyWidth * 0.8;
         Vec3 offset = perpendicular.scale(randomOffset);
         double spawnX = waterPos.x + offset.x;
         double spawnY = waterPos.y + 0.1;
         double spawnZ = waterPos.z + offset.z;
         double velocityX = (body.getRandom().nextDouble() - 0.5) * 0.2;
         double velocityY = 0.2 + body.getRandom().nextDouble() * 0.15;
         double velocityZ = (body.getRandom().nextDouble() - 0.5) * 0.2;
         level.addParticle(ParticleTypes.SPLASH, spawnX, spawnY, spawnZ, velocityX, velocityY, velocityZ);
      }
   }

   private static void spawnSonicBarrierWaves(Level level, Vec3 waterPos, FaunaBase body, Vec3 velocity, double speed) {
      float baseSpeed = (float)body.getAttributeValue(Attributes.FLYING_SPEED);
      float rawScale = (float)((speed - baseSpeed * 0.5F) / (baseSpeed * 0.5F));
      float speedScale = Mth.clamp(rawScale, 0.1F, 1.0F);
      BODY_SPEED_SCALES.put(body.getId(), speedScale);
      double baseAngle = Math.toRadians(30.0 + speed * 10.0);
      baseAngle = Math.min(baseAngle, Math.toRadians(50.0));
      Vec3 normalizedVelocity = velocity.normalize();
      Vec3 perpendicular = new Vec3(-normalizedVelocity.z, 0.0, normalizedVelocity.x);
      float bodyWidth = body.getBbWidth();
      double outwardSpeed = speed * Math.tan(baseAngle) * 0.4;

      for (int side = -1; side <= 1; side += 2) {
         Vec3 sideOffset = perpendicular.scale(bodyWidth * 0.5 * side);
         int particlesToSpawn = 2 + (int)(4.0 * speedScale);

         for (int i = 0; i < particlesToSpawn; i++) {
            double backwardScatter = body.getRandom().nextDouble() * body.getDeltaMovement().length() * 1.5;
            Vec3 scatterOffset = normalizedVelocity.scale(-backwardScatter);
            double jitterX = (body.getRandom().nextDouble() - 0.5) * bodyWidth * 0.6 * speedScale;
            double jitterZ = (body.getRandom().nextDouble() - 0.5) * bodyWidth * 0.6 * speedScale;
            double spawnX = waterPos.x + sideOffset.x + scatterOffset.x + jitterX;
            double spawnY = waterPos.y - 0.4 * speedScale + (body.getRandom().nextDouble() - 0.5) * 0.2;
            double spawnZ = waterPos.z + sideOffset.z + scatterOffset.z + jitterZ;
            double randomVelScale = 0.5 + body.getRandom().nextDouble() * 1.0;
            double velX = perpendicular.x * side * outwardSpeed * randomVelScale + (body.getRandom().nextDouble() - 0.5) * 0.3;
            double velY = 0.05 + body.getRandom().nextDouble() * 0.15 * speedScale;
            double velZ = perpendicular.z * side * outwardSpeed * randomVelScale + (body.getRandom().nextDouble() - 0.5) * 0.3;
            level.addParticle(ParticleTypes.SPLASH, spawnX, spawnY, spawnZ, velX * 1.5, velY * 1.5, velZ * 1.5);
         }
      }
   }

   private static void spawnRadialParticles(
      Level level, Vec3 surfacePos, int count, FaunaBase body, ParticleOptions particleType, double velocityScale, double randomness, double upwardVelocity
   ) {
      float bodyWidth = body.getBbWidth();
      double spawnRadius = bodyWidth * 0.8;

      for (int i = 0; i < count; i++) {
         double angle = body.getRandom().nextDouble() * Math.PI * 2.0;
         double distance = body.getRandom().nextDouble() * spawnRadius;
         double offsetX = Math.cos(angle) * distance;
         double offsetZ = Math.sin(angle) * distance;
         double spawnX = surfacePos.x + offsetX;
         double spawnY = surfacePos.y + 0.1;
         double spawnZ = surfacePos.z + offsetZ;
         double velocityX = offsetX * velocityScale;
         double velocityY = upwardVelocity;
         double velocityZ = offsetZ * velocityScale;
         velocityX += (body.getRandom().nextDouble() - 0.5) * randomness;
         velocityZ += (body.getRandom().nextDouble() - 0.5) * randomness;
         level.addParticle(particleType, spawnX, spawnY, spawnZ, velocityX, velocityY, velocityZ);
      }
   }

   private record SurfaceDetectionResult(
      boolean foundSurface, Vec3 hitPos, FaunaFlightParticleHelper.SurfaceType surfaceType, double distance, double maxDistance
   ) {
   }

   private enum SurfaceType {
      GROUND,
      WATER;
   }
}

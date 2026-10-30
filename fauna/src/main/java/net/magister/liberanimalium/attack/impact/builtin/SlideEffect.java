package net.magister.liberanimalium.attack.impact.builtin;

import net.magister.liberanimalium.attack.data.ImpactConfig;
import net.magister.liberanimalium.attack.data.impact.SlideImpact;
import net.magister.liberanimalium.attack.entity.AttackProjectileEntity;
import net.magister.liberanimalium.attack.impact.ImpactEffect;
import net.magister.liberanimalium.config.WorldRules;
import net.magister.liberanimalium.interop.SubLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class SlideEffect implements ImpactEffect {
   public static Vec3 reflect(Vec3 velocity, Vec3 faceNormal, float bounciness) {
      double dot = velocity.dot(faceNormal);
      Vec3 normalVel = faceNormal.scale(dot);
      Vec3 tangentVel = velocity.subtract(normalVel);
      return tangentVel.subtract(normalVel.scale(bounciness));
   }

   @Override
   public boolean apply(AttackProjectileEntity projectile, HitResult hit, ImpactConfig config) {
      if (hit != null && hit.getType() == Type.BLOCK) {
         SlideImpact slide = (SlideImpact)config;
         int bouncesLeft = projectile.getSlideBouncesRemaining();
         if (bouncesLeft < 0) {
            bouncesLeft = slide.maxBounces();
         }

         BlockHitResult blockHit = (BlockHitResult)hit;
         Vec3 faceNormal = SubLevels.turnOut(projectile.level(), Vec3.atCenterOf(blockHit.getBlockPos()), Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()));
         Vec3 velocity = projectile.getDeltaMovement();
         double dot = velocity.dot(faceNormal);
         if (dot < -0.05) {
            if (bouncesLeft <= 0) {
               projectile.discard();
               return true;
            }

            bouncesLeft--;
         }

         projectile.setDeltaMovement(reflect(velocity, faceNormal, slide.bounciness()));
         if (!projectile.level().isClientSide && slide.setsFire() && WorldRules.maySetFire(projectile.level())) {
            BlockPos adjacentPos = blockHit.getBlockPos().relative(blockHit.getDirection());
            if (projectile.level().getBlockState(adjacentPos).isAir()) {
               projectile.level().setBlock(adjacentPos, Blocks.FIRE.defaultBlockState(), 11);
            }
         }

         projectile.setSlideBouncesRemaining(bouncesLeft);
         return true;
      } else {
         projectile.discard();
         return true;
      }
   }
}

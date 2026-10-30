package net.magister.liberanimalium.anim.rig;

import java.util.function.UnaryOperator;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class EntityFooting implements Footing {
   private final Entity entity;

   public EntityFooting(Entity entity) {
      this.entity = entity;
   }

   @Override
   public double x() {
      return this.entity.getX();
   }

   @Override
   public double y() {
      return this.entity.getY();
   }

   @Override
   public double z() {
      return this.entity.getZ();
   }

   @Override
   public double yo() {
      return this.entity.yo;
   }

   @Override
   public Vec3 groundOld() {
      return new Vec3(this.entity.xo, this.entity.yo, this.entity.zo);
   }

   @Override
   public float yaw() {
      return this.entity instanceof LivingEntity living ? living.yBodyRot : this.entity.getYRot();
   }

   @Override
   public float yawO() {
      return this.entity instanceof LivingEntity living ? living.yBodyRotO : this.entity.yRotO;
   }

   @Override
   public boolean onGround() {
      return this.entity.onGround();
   }

   @Override
   public boolean held(double drop) {
      return LevelGroundOracle.INSTANCE.held(this.entity, drop);
   }

   @Override
   public double height() {
      return this.entity.getBbHeight();
   }

   @Override
   public GroundOracle oracle() {
      return LevelGroundOracle.INSTANCE;
   }

   @Nullable
   @Override
   public UnaryOperator<Vec3> ride() {
      return null;
   }

   @Override
   public Vec3 drawn(Vec3 world, float partialTick) {
      return world;
   }

   @Override
   public Matrix4f modelToStand(Matrix4f modelToWorld, float partialTick) {
      return modelToWorld;
   }

   @Override
   public Vec3 toWorld(Vec3 point) {
      return point;
   }

   @Override
   public Vec3 position() {
      return this.entity.position();
   }
}

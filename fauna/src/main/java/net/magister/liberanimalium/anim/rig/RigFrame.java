package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonObject;
import mod.azure.azurelib.model.AzBakedModel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class RigFrame {
   public final RiggedBody body;
   public final AzBakedModel model;
   public final RigWrite write;
   public final RigSpec spec;
   public final StepsSpec steps;
   public final float partialTick;
   public final Matrix4f modelToWorld;
   public final Vec3 drawnPosition;
   private Footing footing;
   private Matrix4f modelToStand;
   private Vec3 drawnStand;

   public Footing footing() {
      if (this.footing == null && this.body != null) {
         this.footing = this.body.animFooting();
      }

      return this.footing;
   }

   public Matrix4f modelToStand() {
      if (this.modelToStand == null) {
         Footing foot = this.footing();
         this.modelToStand = foot == null ? this.modelToWorld : foot.modelToStand(this.modelToWorld, this.partialTick);
      }

      return this.modelToStand;
   }

   public Vec3 drawnStand() {
      if (this.drawnStand == null) {
         Footing foot = this.footing();
         this.drawnStand = foot == null ? this.drawnPosition : foot.drawn(this.drawnPosition, this.partialTick);
      }

      return this.drawnStand;
   }

   public RigFrame(RiggedBody body, AzBakedModel model, RigWrite write, RigSpec spec, float partialTick) {
      this(body, model, write, spec, partialTick, new Matrix4f(), Vec3.ZERO);
   }

   public RigFrame(RiggedBody body, AzBakedModel model, RigWrite write, RigSpec spec, float partialTick, Matrix4f modelToWorld, Vec3 drawnPosition) {
      this.body = body;
      this.model = model;
      this.write = write;
      this.spec = spec;
      this.steps = body == null ? StepsSpec.EMPTY : body.animSteps();
      this.partialTick = partialTick;
      this.modelToWorld = modelToWorld;
      this.drawnPosition = drawnPosition;
   }

   public JsonObject config(String pass) {
      return this.spec.config(pass);
   }
}

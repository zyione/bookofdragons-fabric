package net.magister.liberanimalium.anim.rig;

import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;

public final class AzBoneSink implements BoneSink {
   private AzBakedModel model;

   @Override
   public void bind(AzBakedModel model) {
      this.model = model;
   }

   @Override
   public boolean has(String bone) {
      return this.model != null && this.model.getBoneOrNull(bone) != null;
   }

   @Override
   public float read(String bone, BoneAxis axis) {
      AzBone b = this.model == null ? null : this.model.getBoneOrNull(bone);
      if (b == null) {
         return 0.0F;
      }

      return switch (axis) {
         case ROT_X -> b.getRotX();
         case ROT_Y -> b.getRotY();
         case ROT_Z -> b.getRotZ();
         case POS_X -> b.getPosX();
         case POS_Y -> b.getPosY();
         case POS_Z -> b.getPosZ();
         case SCALE_X -> b.getScaleX();
         case SCALE_Y -> b.getScaleY();
         case SCALE_Z -> b.getScaleZ();
      };
   }

   @Override
   public void write(String bone, BoneAxis axis, float value) {
      AzBone b = this.model == null ? null : this.model.getBoneOrNull(bone);
      if (b != null) {
         switch (axis) {
            case ROT_X:
               b.setRotX(value);
               break;
            case ROT_Y:
               b.setRotY(value);
               break;
            case ROT_Z:
               b.setRotZ(value);
               break;
            case POS_X:
               b.setPosX(value);
               break;
            case POS_Y:
               b.setPosY(value);
               break;
            case POS_Z:
               b.setPosZ(value);
               break;
            case SCALE_X:
               b.setScaleX(value);
               break;
            case SCALE_Y:
               b.setScaleY(value);
               break;
            case SCALE_Z:
               b.setScaleZ(value);
         }
      }
   }
}

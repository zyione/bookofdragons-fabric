package net.magister.liberanimalium.anim.rig;

import mod.azure.azurelib.model.AzBakedModel;

public interface BoneSink {
   float read(String var1, BoneAxis var2);

   void write(String var1, BoneAxis var2, float var3);

   boolean has(String var1);

   default void bind(AzBakedModel model) {
   }
}

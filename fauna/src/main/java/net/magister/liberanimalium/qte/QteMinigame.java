package net.magister.liberanimalium.qte;

import net.minecraft.util.RandomSource;

public interface QteMinigame {
   QteType type();

   void start(RandomSource var1);

   QteOutcome tick(QteInput var1);

   float score();

   QteRenderState renderState();
}

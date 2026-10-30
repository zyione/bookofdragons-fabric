package net.magister.liberanimalium.chassis.component.qte;

import net.magister.liberanimalium.qte.QteOutcome;

@FunctionalInterface
public interface QteHandler {
   void accept(QteOutcome var1, float var2);
}

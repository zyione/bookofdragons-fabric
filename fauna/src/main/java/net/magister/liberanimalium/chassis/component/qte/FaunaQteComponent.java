package net.magister.liberanimalium.chassis.component.qte;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.IFaunaComponent;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.network.SyncQtePacket;
import net.magister.liberanimalium.qte.QteMinigame;
import net.magister.liberanimalium.qte.QteOutcome;

public class FaunaQteComponent implements IFaunaComponent {
   private FaunaBase body;
   @Nullable
   private QteMinigame session;
   @Nullable
   private QteHandler handler;

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   public boolean isActive() {
      return this.session != null;
   }

   public void begin(QteMinigame game, QteHandler handler) {
      if (!this.body.level().isClientSide()) {
         this.session = game;
         this.handler = handler;
         game.start(this.body.getRandom());
         this.body.setQteActive(true);
         this.sync();
      }
   }

   @Override
   public void serverTick() {
      if (this.session != null) {
         QteOutcome outcome = this.session.tick(QteInputSource.read(this.body));
         this.sync();
         if (outcome != QteOutcome.RUNNING) {
            QteHandler finished = this.handler;
            float score = this.session.score();
            this.end();
            if (finished != null) {
               finished.accept(outcome, score);
            }
         }
      }
   }

   private void end() {
      this.session = null;
      this.handler = null;
      this.body.setQteActive(false);
      FaunaNetwork.sendToClientsTracking(new SyncQtePacket(this.body.getId(), false, null), this.body);
   }

   private void sync() {
      if (this.session != null) {
         FaunaNetwork.sendToClientsTracking(new SyncQtePacket(this.body.getId(), true, this.session.renderState()), this.body);
      }
   }
}

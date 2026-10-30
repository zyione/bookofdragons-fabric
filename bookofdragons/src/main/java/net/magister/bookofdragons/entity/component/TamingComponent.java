package net.magister.bookofdragons.entity.component;

import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.CommandComponent;
import net.magister.liberanimalium.chassis.component.IFaunaComponent;
import net.magister.liberanimalium.chassis.component.command.Modifier;
import net.magister.liberanimalium.chassis.component.command.Stance;
import net.minecraft.nbt.CompoundTag;

public class TamingComponent implements IFaunaComponent {
   protected FaunaBase dragon;

   public void onAttach(FaunaBase host) {
      this.dragon = host;
   }

   private DragonBase body() {
      return (DragonBase)this.dragon;
   }

   public void serverTick() {
      if (this.body().isAwaitingTamingRitual() && !this.body().isTamingRitualCompleted()) {
         this.failTamingRitual();
      }
   }

   public void save(CompoundTag tag) {
      tag.putBoolean("AwaitingTamingRitual", this.body().isAwaitingTamingRitual());
      tag.putInt("TamingRitualTimer", this.body().getTamingRitualTimer());
      tag.putBoolean("TamingRitualCompleted", this.body().isTamingRitualCompleted());
   }

   public void load(CompoundTag tag) {
      if (tag.contains("TamingRitualCompleted")) {
         this.body().setAwaitingTamingRitual(tag.getBoolean("AwaitingTamingRitual"));
         this.body().setTamingRitualTimer(tag.getInt("TamingRitualTimer"));
         this.body().setTamingRitualCompleted(tag.getBoolean("TamingRitualCompleted"));
      }
   }

   public void failTamingRitual() {
      this.body().setAwaitingTamingRitual(false);
      this.body().setTamingRitualTimer(0);
      this.dragon.setOwnerUUID(null);
      ((CommandComponent)this.dragon.componentRegistry.get(CommandComponent.class)).setCommandForced(Stance.FREE, Modifier.GROUND);
   }
}

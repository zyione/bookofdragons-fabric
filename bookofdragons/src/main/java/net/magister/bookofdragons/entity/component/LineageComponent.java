package net.magister.bookofdragons.entity.component;

import java.util.Optional;
import java.util.UUID;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.liberanimalium.body.stats.BondLevel;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.CommandComponent;
import net.magister.liberanimalium.chassis.component.IFaunaComponent;
import net.magister.liberanimalium.chassis.component.command.Modifier;
import net.magister.liberanimalium.chassis.component.command.Stance;
import net.minecraft.nbt.CompoundTag;

public class LineageComponent implements IFaunaComponent {
   protected FaunaBase dragon;

   public void onAttach(FaunaBase host) {
      this.dragon = host;
   }

   public void serverTick() {
   }

   public void save(CompoundTag tag) {
      tag.putBoolean("IsFromTamedParents", this.isFromTamedParents());
      this.getTamedLineageOwner().ifPresent(uuid -> tag.putUUID("TamedLineageOwner", uuid));
   }

   public void load(CompoundTag tag) {
      this.setFromTamedParents(tag.getBoolean("IsFromTamedParents"));
      if (tag.hasUUID("TamedLineageOwner")) {
         this.setTamedLineageOwner(tag.getUUID("TamedLineageOwner"));
      }
   }

   public boolean isFromTamedParents() {
      return (Boolean)this.dragon.getEntityData().get(DragonBase.getIsFromTamedParentsData());
   }

   public void setFromTamedParents(boolean fromTamedParents) {
      this.dragon.getEntityData().set(DragonBase.getIsFromTamedParentsData(), fromTamedParents);
   }

   public Optional<UUID> getTamedLineageOwner() {
      return (Optional<UUID>)this.dragon.getEntityData().get(DragonBase.DATA_TAMED_LINEAGE_OWNER);
   }

   public void setTamedLineageOwner(UUID ownerUUID) {
      this.dragon.getEntityData().set(DragonBase.DATA_TAMED_LINEAGE_OWNER, Optional.of(ownerUUID));
   }

   public void forgetOwner() {
      this.setFromTamedParents(false);
      this.dragon.getEntityData().set(DragonBase.DATA_TAMED_LINEAGE_OWNER, Optional.empty());
   }

   public void handleTamedLineage() {
      if (this.isFromTamedParents()) {
         this.getTamedLineageOwner().ifPresent(ownerUUID -> {
            if (this.dragon.getGrowthStage() == 1 && this.dragon.getBond(ownerUUID) < BondLevel.CURIOUS.getMinAffection()) {
               this.dragon.setBond(ownerUUID, BondLevel.TOLERANT.getMinAffection());
            } else if (this.dragon.getGrowthStage() == 2 && !this.dragon.isTame()) {
               this.dragon.setBond(ownerUUID, BondLevel.CURIOUS.getMinAffection());
               this.dragon.setOwnerUUID(ownerUUID);
               this.dragon.getNavigation().stop();
               this.dragon.level().broadcastEntityEvent(this.dragon, (byte)7);
               ((CommandComponent)this.dragon.componentRegistry.get(CommandComponent.class)).setCommandForced(Stance.FOLLOW, Modifier.TRAIL);
            }
         });
      }
   }
}

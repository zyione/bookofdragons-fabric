package net.magister.liberanimalium.anim.rig;

import net.magister.liberanimalium.anim.event.EventTable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface RigOnlyBody extends RiggedBody {
   @Override
   default String animClipPrefix() {
      return "animation." + this.animSpeciesId() + ".";
   }

   @Override
   default boolean hasClip(String fullClip) {
      return false;
   }

   @Override
   default void playClip(String layer, String fullClip) {
   }

   @Override
   default void playVoice(String name) {
   }

   @Override
   default Vec3 animPosition() {
      Entity entity = this.animEntity();
      return entity == null ? Vec3.ZERO : entity.position();
   }

   @Override
   default Level animLevel() {
      Entity entity = this.animEntity();
      return entity == null ? null : entity.level();
   }

   @Override
   default EventTable animEvents() {
      return EventTable.EMPTY;
   }

   @Override
   default int animTick() {
      Entity entity = this.animEntity();
      return entity == null ? 0 : entity.tickCount;
   }
}

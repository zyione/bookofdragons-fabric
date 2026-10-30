package net.magister.liberanimalium.anim.api;

import net.magister.liberanimalium.anim.event.EventTable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface AnimatedBody {
   String animSpeciesId();

   String animClipPrefix();

   boolean hasClip(String var1);

   void playClip(String var1, String var2);

   void playVoice(String var1);

   Vec3 animPosition();

   default void animWash(int count) {
   }

   default Vec3 animBoneSpot(String bone) {
      return null;
   }

   Level animLevel();

   EventTable animEvents();
}

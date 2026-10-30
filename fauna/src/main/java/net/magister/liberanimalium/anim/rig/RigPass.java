package net.magister.liberanimalium.anim.rig;

public interface RigPass {
   String name();

   void solve(RigFrame var1);

   default void repeat(RigFrame frame) {
   }
}

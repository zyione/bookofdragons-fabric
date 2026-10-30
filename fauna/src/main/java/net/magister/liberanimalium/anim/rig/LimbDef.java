package net.magister.liberanimalium.anim.rig;

import java.util.List;
import java.util.Map;

public record LimbDef(
   String name,
   String root,
   String mid,
   String foot,
   String knee,
   float reachUp,
   float reachDown,
   boolean splay,
   boolean reach,
   float zone,
   List<String> anchors,
   Map<String, float[]> plantHigh,
   Map<String, float[]> plantLow
) {
   public static final float DEFAULT_REACH_UP = 1.0F;
   public static final float DEFAULT_REACH_DOWN = -1.5F;
}

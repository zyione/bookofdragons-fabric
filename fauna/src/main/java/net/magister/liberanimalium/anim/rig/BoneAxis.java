package net.magister.liberanimalium.anim.rig;

import java.util.Locale;

public enum BoneAxis {
   ROT_X,
   ROT_Y,
   ROT_Z,
   POS_X,
   POS_Y,
   POS_Z,
   SCALE_X,
   SCALE_Y,
   SCALE_Z;

   public static final int COUNT = values().length;

   public static BoneAxis byKey(String key) {
      if (key == null) {
         return null;
      }

      return switch (key.toLowerCase(Locale.ROOT)) {
         case "rx", "rotx", "rot_x" -> ROT_X;
         case "ry", "roty", "rot_y" -> ROT_Y;
         case "rz", "rotz", "rot_z" -> ROT_Z;
         case "tx", "posx", "pos_x" -> POS_X;
         case "ty", "posy", "pos_y" -> POS_Y;
         case "tz", "posz", "pos_z" -> POS_Z;
         case "sx", "scalex", "scale_x" -> SCALE_X;
         case "sy", "scaley", "scale_y" -> SCALE_Y;
         case "sz", "scalez", "scale_z" -> SCALE_Z;
         default -> null;
      };
   }
}

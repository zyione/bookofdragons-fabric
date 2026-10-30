package net.magister.liberanimalium.body.anim.binding;

import net.magister.liberanimalium.fauna.api.Stance;
import net.magister.liberanimalium.fauna.drive.Drives;
import net.minecraft.util.Mth;

public final class AnimSignalSync {
   private static final int TRAIT_BITS = 5;
   private static final int TRAIT_MAX = 31;
   public static final int TRAIT_SLOTS = 6;
   public static final int FLAG_BEGGING = 2;
   public static final int FLAG_EATING = 8;
   public static final int FLAG_REFUSING = 32;
   private static final int STANCE_SHIFT = 3;

   private AnimSignalSync() {
   }

   public static int activityIndex(String name) {
      return Drives.indexOf(name) + 1;
   }

   public static String activityName(int idx) {
      String id = Drives.at(idx - 1);
      return id == null ? "NONE" : id;
   }

   public static int packTraits(float[] traits) {
      int packed = 0;

      for (int i = 0; i < 6 && i < traits.length; i++) {
         int q = Mth.clamp(Math.round(traits[i] * 31.0F), 0, 31);
         packed |= q << i * 5;
      }

      return packed;
   }

   public static float traitOf(int packed, int ordinal) {
      return ordinal >= 0 && ordinal < 6 ? (packed >> ordinal * 5 & 31) / 31.0F : 0.0F;
   }

   public static int packDynamic(float wariness, int activityIdx, int flags) {
      return Mth.clamp(Math.round(wariness * 255.0F), 0, 255) | (activityIdx & 0xFF) << 16 | (flags & 0xFF) << 24;
   }

   public static float warinessOf(int p) {
      return (p & 0xFF) / 255.0F;
   }

   public static int activityOf(int p) {
      return p >> 16 & 0xFF;
   }

   public static boolean beggingOf(int p) {
      return (p >> 24 & 0xFF & 2) != 0;
   }

   public static boolean eatingOf(int p) {
      return (p >> 24 & 0xFF & 8) != 0;
   }

   public static boolean refusingOf(int p) {
      return (p >> 24 & 0xFF & 32) != 0;
   }

   public static int packStance(Stance stance) {
      return (stance.ordinal() & 7) << 3 << 16;
   }

   public static int packSocial(Stance stance, float regard) {
      int r = Math.round(Math.max(-1.0F, Math.min(1.0F, regard)) * 127.0F);
      return packStance(stance) | r & 0xFF;
   }

   public static float regardOf(int p) {
      return (byte)(p & 0xFF) / 127.0F;
   }

   public static Stance stanceOf(int p) {
      int ordinal = (p >> 16 & 0xFF) >> 3 & 7;
      Stance[] bands = Stance.values();
      return ordinal < bands.length ? bands[ordinal] : Stance.NONE;
   }

   public static String stanceName(int p) {
      return stanceOf(p).name();
   }
}

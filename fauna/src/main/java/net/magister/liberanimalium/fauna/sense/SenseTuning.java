package net.magister.liberanimalium.fauna.sense;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.fauna.api.SoundKind;

public final class SenseTuning {
   public static final float SEEN_THRESHOLD = 0.0F;
   public static final float PERIPHERAL_MIN_RATIO = 1.25F;
   public static final float ANGLE_PENALTY_MAX = 1.0F;
   public static final float BLIND_MAX_SHARE_OF_PERFECT = 0.6F;
   public static final float BLIND_PENALTY_MAX = 0.9F;
   public static final float DARKNESS_PENALTY_MAX = 0.8F;
   public static final float MAX_LIGHT_LEVEL = 15.0F;
   public static final float WATER_PENALTY = 0.35F;
   public static final float SNEAK_PENALTY = 0.45F;
   public static final float REFERENCE_VISUAL_SIZE = 1.08F;
   public static final float SMALLNESS_PENALTY_MAX = 0.3F;
   public static final Map<SoundKind, Float> LOUDNESS = new EnumMap<>(
      Map.of(
         SoundKind.COMBAT,
         24.0F,
         SoundKind.CONSTRUCTION,
         18.0F,
         SoundKind.VOCAL,
         20.0F,
         SoundKind.LOCOMOTION,
         12.0F,
         SoundKind.FORAGING,
         6.0F,
         SoundKind.CALL,
         128.0F,
         SoundKind.ROAR,
         64.0F
      )
   );
   public static final float KEENEST_EAR = 2.0F;
   public static final float SOUND_BLOCKS_PER_TICK = 17.0F;
   public static final int GLANCE_TICKS = 40;
   public static final int GLANCE_MIN_TICKS = 6;
   public static final float HEARD_FIGHT_MARK_CALM = 0.03F;
   public static final float HEARD_FIGHT_MARK_NERVOUS = 0.15F;
   public static final float HEARING_MOVE_SATURATION = 0.25F;
   public static final float HEARING_MOVE_PENALTY_MAX = 0.6F;
   public static final float SAME_SPECIES_HEARING = 0.1F;
   public static final int STIMULUS_INBOX = 16;
   public static final float HURT_FULL_STRENGTH_FRACTION = 0.25F;
   public static final float WEAPON_MENACE = 0.5F;
   public static final float SHIELD_MENACE = 0.25F;
   public static final float ITEM_MENACE = 0.15F;
   public static final float FULL_ARMOUR_VALUE = 20.0F;
   public static final float FAVOURITE_CRAVING = 1.0F;
   public static final float SPECIAL_CRAVING = 0.66F;
   public static final float GENERAL_CRAVING = 0.33F;
   public static final float CURIOSITY_CRAVING = 0.12F;
   public static final int ITEM_SCAN_CAP = 16;

   private SenseTuning() {
   }

   public static float maxLoudness() {
      float max = 0.0F;

      for (Entry<SoundKind, Float> entry : LOUDNESS.entrySet()) {
         if (entry.getKey() != SoundKind.CALL && entry.getKey() != SoundKind.ROAR) {
            max = Math.max(max, entry.getValue());
         }
      }

      return max;
   }

   public static int callRadius() {
      return (int)Math.ceil(LOUDNESS.get(SoundKind.VOCAL) * 2.0F);
   }

   public static float earOf(float hearingSkill) {
      return Math.max(1.0F, Math.min(hearingSkill, 2.0F));
   }
}

package net.magister.bookofdragons.client;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.client.KeyContext;
import net.magister.liberanimalium.platform.client.ClientServices.KeySink;
import net.magister.liberanimalium.ride.client.RideKeys;
import net.magister.liberanimalium.ride.client.RideKeys.Bound;
import net.minecraft.client.KeyMapping;

public final class KeyBindings {
   public static final String KEY_CATEGORY = "key.category.bookofdragons";
   public static final String KEY_CATEGORY_TAMING = "key.category.bookofdragons.taming";
   public static final KeyMapping OPEN_DRAGON_INVENTORY_KEY = key("open_inventory", 71);
   public static final KeyMapping DRAGON_SECONDARY_ABILITY_KEY = key("secondary_ability", 67);
   public static final KeyMapping DRAGON_DEBUG_TOGGLE_KEY = key("debug_toggle", 293);
   public static final KeyMapping DRAGON_SPRINT_KEY = key("dragon_sprint", 341);
   public static final KeyMapping DRAGON_PRIMARY_ABILITY_KEY = mouse("primary_ability", 0);
   public static final KeyMapping DRAGON_FREE_CAMERA_KEY = mouse("free_camera", 1);
   public static final KeyMapping DRAGON_DISMOUNT_KEY = key("dismount", 82);
   public static final KeyMapping DRAGON_GRAB_KEY = key("grab", 88);
   public static final KeyMapping ROOST_LINES_KEY = key("roost_lines", 295);
   private static final int[] TAMING_DEFAULT_CODES = new int[]{70, 71, 72, 74, 75, 76};
   public static final KeyMapping[] TAMING_KEYS = new KeyMapping[TAMING_DEFAULT_CODES.length];

   private KeyBindings() {
   }

   private static KeyMapping key(String name, int code) {
      return Services.client().key("key.bookofdragons." + name, KeyContext.IN_GAME, Type.KEYSYM, code, "key.category.bookofdragons");
   }

   private static KeyMapping mouse(String name, int button) {
      return Services.client().key("key.bookofdragons." + name, KeyContext.IN_GAME, Type.MOUSE, button, "key.category.bookofdragons");
   }

   public static void register(KeySink event) {
      event.register(OPEN_DRAGON_INVENTORY_KEY);
      event.register(DRAGON_SECONDARY_ABILITY_KEY);
      event.register(DRAGON_SPRINT_KEY);
      event.register(DRAGON_PRIMARY_ABILITY_KEY);
      event.register(DRAGON_FREE_CAMERA_KEY);
      event.register(DRAGON_DISMOUNT_KEY);
      event.register(DRAGON_GRAB_KEY);
      if (!Services.platform().isProduction()) {
         event.register(DRAGON_DEBUG_TOGGLE_KEY);
         event.register(ROOST_LINES_KEY);
      }

      for (KeyMapping letter : TAMING_KEYS) {
         event.register(letter);
      }

      RideKeys.bind(
         "bookofdragons",
         new Bound(
            DRAGON_SPRINT_KEY,
            DRAGON_PRIMARY_ABILITY_KEY,
            DRAGON_SECONDARY_ABILITY_KEY,
            DRAGON_FREE_CAMERA_KEY,
            DRAGON_DISMOUNT_KEY,
            DRAGON_DEBUG_TOGGLE_KEY,
            TAMING_KEYS,
            DRAGON_GRAB_KEY
         )
      );
   }

   static {
      for (int i = 0; i < TAMING_DEFAULT_CODES.length; i++) {
         TAMING_KEYS[i] = Services.client()
            .key("key.bookofdragons.taming_letter_" + (i + 1), RideKeys.STRUGGLE, Type.KEYSYM, TAMING_DEFAULT_CODES[i], "key.category.bookofdragons.taming");
      }
   }
}

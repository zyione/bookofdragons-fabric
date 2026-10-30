package net.magister.liberanimalium.ride.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.platform.client.KeyContext;
import net.magister.liberanimalium.qte.client.ClientQteState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public final class RideKeys {
   public static final RideKeys.Bound NONE = new RideKeys.Bound(null, null, null, null, null, null, new KeyMapping[0]);
   private static final Map<String, RideKeys.Bound> HOSTS = Collections.synchronizedMap(new LinkedHashMap<>());
   public static final KeyContext STRUGGLE = KeyContext.custom(() -> {
      LocalPlayer me = Minecraft.getInstance().player;
      return me != null && me.getVehicle() != null && ClientQteState.tameCircleActiveFor(me.getVehicle().getId());
   });

   private RideKeys() {
   }

   static void silenceShared(RideKeys.Bound keys, int key, int scanCode) {
      List<KeyMapping> letters = Arrays.asList(keys.struggleLetters());
      if (!letters.stream().noneMatch(letter -> letter != null && letter.matches(key, scanCode))) {
         for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
            if (!letters.contains(other) && other.matches(key, scanCode)) {
               while (other.consumeClick()) {
               }

               other.setDown(false);
            }
         }
      }
   }

   static void silenceActiveStruggle(RideKeys.Bound keys) {
      if (keys.struggleLetters().length != 0) {
         List<KeyMapping> letters = Arrays.asList(keys.struggleLetters());

         for (KeyMapping letter : keys.struggleLetters()) {
            if (letter != null) {
               for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
                  if (!letters.contains(other) && other.same(letter)) {
                     while (other.consumeClick()) {
                     }

                     other.setDown(false);
                  }
               }
            }
         }
      }
   }

   public static void bind(String namespace, RideKeys.Bound keys) {
      HOSTS.put(namespace, keys);
   }

   public static String hostOf(Entity mount) {
      return mount == null ? null : SpeciesAssets.hostOf(mount);
   }

   public static RideKeys.Bound of(Entity mount) {
      String host = hostOf(mount);
      RideKeys.Bound keys = host == null ? null : HOSTS.get(host);
      return keys == null ? NONE : keys;
   }

   public static Collection<RideKeys.Bound> all() {
      synchronized (HOSTS) {
         return List.copyOf(HOSTS.values());
      }
   }

   public static boolean down(KeyMapping key) {
      return key != null && key.isDown();
   }

   public static boolean clicked(KeyMapping key) {
      return key != null && key.consumeClick();
   }

   public static boolean onMouse(KeyMapping key, int button) {
      return key != null && key.matchesMouse(button);
   }

   public record Bound(
      KeyMapping sprint,
      KeyMapping primaryAbility,
      KeyMapping secondaryAbility,
      KeyMapping freeCamera,
      KeyMapping dismount,
      KeyMapping flightHud,
      KeyMapping[] struggleLetters,
      KeyMapping grab
   ) {
      public Bound(
         KeyMapping sprint,
         KeyMapping primaryAbility,
         KeyMapping secondaryAbility,
         KeyMapping freeCamera,
         KeyMapping dismount,
         KeyMapping flightHud,
         KeyMapping[] struggleLetters
      ) {
         this(sprint, primaryAbility, secondaryAbility, freeCamera, dismount, flightHud, struggleLetters, null);
      }

      void drain() {
         for (KeyMapping key : new KeyMapping[]{this.sprint, this.primaryAbility, this.secondaryAbility, this.freeCamera, this.dismount, this.grab}) {
            while (RideKeys.clicked(key)) {
            }
         }

         for (KeyMapping letter : this.struggleLetters) {
            while (RideKeys.clicked(letter)) {
            }
         }
      }
   }
}

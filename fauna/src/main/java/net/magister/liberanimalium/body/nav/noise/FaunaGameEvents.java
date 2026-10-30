package net.magister.liberanimalium.body.nav.noise;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.compat.GameEvents;
import net.magister.liberanimalium.fauna.sense.SenseTuning;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.gameevent.GameEvent;

public final class FaunaGameEvents {
   public static final Registrar<GameEvent> GAME_EVENTS = Services.registries().create(Registries.GAME_EVENT, "liberanimalium");
   public static final RegEntry<GameEvent> CALL = GAME_EVENTS.register("call", () -> GameEvents.create("call", SenseTuning.callRadius()));
   public static final TagKey<GameEvent> FAUNA_CAN_LISTEN = TagKey.create(
      Registries.GAME_EVENT, new ResourceLocation("liberanimalium", "fauna_can_listen")
   );

   public static void call(FaunaBase body) {
      if (!body.level().isClientSide) {
         GameEvents.send(body, CALL);
      }
   }

   public static void register() {
   }

   private FaunaGameEvents() {
   }
}

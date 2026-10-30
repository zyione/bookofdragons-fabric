package net.magister.liberanimalium.platform.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;

public final class FabricDespawnEvent {
    public interface Callback { InteractionResult allowDespawn(Mob mob); }
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, listeners -> mob -> {
        for (Callback listener : listeners) {
            InteractionResult result = listener.allowDespawn(mob);
            if (result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    });
}

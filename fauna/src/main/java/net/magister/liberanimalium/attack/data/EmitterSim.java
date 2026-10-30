package net.magister.liberanimalium.attack.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.magister.liberanimalium.attack.movement.MovementModifier;

public record EmitterSim(
   float speed,
   int lifetimeTicks,
   int growInTicks,
   float initialTemperature,
   float coolingRate,
   boolean coolingShrink,
   float flicker,
   List<MovementModifier> movement,
   boolean collide
) {
   public static final EmitterSim DEFAULT = new EmitterSim(1.0F, 60, 0, 1.0F, 0.05F, false, 0.0F, List.of());
   public static final Codec<EmitterSim> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(EmitterSim::speed),
            Codec.INT.optionalFieldOf("lifetime_ticks", 60).forGetter(EmitterSim::lifetimeTicks),
            Codec.INT.optionalFieldOf("grow_in_ticks", 0).forGetter(EmitterSim::growInTicks),
            Codec.FLOAT.optionalFieldOf("initial_temperature", 1.0F).forGetter(EmitterSim::initialTemperature),
            Codec.FLOAT.optionalFieldOf("cooling_rate", 0.05F).forGetter(EmitterSim::coolingRate),
            Codec.BOOL.optionalFieldOf("cooling_shrink", false).forGetter(EmitterSim::coolingShrink),
            Codec.FLOAT.optionalFieldOf("flicker", 0.0F).forGetter(EmitterSim::flicker),
            MovementModifier.CODEC.listOf().optionalFieldOf("movement", List.of()).forGetter(EmitterSim::movement),
            Codec.BOOL.optionalFieldOf("collide", false).forGetter(EmitterSim::collide)
         )
         .apply(i, EmitterSim::new)
   );

   public EmitterSim(
      float speed,
      int lifetimeTicks,
      int growInTicks,
      float initialTemperature,
      float coolingRate,
      boolean coolingShrink,
      float flicker,
      List<MovementModifier> movement
   ) {
      this(speed, lifetimeTicks, growInTicks, initialTemperature, coolingRate, coolingShrink, flicker, movement, false);
   }
}

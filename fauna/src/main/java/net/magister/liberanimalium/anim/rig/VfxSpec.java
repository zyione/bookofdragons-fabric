package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public record VfxSpec(boolean coldBreath, boolean flightWake, boolean wingTrails, boolean sleepBubbles, boolean swimBubbles, boolean runDust) {
   public static final VfxSpec DEFAULT = new VfxSpec(true, true, true, true, true, true);

   public static VfxSpec parse(JsonObject json) {
      return json == null
         ? DEFAULT
         : new VfxSpec(
            GsonHelper.getAsBoolean(json, "cold_breath", true),
            GsonHelper.getAsBoolean(json, "flight_wake", true),
            GsonHelper.getAsBoolean(json, "wing_trails", true),
            GsonHelper.getAsBoolean(json, "sleep_bubbles", true),
            GsonHelper.getAsBoolean(json, "swim_bubbles", true),
            GsonHelper.getAsBoolean(json, "run_dust", true)
         );
   }
}

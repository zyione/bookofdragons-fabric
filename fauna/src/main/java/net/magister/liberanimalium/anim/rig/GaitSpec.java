package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonObject;
import net.magister.liberanimalium.util.SpecReader;

public record GaitSpec(
   double reverseFrom,
   double zone,
   double strideShare,
   double strideFloor,
   int strideTicks,
   int stepTicks,
   double stepSpeed,
   double liftShare,
   int leadTicks,
   double leashShare,
   double leadShare,
   double paceShare,
   int triggerTicks,
   int releaseTicks,
   double landingRate
) {
   public static final GaitSpec DEFAULT = new GaitSpec(60.0, 0.6, 0.85, 0.4, 16, 8, 0.075, 0.25, 4, 2.0, 1.0, 4.0, 6, 6, 0.5);

   public static GaitSpec parse(JsonObject json) {
      GaitSpec d = DEFAULT;
      return json == null
         ? d
         : new GaitSpec(
            SpecReader.num(json, "reverse_from", d.reverseFrom),
            SpecReader.num(json, "zone", d.zone),
            SpecReader.num(json, "stride_share", d.strideShare),
            SpecReader.num(json, "stride_floor", d.strideFloor),
            (int)SpecReader.num(json, "stride_ticks", d.strideTicks),
            (int)SpecReader.num(json, "step_ticks", d.stepTicks),
            SpecReader.num(json, "step_speed", d.stepSpeed),
            SpecReader.num(json, "lift_share", d.liftShare),
            (int)SpecReader.num(json, "lead_ticks", d.leadTicks),
            SpecReader.num(json, "leash_share", d.leashShare),
            SpecReader.num(json, "lead_share", d.leadShare),
            SpecReader.num(json, "pace_share", d.paceShare),
            (int)SpecReader.num(json, "trigger_ticks", d.triggerTicks),
            (int)SpecReader.num(json, "release_ticks", d.releaseTicks),
            SpecReader.num(json, "landing_rate", d.landingRate)
         );
   }
}

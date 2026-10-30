package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonObject;
import java.util.List;
import net.magister.liberanimalium.util.SpecReader;

public record BonesSpec(List<String> head, List<String> nose) {
   public static final BonesSpec DEFAULT = new BonesSpec(List.of(), List.of());

   public static BonesSpec parse(JsonObject json) {
      return json == null ? DEFAULT : new BonesSpec(SpecReader.strings(json, "head"), SpecReader.strings(json, "nose"));
   }
}

package net.magister.liberanimalium.anim.rig;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.magister.liberanimalium.util.SpecReader;

public final class RigSpec {
   public static final RigSpec EMPTY = new RigSpec(List.of(), Map.of(), List.of(""), Map.of());
   private final List<String> passes;
   private final Map<String, List<String>> chains;
   private final List<String> prefixes;
   private final Map<String, JsonObject> passConfig;
   private final boolean declaresPrefixes;
   private final CascadeSpec cascade;
   private final HeadTrackSpec headTrack;
   private final SizeSpec size;
   private final ChainSpec chain;
   private final WingsSpec wings;
   private final CarrySpec carry;
   private final SwimSpec swim;
   private final VfxSpec vfx;
   private final BonesSpec bones;
   private final EyesSpec eyes;
   public static final Set<String> BODY_KEYS = Set.of("chains", "prefixes", "parts", "tracked");

   public RigSpec(List<String> passes, Map<String, List<String>> chains, List<String> prefixes, Map<String, JsonObject> passConfig) {
      this.declaresPrefixes = !prefixes.isEmpty();
      this.passes = List.copyOf(passes);
      this.chains = Map.copyOf(chains);
      this.prefixes = prefixes.isEmpty() ? List.of("") : List.copyOf(prefixes);
      this.passConfig = Map.copyOf(passConfig);
      this.cascade = CascadeSpec.parse(passConfig.get("cascade"));
      this.headTrack = HeadTrackSpec.parse(passConfig.get("head_track"));
      this.size = SizeSpec.parse(passConfig.get("size"));
      this.chain = ChainSpec.parse(passConfig.get("chain"));
      this.wings = WingsSpec.parse(passConfig.get("wings"));
      this.carry = CarrySpec.parse(passConfig.get("carry"));
      this.swim = SwimSpec.parse(passConfig.get("swim"));
      this.vfx = VfxSpec.parse(passConfig.get("vfx"));
      this.bones = BonesSpec.parse(passConfig.get("bones"));
      this.eyes = EyesSpec.parse(passConfig.get("eyes"));
   }

   public VfxSpec vfx() {
      return this.vfx;
   }

   public BonesSpec bones() {
      return this.bones;
   }

   public EyesSpec eyes() {
      return this.eyes;
   }

   public WingsSpec wings() {
      return this.wings;
   }

   public CarrySpec carry() {
      return this.carry;
   }

   public SwimSpec swim() {
      return this.swim;
   }

   public Map<String, PointChain.Water> chainWater() {
      return this.chain.water();
   }

   public SizeSpec size() {
      return this.size;
   }

   public static RigSpec inherit(RigSpec base, RigSpec override) {
      if (override == null || override == EMPTY) {
         return base;
      } else if (base != null && base != EMPTY) {
         Map<String, List<String>> chains = new LinkedHashMap<>(base.chains);
         chains.putAll(override.chains);
         Map<String, JsonObject> config = new LinkedHashMap<>(base.passConfig);
         config.putAll(override.passConfig);
         return new RigSpec(
            override.passes.isEmpty() ? base.passes : override.passes, chains, override.declaresPrefixes ? override.prefixes : base.prefixes, config
         );
      } else {
         return override;
      }
   }

   public CascadeSpec cascade() {
      return this.cascade;
   }

   public HeadTrackSpec headTrack() {
      return this.headTrack;
   }

   public Map<String, PointChain.Params> physics() {
      return this.chain.physics();
   }

   public List<String> passes() {
      return this.passes;
   }

   public List<String> chain(String name) {
      return this.chains.getOrDefault(name, List.of());
   }

   public Map<String, List<String>> chains() {
      return this.chains;
   }

   public RigSpec withBody(List<String> prefixes, Map<String, List<String>> chains) {
      return new RigSpec(this.passes, chains, prefixes, this.passConfig);
   }

   public List<String> prefixes() {
      return this.prefixes;
   }

   public JsonObject config(String pass) {
      return this.passConfig.get(pass);
   }

   public boolean isEmpty() {
      return this.passes.isEmpty() && this.chains.isEmpty();
   }

   public static RigSpec parse(JsonObject json) {
      if (json == null) {
         return EMPTY;
      }

      List<String> passes = SpecReader.strings(json, "passes");
      Map<String, JsonObject> config = new LinkedHashMap<>();

      for (Entry<String, JsonElement> entry : json.entrySet()) {
         String key = entry.getKey();
         if (!BODY_KEYS.contains(key) && !key.equals("passes") && entry.getValue().isJsonObject()) {
            config.put(key, entry.getValue().getAsJsonObject());
         }
      }

      return new RigSpec(passes, Map.of(), List.of(), config);
   }
}

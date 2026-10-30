package net.magister.liberanimalium.anim.rig;

import com.mojang.logging.LogUtils;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import mod.azure.azurelib.model.AzBakedModel;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.slf4j.Logger;

public final class RigRunner {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final BoneSink sink;
   private final RigWrite write;
   private int lastComplaint = Integer.MIN_VALUE;
   private Consumer<List<String>> traceSink;
   private static final PerfClock.Section CLOCK = PerfClock.client("rig");
   private static final PerfClock.Section CLOCK_REPLAY = PerfClock.client("rig.replay");
   private static final Map<RigPass, PerfClock.Section> PASS_CLOCKS = new IdentityHashMap<>();

   public RigRunner() {
      this(new AzBoneSink());
   }

   public RigRunner(BoneSink sink) {
      this.sink = sink;
      this.write = new RigWrite(sink);
   }

   public void run(RiggedBody body, AzBakedModel model, float partialTick, boolean firstPass, Map<String, RigPass> registry) {
      this.run(body, model, partialTick, firstPass, registry, new Matrix4f(), Vec3.ZERO);
   }

   public void run(
      RiggedBody body, AzBakedModel model, float partialTick, boolean firstPass, Map<String, RigPass> registry, Matrix4f modelToWorld, Vec3 drawnPosition
   ) {
      this.sink.bind(model);
      RigSpec spec = body.animRig();
      RigFrame frame = new RigFrame(body, model, this.write, spec, partialTick, modelToWorld, drawnPosition);
      if (!firstPass) {
         CLOCK_REPLAY.end(CLOCK_REPLAY.start());
         this.write.replay();

         for (String name : spec.passes()) {
            RigPass pass = registry.get(name);
            if (pass != null) {
               pass.repeat(frame);
            }
         }
      } else {
         String speciesId = body.animSpeciesId();
         if (!RigReport.checked(speciesId)) {
            RigReport.check(speciesId, spec, body.animPartBones(), this.sink);
         }

         this.write.reset();
         Consumer<List<String>> traceSink = this.traceSink;
         if (traceSink != null) {
            this.write.armTrace();
         }

         long rigClock = CLOCK.start();

         for (String name : spec.passes()) {
            RigPass pass = registry.get(name);
            if (pass != null) {
               this.write.beginPass(name);
               PerfClock.Section clock = PASS_CLOCKS.computeIfAbsent(pass, p -> PerfClock.client("rig." + name));
               long start = clock.start();
               pass.solve(frame);
               clock.end(start);
            }
         }

         CLOCK.end(rigClock);
         if (traceSink != null) {
            this.traceSink = null;
            traceSink.accept(this.write.drainTrace());
         }

         this.complain(body);
      }
   }

   public void traceNextFrame(Consumer<List<String>> sink) {
      this.traceSink = sink;
   }

   private void complain(RiggedBody body) {
      List<String> violations = this.write.violations();
      if (!violations.isEmpty() && body.animTick() != this.lastComplaint) {
         this.lastComplaint = body.animTick();
         LOGGER.warn("[Rig] {}: {} bone-axis written by two passes: {}", new Object[]{body.animSpeciesId(), violations.size(), String.join("; ", violations)});
      }
   }

   public List<String> violations() {
      return this.write.violations();
   }

   public RigWrite writes() {
      return this.write;
   }
}

package net.magister.liberanimalium.anim.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.anim.api.SignalView;
import net.magister.liberanimalium.anim.binding.AnimationPredicate;

public final class ControllerMachine {
   private static final int NO_RUNG = Integer.MAX_VALUE;
   private final ControllerSpec spec;
   private String current;
   private int currentRung = Integer.MAX_VALUE;
   private long dwellUntil;
   private final Map<String, Long> nextEntry = new HashMap<>();
   private final Map<String, Long> nextPick = new HashMap<>();
   private String pickedClip;
   private final boolean[] rungHeld;
   private ControllerMachine.Step step = ControllerMachine.Step.NOTHING;

   public ControllerMachine(ControllerSpec spec) {
      this.spec = spec;
      this.rungHeld = new boolean[spec.any().size()];
   }

   public ControllerMachine.Step tick(SignalView signals, Ambit ambit) {
      if (!this.spec.activeWhen().test(signals)) {
         this.step = this.current == null ? ControllerMachine.Step.NOTHING : new ControllerMachine.Step(null, null, this.current);
         this.current = null;
         this.pickedClip = null;
         this.currentRung = Integer.MAX_VALUE;
         return this.step;
      }

      if (this.current == null) {
         return this.enter(this.spec.initial(), Integer.MAX_VALUE, signals, ambit);
      }

      StateSpec state = this.spec.state(this.current);
      if (state == null) {
         return this.enter(this.spec.initial(), Integer.MAX_VALUE, signals, ambit);
      }

      boolean dwelling = this.dwelling(state, ambit);
      List<ControllerSpec.Transition> ladder = this.spec.any();
      int taken = -1;
      boolean settled = false;

      for (int rung = 0; rung < ladder.size(); rung++) {
         ControllerSpec.Transition rungStep = ladder.get(rung);
         boolean holds = rungStep.when().test(signals);
         boolean rose = holds && !this.rungHeld[rung];
         this.rungHeld[rung] = holds;
         if (!settled) {
            if (dwelling && rung >= this.currentRung) {
               settled = true;
            } else if (rungStep.edge() ? rose : holds) {
               if (rungStep.to().equals(this.current)) {
                  settled = true;
               } else if (!this.onCooldown(rungStep.to(), ambit)) {
                  taken = rung;
                  settled = true;
               }
            }
         }
      }

      if (taken >= 0) {
         return this.enter(ladder.get(taken).to(), taken, signals, ambit);
      }

      if (dwelling) {
         this.step = new ControllerMachine.Step(this.current, null, null);
         return this.step;
      }

      for (Entry<String, AnimationPredicate> transition : state.transitions().entrySet()) {
         if (transition.getValue().test(signals)) {
            if (!transition.getKey().equals(this.current)) {
               return this.enter(transition.getKey(), Integer.MAX_VALUE, signals, ambit);
            }
            break;
         }
      }

      this.step = new ControllerMachine.Step(this.current, null, null);
      return this.step;
   }

   private boolean dwelling(StateSpec state, Ambit ambit) {
      return ambit.now() < this.dwellUntil ? true : state.exitOnClipEnd() && !ambit.layerIdle();
   }

   private boolean onCooldown(String state, Ambit ambit) {
      Long until = this.nextEntry.get(state);
      return until != null && ambit.now() < until;
   }

   private ControllerMachine.Step enter(String target, int rung, SignalView signals, Ambit ambit) {
      String left = this.current;
      this.current = target;
      this.currentRung = rung;
      StateSpec state = this.spec.state(target);
      if (state == null) {
         this.dwellUntil = 0L;
         this.pickedClip = null;
      } else {
         this.dwellUntil = ambit.now() + state.rollDwell(ambit.random());
         if (state.cooldown() > 0) {
            this.nextEntry.put(target, ambit.now() + state.cooldown());
         }

         this.pickedClip = this.pick(state, signals, ambit);
      }

      this.step = new ControllerMachine.Step(target, target, left);
      return this.step;
   }

   private String pick(StateSpec state, SignalView signals, Ambit ambit) {
      if (state.pool().isEmpty()) {
         return null;
      }

      List<PoolEntry> eligible = new ArrayList<>();
      int total = 0;

      for (PoolEntry entry : state.pool()) {
         if (entry.when().test(signals)) {
            String clip = entry.resolveClip(ambit.clipPrefix());
            if (clip != null && ambit.clipExists().test(clip)) {
               Long until = this.nextPick.get(clip);
               if (until == null || ambit.now() >= until) {
                  eligible.add(entry);
                  total += entry.weight();
               }
            }
         }
      }

      if (eligible.isEmpty()) {
         return null;
      }

      int roll = ambit.random().applyAsInt(total);
      PoolEntry chosen = eligible.get(eligible.size() - 1);

      for (PoolEntry entry : eligible) {
         roll -= entry.weight();
         if (roll < 0) {
            chosen = entry;
            break;
         }
      }

      String clip = chosen.resolveClip(ambit.clipPrefix());
      if (chosen.cooldown() > 0) {
         this.nextPick.put(clip, ambit.now() + chosen.cooldown());
      }

      return clip;
   }

   public String currentClip(String speciesPrefix) {
      StateSpec state = this.current == null ? null : this.spec.state(this.current);
      if (state == null) {
         return null;
      } else {
         return !state.pool().isEmpty() ? this.pickedClip : state.resolveClip(speciesPrefix);
      }
   }

   public String current() {
      return this.current;
   }

   public ControllerSpec spec() {
      return this.spec;
   }

   public ControllerMachine.Step step() {
      return this.step;
   }

   public List<String> suppressed() {
      StateSpec state = this.current == null ? null : this.spec.state(this.current);
      return state == null ? List.of() : state.suppress();
   }

   public record Step(String state, String entered, String exited) {
      static final ControllerMachine.Step NOTHING = new ControllerMachine.Step(null, null, null);

      public boolean moved() {
         return this.entered != null || this.exited != null;
      }
   }
}

package net.magister.liberanimalium.util;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PerfClock {
   static final double RATE = 0.025;
   private static final List<PerfClock.Section> SERVER = new CopyOnWriteArrayList<>();
   private static final List<PerfClock.Section> CLIENT = new CopyOnWriteArrayList<>();
   private static int framesThisTick;
   private static double emaFrames;
   private static boolean framesPrimed;
   private static int frameId;

   public static void frame() {
      framesThisTick++;
      frameId++;
   }

   public static int frameId() {
      return frameId;
   }

   public static double framesPerTick() {
      return emaFrames;
   }

   private PerfClock() {
   }

   public static PerfClock.Section server(String name) {
      PerfClock.Section s = new PerfClock.Section(name);
      SERVER.add(s);
      return s;
   }

   public static PerfClock.Section client(String name) {
      PerfClock.Section s = new PerfClock.Section(name);
      CLIENT.add(s);
      return s;
   }

   public static void resetClient() {
      for (PerfClock.Section s : CLIENT) {
         s.clear();
      }

      framesThisTick = 0;
      emaFrames = 0.0;
      framesPrimed = false;
   }

   public static void rollServer() {
      for (PerfClock.Section s : SERVER) {
         s.roll();
      }
   }

   public static void rollClient() {
      for (PerfClock.Section s : CLIENT) {
         s.roll();
      }

      if (!framesPrimed) {
         emaFrames = framesThisTick;
         framesPrimed = true;
      } else {
         emaFrames = emaFrames + (framesThisTick - emaFrames) * 0.025;
      }

      framesThisTick = 0;
   }

   public static List<PerfClock.Section> serverSections() {
      return List.copyOf(SERVER);
   }

   public static List<PerfClock.Section> clientSections() {
      return List.copyOf(CLIENT);
   }

   public static final class Section {
      private final String name;
      private long accNs;
      private int accCalls;
      private double emaNs;
      private double emaCalls;
      private boolean primed;

      private Section(String name) {
         this.name = name;
      }

      public String name() {
         return this.name;
      }

      public long start() {
         return System.nanoTime();
      }

      public void end(long start) {
         this.accNs = this.accNs + (System.nanoTime() - start);
         this.accCalls++;
      }

      void clear() {
         this.accNs = 0L;
         this.accCalls = 0;
         this.emaNs = 0.0;
         this.emaCalls = 0.0;
         this.primed = false;
      }

      void roll() {
         if (!this.primed) {
            this.emaNs = this.accNs;
            this.emaCalls = this.accCalls;
            this.primed = true;
         } else {
            this.emaNs = this.emaNs + (this.accNs - this.emaNs) * 0.025;
            this.emaCalls = this.emaCalls + (this.accCalls - this.emaCalls) * 0.025;
         }

         this.accNs = 0L;
         this.accCalls = 0;
      }

      public double microsPerTick() {
         return this.emaNs / 1000.0;
      }

      public double callsPerTick() {
         return this.emaCalls;
      }

      public double microsPerCall() {
         return this.emaCalls < 1.0E-9 ? 0.0 : this.emaNs / this.emaCalls / 1000.0;
      }

      public String line() {
         return String.format(
            Locale.ROOT, "%-18s %9.1f us/tick %7.1f calls/tick %8.1f us/call", this.name, this.microsPerTick(), this.callsPerTick(), this.microsPerCall()
         );
      }

      public String frameLine(double framesPerTick) {
         double perFrame = framesPerTick < 1.0E-9 ? 0.0 : this.microsPerTick() / framesPerTick;
         return String.format(
            Locale.ROOT,
            "%-18s %9.1f us/tick %9.1f us/frame %7.1f calls/tick %8.1f us/call",
            this.name,
            this.microsPerTick(),
            perFrame,
            this.callsPerTick(),
            this.microsPerCall()
         );
      }
   }
}

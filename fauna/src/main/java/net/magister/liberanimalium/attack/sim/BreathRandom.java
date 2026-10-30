package net.magister.liberanimalium.attack.sim;

import net.magister.liberanimalium.fauna.core.Seeds;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

public final class BreathRandom implements RandomSource {
   private long seed;

   public BreathRandom(long seed) {
      this.seed = seed == 0L ? -7046029254386353131L : seed;
   }

   public static long seedFor(long runSeed, int emitterIndex, int spawnTick, int ordinal) {
      long h = runSeed;
      h = Seeds.mix(h ^ emitterIndex * -7046029254386353131L);
      h = Seeds.mix(h ^ spawnTick * -4417276706812531889L);
      return Seeds.mix(h ^ ordinal * 1609587929392839161L);
   }

   public long state() {
      return this.seed;
   }

   private long next() {
      long s = this.seed;
      s ^= s >>> 12;
      s ^= s << 25;
      s ^= s >>> 27;
      this.seed = s;
      return s * 2685821657736338717L;
   }

   public RandomSource fork() {
      return new BreathRandom(this.next());
   }

   public PositionalRandomFactory forkPositional() {
      throw new UnsupportedOperationException("BreathRandom is not positional");
   }

   public void setSeed(long newSeed) {
      this.seed = newSeed == 0L ? -7046029254386353131L : newSeed;
   }

   public int nextInt() {
      return (int)(this.next() >>> 32);
   }

   public int nextInt(int bound) {
      return bound <= 0 ? 0 : (int)((this.next() >>> 33) % bound);
   }

   public long nextLong() {
      return this.next();
   }

   public boolean nextBoolean() {
      return (this.next() & 1L) != 0L;
   }

   public float nextFloat() {
      return (float)(this.next() >>> 40) * 5.9604645E-8F;
   }

   public double nextDouble() {
      return (this.next() >>> 11) * 1.110223E-16F;
   }

   public double nextGaussian() {
      double u1 = Math.max(1.0E-12, this.nextDouble());
      double u2 = this.nextDouble();
      return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos((Math.PI * 2) * u2);
   }
}

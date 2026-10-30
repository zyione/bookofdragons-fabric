package net.magister.liberanimalium.platform.event;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;

public final class Event<T> {
   private volatile T[][] bands;
   private volatile T[] all;

   public Event(Class<? super T> type) {
      this.bands = (T[][])((Object[][])Array.newInstance(type, Event.Priority.values().length, 0));
      this.all = (T[])((Object[])Array.newInstance(type, 0));
   }

   public void register(T listener) {
      this.register(Event.Priority.NORMAL, listener);
   }

   public synchronized void register(Event.Priority priority, T listener) {
      Objects.requireNonNull(priority, "priority");
      Objects.requireNonNull(listener, "listener");
      T[][] next = (T[][])((Object[][])this.bands.clone());
      T[] band = next[priority.ordinal()];
      T[] grown = (T[])Arrays.copyOf(band, band.length + 1);
      grown[band.length] = listener;
      next[priority.ordinal()] = grown;
      int total = 0;

      for (T[] each : next) {
         total += each.length;
      }

      T[] joined = (T[])Arrays.copyOf(next[0], total);
      int at = next[0].length;

      for (int i = 1; i < next.length; i++) {
         System.arraycopy(next[i], 0, joined, at, next[i].length);
         at += next[i].length;
      }

      this.bands = next;
      this.all = joined;
   }

   public T[] listeners(Event.Priority priority) {
      return this.bands[priority.ordinal()];
   }

   public T[] all() {
      return this.all;
   }

   public void allEach(java.util.function.Consumer<? super T> action) {
      for (T listener : this.all) action.accept(listener);
   }

   public enum Priority {
      HIGHEST,
      NORMAL,
      LOWEST;
   }
}

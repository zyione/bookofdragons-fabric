package net.magister.liberanimalium.fauna.move;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.util.NavStats;

public final class AsyncSolver {
   private static final int WORKERS = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
   private static final ExecutorService POOL = Executors.newFixedThreadPool(WORKERS, r -> {
      Thread t = new Thread(r, "fauna-nav");
      t.setDaemon(true);
      return t;
   });
   private static final ConcurrentLinkedQueue<AsyncSolver.Delivery> DONE = new ConcurrentLinkedQueue<>();
   private static final AtomicLong GENERATION = new AtomicLong();

   private AsyncSolver() {
   }

   public static long nextGeneration() {
      return GENERATION.incrementAndGet();
   }

   public static void submit(SolveRequest request, Consumer<AsyncSolver.Result> callback) {
      submit(request, callback, () -> true);
   }

   public static void submit(SolveRequest request, Consumer<AsyncSolver.Result> callback, BooleanSupplier stillWanted) {
      NavStats.submitted();
      POOL.execute(() -> {
         if (!stillWanted.getAsBoolean()) {
            close(request);
            NavStats.skipped();
         } else {
            long started = System.nanoTime();
            NavPath path = null;

            try {
               path = NavSolver.solve(request.source(), request.goal(), request.goalMode(), request.modeCost(), request.range(), request.danger());
            } catch (RuntimeException e) {
               path = null;
            } finally {
               close(request);
               NavStats.solved(System.nanoTime() - started);
            }

            DONE.add(new AsyncSolver.Delivery(path, request.generation(), callback));
         }
      });
   }

   private static void close(SolveRequest request) {
      if (request.source() instanceof AutoCloseable closeable) {
         try {
            closeable.close();
         } catch (Exception var3) {
         }
      }
   }

   public static int drain() {
      int applied;
      AsyncSolver.Delivery d;
      for (applied = 0; (d = DONE.poll()) != null; applied++) {
         d.callback().accept(new AsyncSolver.Result(d.path(), d.generation()));
      }

      return applied;
   }

   public static int pending() {
      return DONE.size();
   }

   public static int workers() {
      return WORKERS;
   }

   private record Delivery(NavPath path, long generation, Consumer<AsyncSolver.Result> callback) {
   }

   public record Result(@Nullable NavPath path, long generation) {
   }
}

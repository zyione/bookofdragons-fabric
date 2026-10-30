package net.magister.liberanimalium.config;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public final class WorldRules {
   private static final List<WorldRules.Source> HOSTS = new CopyOnWriteArrayList<>();

   private WorldRules() {
   }

   public static void install(WorldRules.Source host) {
      HOSTS.add(host);
   }

   public static boolean maySetFire() {
      for (WorldRules.Source host : HOSTS) {
         if (!host.maySetFire()) {
            return false;
         }
      }

      return true;
   }

   public static boolean mayBreakBlocks() {
      for (WorldRules.Source host : HOSTS) {
         if (!host.mayBreakBlocks()) {
            return false;
         }
      }

      return true;
   }

   public static boolean maySetFire(Level level) {
      return maySetFire() && griefing(level);
   }

   public static boolean mayBreakBlocks(Level level) {
      return mayBreakBlocks() && griefing(level);
   }

   private static boolean griefing(Level level) {
      return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
   }

   public static boolean mayGrabPlayers() {
      for (WorldRules.Source host : HOSTS) {
         if (!host.grabPlayers()) {
            return false;
         }
      }

      return true;
   }

   public static boolean mayWildGrabPlayers() {
      if (!mayGrabPlayers()) {
         return false;
      }

      for (WorldRules.Source host : HOSTS) {
         if (!host.wildGrabPlayers()) {
            return false;
         }
      }

      return true;
   }

   public static float strangerShare() {
      float share = 1.0F;

      for (WorldRules.Source host : HOSTS) {
         share = Math.min(share, host.strangerShare());
      }

      return Math.max(0.0F, share);
   }

   public static boolean isQteTapAssistEnabled() {
      for (WorldRules.Source host : HOSTS) {
         if (host.qteTapAssist()) {
            return true;
         }
      }

      return false;
   }

   public interface Source {
      default boolean maySetFire() {
         return true;
      }

      default boolean mayBreakBlocks() {
         return true;
      }

      default boolean qteTapAssist() {
         return false;
      }

      default boolean grabPlayers() {
         return true;
      }

      default boolean wildGrabPlayers() {
         return true;
      }

      default float strangerShare() {
         return 1.0F;
      }
   }
}

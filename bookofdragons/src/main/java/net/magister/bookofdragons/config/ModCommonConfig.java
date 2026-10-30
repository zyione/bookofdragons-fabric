package net.magister.bookofdragons.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import org.apache.commons.lang3.tuple.Pair;

public class ModCommonConfig {
   public static final ForgeConfigSpec SPEC;
   public static final ModCommonConfig INSTANCE;
   public final BooleanValue qteTapAssist;
   public final BooleanValue dragonsSetFire;
   public final BooleanValue dragonsBreakBlocks;
   public final BooleanValue grabPlayers;
   public final DoubleValue strangerReaction;

   private ModCommonConfig(Builder builder) {
      builder.comment("Book of Dragons Common Settings").push("common");
      this.qteTapAssist = builder.comment(
            new String[]{"Accessibility: holding a direction in tap-driven QTEs auto-taps at a fixed", "cadence instead of requiring rapid key drumming"}
         )
         .translation("bookofdragons.config.qteTapAssist")
         .define("qteTapAssist", false);
      builder.pop();
      builder.comment(
            new String[]{
               "What dragons may do to the terrain. Each breath attack authors its own",
               "sets_fire / breaks_blocks; these switches take precedence and can only forbid."
            }
         )
         .push("griefing");
      this.dragonsSetFire = builder.comment("Dragon attacks and abilities may set fire to blocks. The mobGriefing game rule also applies")
         .translation("bookofdragons.config.dragonsSetFire")
         .define("dragonsSetFire", false);
      this.dragonsBreakBlocks = builder.comment("Dragon attacks may break blocks (explosive impacts). The mobGriefing game rule also applies")
         .translation("bookofdragons.config.dragonsBreakBlocks")
         .define("dragonsBreakBlocks", false);
      builder.pop();
      builder.comment("What a dragon's grip may take. Players are taken only where the server allows PvP.").push("grabbing");
      this.grabPlayers = builder.comment("A dragon's grip may take players").translation("bookofdragons.config.grabPlayers").define("grabPlayers", true);
      builder.pop();
      builder.comment("How dragons treat creatures from other mods.").push("creatures");
      this.strangerReaction = builder.comment(
            new String[]{
               "How strongly a dragon fears, dislikes or attacks a creature from another mod that is not",
               "after it, as a share of its normal reaction. 1.0 is the full reaction, 0.0 ignores it"
            }
         )
         .translation("bookofdragons.config.strangerReaction")
         .defineInRange("strangerReaction", 1.0, 0.0, 1.0);
      builder.pop();
   }

   public static boolean dragonsMaySetFire() {
      try {
         return (Boolean)INSTANCE.dragonsSetFire.get();
      } catch (IllegalStateException | NullPointerException e) {
         return true;
      }
   }

   public static boolean dragonsMayBreakBlocks() {
      try {
         return (Boolean)INSTANCE.dragonsBreakBlocks.get();
      } catch (IllegalStateException | NullPointerException e) {
         return true;
      }
   }

   public static float strangerReactionShare() {
      try {
         return ((Double)INSTANCE.strangerReaction.get()).floatValue();
      } catch (IllegalStateException | NullPointerException e) {
         return 1.0F;
      }
   }

   public static boolean dragonsMayGrabPlayers() {
      try {
         return (Boolean)INSTANCE.grabPlayers.get();
      } catch (IllegalStateException | NullPointerException e) {
         return true;
      }
   }

   public static boolean isQteTapAssistEnabled() {
      try {
         return (Boolean)INSTANCE.qteTapAssist.get();
      } catch (IllegalStateException | NullPointerException e) {
         return false;
      }
   }

   static {
      Pair<ModCommonConfig, ForgeConfigSpec> pair = new Builder().configure(ModCommonConfig::new);
      INSTANCE = (ModCommonConfig)pair.getLeft();
      SPEC = (ForgeConfigSpec)pair.getRight();
   }
}

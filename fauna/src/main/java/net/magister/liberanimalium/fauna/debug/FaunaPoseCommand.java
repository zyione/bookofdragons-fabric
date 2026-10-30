package net.magister.liberanimalium.fauna.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class FaunaPoseCommand {
   private static final List<String> POSE_WORDS = Arrays.stream(PoseFlag.values()).map(f -> f.name().toLowerCase(Locale.ROOT)).toList();

   private FaunaPoseCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fauna").requires(source -> source.hasPermission(2)))
            .then(
               Commands.literal("force")
                  .then(
                     Commands.literal("pose")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("flag", StringArgumentType.word())
                                 .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(POSE_WORDS, builder))
                                 .executes(ctx -> forcePose((CommandSourceStack)ctx.getSource(), StringArgumentType.getString(ctx, "flag"), true)))
                              .then(
                                 Commands.argument("held", BoolArgumentType.bool())
                                    .executes(
                                       ctx -> forcePose(
                                          (CommandSourceStack)ctx.getSource(), StringArgumentType.getString(ctx, "flag"), BoolArgumentType.getBool(ctx, "held")
                                       )
                                    )
                              )
                        )
                  )
            )
      );
   }

   @Nullable
   static FaunaPoseCommand.Nearest nearest(ServerPlayer player) {
      FaunaMind mind = null;
      Entity body = null;
      double best = Double.MAX_VALUE;

      for (Entity e : player.serverLevel().getEntities((Entity)null, player.getBoundingBox().inflate(FaunaDebug.range()), x -> true)) {
         if (e instanceof AnimalHandle handle) {
            FaunaMind candidate = handle.faunaMind();
            if (candidate != null) {
               double distance = e.distanceToSqr(player);
               if (distance < best) {
                  best = distance;
                  mind = candidate;
                  body = e;
               }
            }
         }
      }

      return mind == null ? null : new FaunaPoseCommand.Nearest(mind, body);
   }

   static void line(CommandSourceStack source, ChatFormatting colour, String text) {
      source.sendSuccess(() -> Component.literal(text).withStyle(colour), false);
   }

   private static int forcePose(CommandSourceStack source, String word, boolean held) {
      ServerPlayer player = source.getPlayer();
      if (player == null) {
         source.sendFailure(Component.literal("Run this as a player."));
         return 0;
      }

      PoseFlag flag = null;

      for (PoseFlag candidate : PoseFlag.values()) {
         if (candidate.name().equalsIgnoreCase(word)) {
            flag = candidate;
         }
      }

      if (flag == null) {
         source.sendFailure(Component.literal("No such pose. One of: " + POSE_WORDS));
         return 0;
      } else {
         FaunaPoseCommand.Nearest nearest = nearest(player);
         if (nearest == null) {
            source.sendFailure(Component.literal("No FAUNA animal with a mind within " + (int)FaunaDebug.range() + " blocks."));
            return 0;
         } else {
            String who = nearest.body().getName().getString();
            long until = nearest.mind().forcePose(flag, held);
            line(source, ChatFormatting.GREEN, String.format("pose %s %s on %s", flag.name().toLowerCase(Locale.ROOT), held ? "held" : "released", who));
            line(
               source,
               ChatFormatting.GRAY,
               "  binds as fauna_"
                  + flag.name().toLowerCase(Locale.ROOT)
                  + " - a species with no rule on that signal shows nothing, which is the difference this verb exists to tell apart"
            );
            line(
               source,
               held ? ChatFormatting.YELLOW : ChatFormatting.GRAY,
               held
                  ? "  lapses at age " + until + " (1536t) - after that the engine's own answer stands again"
                  : "  the engine's own answer stands from the next publish"
            );
            return 1;
         }
      }
   }

   record Nearest(FaunaMind mind, Entity body) {
   }
}

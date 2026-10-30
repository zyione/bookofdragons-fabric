package net.magister.bookofdragons.network.message;

import java.util.ArrayList;
import java.util.List;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public class StripLineMessage {
   private static final int MAX_DEPTH = 4;
   private static final int MAX_ARGS = 16;
   private final StripLineMessage.Text text;

   public StripLineMessage(StripLineMessage.Text text) {
      this.text = text;
   }

   public static void encode(StripLineMessage message, FriendlyByteBuf buffer) {
      message.text.write(buffer);
   }

   public static StripLineMessage decode(FriendlyByteBuf buffer) {
      return new StripLineMessage(StripLineMessage.Text.read(buffer, 0));
   }

   public static void handle(StripLineMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         if (Services.platform().isClient()) {
            ClientPacketHandler.handleStripLine(message.text);
         }
      });
   }

   public record Text(String key, String words, List<StripLineMessage.Text> args) {
      public static StripLineMessage.Text of(Component line) {
         return of(line, 0);
      }

      private static StripLineMessage.Text of(Component line, int depth) {
         if (depth < 4 && line.getSiblings().isEmpty() && line.getContents() instanceof TranslatableContents translated && translated.getArgs().length <= 16) {
            List<StripLineMessage.Text> args = new ArrayList<>();

            for (Object arg : translated.getArgs()) {
               args.add(arg instanceof Component part ? of(part, depth + 1) : new StripLineMessage.Text("", String.valueOf(arg), List.of()));
            }

            return new StripLineMessage.Text(translated.getKey(), "", args);
         } else {
            return new StripLineMessage.Text("", line.getString(), List.of());
         }
      }

      public Component plain() {
         if (this.key.isEmpty()) {
            return Component.literal(this.words);
         }

         Object[] parts = new Object[this.args.size()];

         for (int i = 0; i < parts.length; i++) {
            parts[i] = this.args.get(i).plain();
         }

         return Component.translatable(this.key, parts);
      }

      private void write(FriendlyByteBuf buffer) {
         buffer.writeUtf(this.key);
         buffer.writeUtf(this.words);
         buffer.writeVarInt(this.args.size());

         for (StripLineMessage.Text arg : this.args) {
            arg.write(buffer);
         }
      }

      private static StripLineMessage.Text read(FriendlyByteBuf buffer, int depth) {
         String key = buffer.readUtf();
         String words = buffer.readUtf();
         int count = buffer.readVarInt();
         if (count >= 0 && count <= 16 && (count <= 0 || depth < 4)) {
            List<StripLineMessage.Text> args = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
               args.add(read(buffer, depth + 1));
            }

            return new StripLineMessage.Text(key, words, args);
         } else {
            throw new IllegalArgumentException("strip line: " + count + " arguments at depth " + depth);
         }
      }
   }
}

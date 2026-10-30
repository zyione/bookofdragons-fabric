package net.magister.liberanimalium.network;

import net.magister.liberanimalium.body.grip.Grip;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.GripComponent;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.magister.liberanimalium.qte.QteInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class ControlMessageStruggle {
   private final QteInput.Dir dir;

   public ControlMessageStruggle(QteInput.Dir dir) {
      this.dir = dir;
   }

   public static void encode(ControlMessageStruggle message, FriendlyByteBuf buffer) {
      buffer.writeByte(message.dir.ordinal());
   }

   public static ControlMessageStruggle decode(FriendlyByteBuf buffer) {
      int ordinal = buffer.readByte();
      QteInput.Dir[] all = QteInput.Dir.values();
      return new ControlMessageStruggle(all[Math.floorMod(ordinal, all.length)]);
   }

   public static void handle(ControlMessageStruggle message, PacketContext context) {
      context.enqueueWork(() -> {
         ServerPlayer player = context.sender();
         if (player != null) {
            FaunaBase holder = Grip.holderOf(player);
            GripComponent grip = holder == null ? null : holder.grip();
            if (grip != null) {
               grip.struggle(player, message.dir);
            }
         }
      });
   }
}

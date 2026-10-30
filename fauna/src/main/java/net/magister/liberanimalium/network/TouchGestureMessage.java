package net.magister.liberanimalium.network;

import net.magister.liberanimalium.body.touch.TouchState;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.magister.liberanimalium.ride.touch.TouchGestures;
import net.minecraft.network.FriendlyByteBuf;

public class TouchGestureMessage {
   private final int playerId;
   private final TouchState.Gesture gesture;

   public TouchGestureMessage(int playerId, TouchState.Gesture gesture) {
      this.playerId = playerId;
      this.gesture = gesture;
   }

   public int playerId() {
      return this.playerId;
   }

   public TouchState.Gesture gesture() {
      return this.gesture;
   }

   public static void encode(TouchGestureMessage msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.playerId);
      buf.writeEnum(msg.gesture);
   }

   public static TouchGestureMessage decode(FriendlyByteBuf buf) {
      return new TouchGestureMessage(buf.readVarInt(), (TouchState.Gesture)buf.readEnum(TouchState.Gesture.class));
   }

   public static void handle(TouchGestureMessage msg, PacketContext ctx) {
      ctx.enqueueWork(() -> {
         if (Services.platform().isClient()) {
            TouchGestures.accept(msg);
         }
      });
   }
}

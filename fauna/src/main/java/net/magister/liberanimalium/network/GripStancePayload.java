package net.magister.liberanimalium.network;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.GripComponent;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class GripStancePayload {
   private final int bodyId;
   private final float x;
   private final float y;
   private final float z;

   public GripStancePayload(int bodyId, Vec3 offset) {
      this(bodyId, (float)offset.x, (float)offset.y, (float)offset.z);
   }

   private GripStancePayload(int bodyId, float x, float y, float z) {
      this.bodyId = bodyId;
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public static void encode(GripStancePayload message, FriendlyByteBuf buffer) {
      buffer.writeVarInt(message.bodyId);
      buffer.writeFloat(message.x);
      buffer.writeFloat(message.y);
      buffer.writeFloat(message.z);
   }

   public static GripStancePayload decode(FriendlyByteBuf buffer) {
      return new GripStancePayload(buffer.readVarInt(), net.magister.liberanimalium.platform.net.PacketValues.finiteFloat(buffer), net.magister.liberanimalium.platform.net.PacketValues.finiteFloat(buffer), net.magister.liberanimalium.platform.net.PacketValues.finiteFloat(buffer));
   }

   public static void handle(GripStancePayload message, PacketContext context) {
      context.enqueueWork(() -> {
         ServerPlayer sender = context.sender();
         if (sender != null) {
            if (sender.level().getEntity(message.bodyId) instanceof FaunaBase body) {
               GripComponent grip = body.grip();
               if (grip != null) {
                  grip.acceptStance(sender.getId(), new Vec3(message.x, message.y, message.z));
               }
            }
         }
      });
   }
}

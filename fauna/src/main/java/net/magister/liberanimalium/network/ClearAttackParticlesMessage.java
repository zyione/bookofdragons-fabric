package net.magister.liberanimalium.network;

import net.magister.liberanimalium.attack.client.particle.AttackParticle;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class ClearAttackParticlesMessage {
   private final ResourceLocation attackId;
   private final double x;
   private final double y;
   private final double z;
   private final float radius;

   public ClearAttackParticlesMessage(ResourceLocation attackId, double x, double y, double z, float radius) {
      this.attackId = attackId;
      this.x = x;
      this.y = y;
      this.z = z;
      this.radius = radius;
   }

   public static void encode(ClearAttackParticlesMessage msg, FriendlyByteBuf buf) {
      buf.writeResourceLocation(msg.attackId);
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeFloat(msg.radius);
   }

   public static ClearAttackParticlesMessage decode(FriendlyByteBuf buf) {
      return new ClearAttackParticlesMessage(buf.readResourceLocation(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat());
   }

   public static void handle(ClearAttackParticlesMessage msg, PacketContext ctx) {
      ctx.enqueueWork(() -> {
         if (Services.platform().isClient()) {
            AttackParticle.clearNear(msg.attackId, msg.x, msg.y, msg.z, msg.radius);
         }
      });
   }
}

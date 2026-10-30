package net.magister.liberanimalium.network;

import net.magister.liberanimalium.attack.client.BreathReplay;
import net.magister.liberanimalium.attack.runtime.AttackPhaseClock;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public class BreathEmitMessage {
   private final int entityId;
   private final int tick;
   private final AttackPhaseClock.Phase phase;
   private final float progress;
   private final float mx;
   private final float my;
   private final float mz;
   private final float dx;
   private final float dy;
   private final float dz;

   public BreathEmitMessage(int entityId, int tick, AttackPhaseClock.Phase phase, float progress, Vec3 mouth, Vec3 direction) {
      this(
         entityId,
         tick,
         phase,
         progress,
         (float)mouth.x,
         (float)mouth.y,
         (float)mouth.z,
         (float)direction.x,
         (float)direction.y,
         (float)direction.z
      );
   }

   private BreathEmitMessage(int entityId, int tick, AttackPhaseClock.Phase phase, float progress, float mx, float my, float mz, float dx, float dy, float dz) {
      this.entityId = entityId;
      this.tick = tick;
      this.phase = phase;
      this.progress = progress;
      this.mx = mx;
      this.my = my;
      this.mz = mz;
      this.dx = dx;
      this.dy = dy;
      this.dz = dz;
   }

   public static void encode(BreathEmitMessage msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.entityId);
      buf.writeVarInt(msg.tick);
      buf.writeEnum(msg.phase);
      buf.writeFloat(msg.progress);
      buf.writeFloat(msg.mx);
      buf.writeFloat(msg.my);
      buf.writeFloat(msg.mz);
      buf.writeFloat(msg.dx);
      buf.writeFloat(msg.dy);
      buf.writeFloat(msg.dz);
   }

   public static BreathEmitMessage decode(FriendlyByteBuf buf) {
      return new BreathEmitMessage(
         buf.readVarInt(),
         buf.readVarInt(),
         (AttackPhaseClock.Phase)buf.readEnum(AttackPhaseClock.Phase.class),
         buf.readFloat(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readFloat()
      );
   }

   public static void handle(BreathEmitMessage msg, PacketContext ctx) {
      ctx.enqueueWork(() -> {
         if (Services.platform().isClient()) {
            BreathReplay.apply(msg.entityId, msg.tick, msg.phase, msg.progress, new Vec3(msg.mx, msg.my, msg.mz), new Vec3(msg.dx, msg.dy, msg.dz));
         }
      });
   }
}

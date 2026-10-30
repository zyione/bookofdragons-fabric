package net.magister.liberanimalium.platform.part;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.Entity;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

public abstract class PartBody<T extends Entity> extends Entity {
   private final T parent;
   protected PartBody(T parent) {
      super(parent.getType(), parent.level());
      this.parent = parent;
   }

   public final T getParent() { return this.parent; }

   @Override
   public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
      throw new UnsupportedOperationException("Multipart bodies are created by their parent, never independently spawned");
   }

   static boolean isPart(Entity entity) {
      return entity instanceof PartBody<?> || entity instanceof EnderDragonPart;
   }

   static Entity parentOf(Entity part) {
      return part instanceof PartBody<?> body ? body.getParent() : ((EnderDragonPart)part).parentMob;
   }

   static boolean isMultipart(Entity entity) {
      return entity instanceof FaunaBase fauna ? fauna.isMultipartEntity() : entity instanceof EnderDragon;
   }

   @Nullable
   static Entity[] partsOf(Entity entity) {
      return entity instanceof FaunaBase fauna ? fauna.getParts() : entity instanceof EnderDragon dragon ? dragon.getSubEntities() : null;
   }
}

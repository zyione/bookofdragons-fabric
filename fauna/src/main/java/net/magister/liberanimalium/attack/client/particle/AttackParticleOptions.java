package net.magister.liberanimalium.attack.client.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleOptions.Deserializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AttackParticleOptions implements ParticleOptions {
   private final ResourceLocation attackId;
   private final int emitterIndex;
   private final float scale;
   private final long seed;
   public static final MapCodec<AttackParticleOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("attackId").forGetter(AttackParticleOptions::getAttackId),
            Codec.INT.fieldOf("emitterIndex").forGetter(AttackParticleOptions::getEmitterIndex),
            Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(AttackParticleOptions::getScale),
            Codec.LONG.optionalFieldOf("seed", 0L).forGetter(AttackParticleOptions::getSeed)
         )
         .apply(instance, AttackParticleOptions::new)
   );
   public static final Codec<AttackParticleOptions> CODEC = MAP_CODEC.codec();
   public static final Deserializer<AttackParticleOptions> DESERIALIZER = new Deserializer<AttackParticleOptions>() {
      public AttackParticleOptions fromCommand(ParticleType<AttackParticleOptions> pParticleType, StringReader pReader) throws CommandSyntaxException {
         pReader.expect(' ');
         ResourceLocation id = ResourceLocation.read(pReader);
         pReader.expect(' ');
         int index = pReader.readInt();
         float scale = 1.0F;
         long seed = 0L;
         if (pReader.canRead() && pReader.peek() == ' ') {
            pReader.skip();
            scale = pReader.readFloat();
         }

         if (pReader.canRead() && pReader.peek() == ' ') {
            pReader.skip();
            seed = pReader.readLong();
         }

         return new AttackParticleOptions(id, index, scale, seed);
      }

      public AttackParticleOptions fromNetwork(ParticleType<AttackParticleOptions> pParticleType, FriendlyByteBuf pBuffer) {
         return AttackParticleOptions.read(pBuffer);
      }
   };

   public AttackParticleOptions(ResourceLocation attackId, int emitterIndex) {
      this(attackId, emitterIndex, 1.0F, 0L);
   }

   public AttackParticleOptions(ResourceLocation attackId, int emitterIndex, float scale) {
      this(attackId, emitterIndex, scale, 0L);
   }

   public AttackParticleOptions(ResourceLocation attackId, int emitterIndex, float scale, long seed) {
      this.attackId = attackId;
      this.emitterIndex = emitterIndex;
      this.scale = scale;
      this.seed = seed;
   }

   public ResourceLocation getAttackId() {
      return this.attackId;
   }

   public int getEmitterIndex() {
      return this.emitterIndex;
   }

   public float getScale() {
      return this.scale;
   }

   public long getSeed() {
      return this.seed;
   }

   public ParticleType<?> getType() {
      return FaunaParticles.ATTACK_PARTICLE_TYPE.get();
   }

   public void writeToNetwork(FriendlyByteBuf pBuffer) {
      write(pBuffer, this);
   }

   public static void write(FriendlyByteBuf buffer, AttackParticleOptions options) {
      buffer.writeResourceLocation(options.attackId);
      buffer.writeVarInt(options.emitterIndex);
      buffer.writeFloat(options.scale);
      buffer.writeLong(options.seed);
   }

   public static AttackParticleOptions read(FriendlyByteBuf buffer) {
      return new AttackParticleOptions(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readFloat(), buffer.readLong());
   }

   public String writeToString() {
      return String.format(
         Locale.ROOT,
         "%s %s %d %.3f %d",
         FaunaParticles.ATTACK_PARTICLE_TYPE.id().toString(),
         this.attackId.toString(),
         this.emitterIndex,
         this.scale,
         this.seed
      );
   }
}

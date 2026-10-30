package net.magister.bookofdragons.datagen.data;

import java.util.UUID;
import net.magister.bookofdragons.entity.base.dragon.GrowthStage;
import net.magister.bookofdragons.entity.data.DragonType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class DragonRegistryEntry {
   private UUID dragonUUID;
   private UUID ownerUUID;
   private String customName;
   private DragonType type;
   private GrowthStage stage;
   private BlockPos lastKnownPos;
   private ResourceKey<Level> dimension;
   private long lastUpdateTime;
   private float health;
   private float maxHealth;
   private int affection;
   private boolean hasSaddle;
   private boolean hasChest;
   private CompoundTag genetics = new CompoundTag();
   private long tamedAt;

   public DragonRegistryEntry(
      UUID dragonUUID,
      UUID ownerUUID,
      String customName,
      DragonType type,
      GrowthStage stage,
      BlockPos lastKnownPos,
      ResourceKey<Level> dimension,
      float health,
      float maxHealth,
      int affection,
      boolean hasSaddle,
      boolean hasChest
   ) {
      this.dragonUUID = dragonUUID;
      this.ownerUUID = ownerUUID;
      this.customName = customName;
      this.type = type;
      this.stage = stage;
      this.lastKnownPos = lastKnownPos;
      this.dimension = dimension;
      this.lastUpdateTime = System.currentTimeMillis();
      this.health = health;
      this.maxHealth = maxHealth;
      this.affection = affection;
      this.hasSaddle = hasSaddle;
      this.hasChest = hasChest;
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.putUUID("DragonUUID", this.dragonUUID);
      tag.putUUID("OwnerUUID", this.ownerUUID);
      tag.putString("CustomName", this.customName);
      tag.putString("Type", this.type.name());
      tag.putString("Stage", this.stage.name());
      tag.putInt("PosX", this.lastKnownPos.getX());
      tag.putInt("PosY", this.lastKnownPos.getY());
      tag.putInt("PosZ", this.lastKnownPos.getZ());
      tag.putString("Dimension", this.dimension.location().toString());
      tag.putLong("LastUpdate", this.lastUpdateTime);
      tag.putFloat("Health", this.health);
      tag.putFloat("MaxHealth", this.maxHealth);
      tag.putInt("Affection", this.affection);
      tag.putBoolean("HasSaddle", this.hasSaddle);
      tag.putBoolean("HasChest", this.hasChest);
      tag.put("Genetics", this.genetics);
      tag.putLong("TamedAt", this.tamedAt);
      return tag;
   }

   public static DragonRegistryEntry load(CompoundTag tag) {
      UUID dragonUUID = tag.getUUID("DragonUUID");
      UUID ownerUUID = tag.getUUID("OwnerUUID");
      String customName = tag.getString("CustomName");
      DragonType type = DragonType.valueOf(tag.getString("Type"));
      GrowthStage stage = GrowthStage.valueOf(tag.getString("Stage"));
      BlockPos pos = new BlockPos(tag.getInt("PosX"), tag.getInt("PosY"), tag.getInt("PosZ"));
      String dimensionStr = tag.getString("Dimension");
      ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionStr));
      float health = tag.getFloat("Health");
      float maxHealth = tag.getFloat("MaxHealth");
      int affection = tag.getInt("Affection");
      boolean hasSaddle = tag.getBoolean("HasSaddle");
      boolean hasChest = tag.getBoolean("HasChest");
      DragonRegistryEntry entry = new DragonRegistryEntry(
         dragonUUID, ownerUUID, customName, type, stage, pos, dimension, health, maxHealth, affection, hasSaddle, hasChest
      );
      entry.lastUpdateTime = tag.getLong("LastUpdate");
      entry.genetics = tag.getCompound("Genetics");
      entry.tamedAt = tag.getLong("TamedAt");
      return entry;
   }

   public UUID getDragonUUID() {
      return this.dragonUUID;
   }

   public UUID getOwnerUUID() {
      return this.ownerUUID;
   }

   public String getCustomName() {
      return this.customName;
   }

   public DragonType getType() {
      return this.type;
   }

   public GrowthStage getStage() {
      return this.stage;
   }

   public BlockPos getLastKnownPos() {
      return this.lastKnownPos;
   }

   public ResourceKey<Level> getDimension() {
      return this.dimension;
   }

   public float getHealth() {
      return this.health;
   }

   public float getMaxHealth() {
      return this.maxHealth;
   }

   public int getAffection() {
      return this.affection;
   }

   public boolean hasSaddle() {
      return this.hasSaddle;
   }

   public boolean hasChest() {
      return this.hasChest;
   }

   public CompoundTag getGenetics() {
      return this.genetics;
   }

   public void setGenetics(CompoundTag genetics) {
      this.genetics = genetics == null ? new CompoundTag() : genetics.copy();
   }

   public long getTamedAt() {
      return this.tamedAt;
   }

   public void setTamedAt(long tamedAt) {
      this.tamedAt = tamedAt;
   }
}

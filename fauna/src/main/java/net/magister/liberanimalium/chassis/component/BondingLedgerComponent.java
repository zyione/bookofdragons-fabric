package net.magister.liberanimalium.chassis.component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.magister.liberanimalium.body.stats.TamingStage;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.fauna.api.Delivery;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class BondingLedgerComponent implements IFaunaComponent {
   public static final int MAX_BOND = 1000;
   private static final int GIFT_HAND = 30;
   private static final float REPEAT_FOLD = 1.0F;
   private FaunaBase body;
   private final Map<UUID, Integer> bond = new ConcurrentHashMap<>();

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   public int getBond(UUID playerUUID) {
      return this.bond.getOrDefault(playerUUID, 0);
   }

   public int highestBond() {
      int highest = 0;

      for (int value : this.bond.values()) {
         highest = Math.max(highest, value);
      }

      return highest;
   }

   public void setBond(UUID playerUUID, int amount) {
      this.bond.put(playerUUID, Math.max(0, Math.min(1000, amount)));
   }

   public void addBond(UUID playerUUID, int amount) {
      this.setBond(playerUUID, this.getBond(playerUUID) + amount);
   }

   public TamingStage stageFor(UUID playerUUID) {
      boolean owned = this.body != null && this.body.isTame() && playerUUID.equals(this.body.getOwnerUUID());
      return TamingStage.of(this.getBond(playerUUID), owned);
   }

   public int awardGift(LivingEntity giver, ItemStack stack, Delivery delivery) {
      return giver != null && stack != null && !stack.isEmpty() ? this.awardGift(giver.getUUID(), delivery) : 0;
   }

   public int awardGift(UUID giverId, Delivery delivery) {
      if (giverId != null && delivery != null) {
         int points = Math.round(30.0F * delivery.share());
         this.addBond(giverId, points);
         return points;
      } else {
         return 0;
      }
   }

   public int awardChannel(UUID playerUUID, int base, float repeats) {
      int points = Math.round(base / (1.0F + 1.0F * Math.max(0.0F, repeats)));
      if (points <= 0) {
         return 0;
      }

      this.addBond(playerUUID, points);
      return points;
   }

   @Override
   public void save(CompoundTag tag) {
      CompoundTag bonds = new CompoundTag();

      for (Entry<UUID, Integer> e : this.bond.entrySet()) {
         bonds.putInt(e.getKey().toString(), e.getValue());
      }

      tag.put("BondLedger", bonds);
   }

   @Override
   public void load(CompoundTag tag) {
      this.bond.clear();
      if (tag.contains("BondLedger")) {
         CompoundTag bonds = tag.getCompound("BondLedger");
         Map<UUID, Integer> loaded = new HashMap<>();

         for (String key : bonds.getAllKeys()) {
            try {
               loaded.put(UUID.fromString(key), bonds.getInt(key));
            } catch (IllegalArgumentException var7) {
            }
         }

         this.bond.putAll(loaded);
      }
   }
}

package net.magister.bookofdragons.entity.custom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.magister.bookofdragons.block.ModBlocks;
import net.magister.bookofdragons.compat.Trades;
import net.magister.bookofdragons.item.ModItems;
import net.magister.bookofdragons.villager.FixedTrade;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class HooliganDrifterEntity extends WanderingTrader {
   public HooliganDrifterEntity(EntityType<? extends WanderingTrader> entityType, Level level) {
      super(entityType, level);
   }

   public static Builder createAttributes() {
      return WanderingTrader.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 1.0).add(Attributes.FOLLOW_RANGE, 48.0);
   }

   protected void updateTrades() {
      MerchantOffers offers = this.getOffers();
      offers.clear();
      this.addGuaranteedTrades(offers);
      List<ItemListing> common = this.getCommonTrades();
      Collections.shuffle(common);

      for (int i = 0; i < Math.min(3, common.size()); i++) {
         MerchantOffer offer = common.get(i).getOffer(this, this.random);
         if (offer != null) {
            offers.add(offer);
         }
      }

      List<ItemListing> rare = this.getRareTrades();
      Collections.shuffle(rare);

      for (int i = 0; i < Math.min(2, rare.size()); i++) {
         MerchantOffer offer = rare.get(i).getOffer(this, this.random);
         if (offer != null) {
            offers.add(offer);
         }
      }
   }

   private void addGuaranteedTrades(MerchantOffers offers) {
      offers.add(Trades.offer(new ItemStack(Items.IRON_INGOT, 1), new ItemStack((ItemLike)ModItems.BOOK_OF_DRAGONS.get(), 1), 1, 5, 0.05F));
      offers.add(
         Trades.offer(
            new ItemStack(Items.EMERALD, 10), new ItemStack(Items.IRON_BLOCK, 1), new ItemStack((ItemLike)ModBlocks.VIKING_ANVIL.get(), 1), 6, 10, 0.05F
         )
      );
   }

   private List<ItemListing> getCommonTrades() {
      List<ItemListing> trades = new ArrayList<>();
      trades.add(new FixedTrade(new ItemStack(Items.EMERALD, 4), new ItemStack(Items.MUTTON, 8), 12, 5, 0.05F));
      trades.add(new FixedTrade(new ItemStack(Items.EMERALD, 3), new ItemStack(Items.CHICKEN, 8), 12, 5, 0.05F));
      trades.add(new FixedTrade(new ItemStack(Items.EMERALD, 2), new ItemStack(Items.COD, 8), 12, 5, 0.05F));
      trades.add(new FixedTrade(new ItemStack(Items.EMERALD, 12), new ItemStack(Items.SADDLE, 1), 6, 10, 0.05F));
      trades.add(new FixedTrade(new ItemStack(Items.EMERALD, 3), new ItemStack(Items.LEAD, 4), 12, 5, 0.05F));
      trades.add(new FixedTrade(new ItemStack(Items.EMERALD, 6), new ItemStack(Items.IRON_AXE, 1), 12, 5, 0.05F));
      return trades;
   }

   private List<ItemListing> getRareTrades() {
      List<ItemListing> trades = new ArrayList<>();
      trades.add(
         new FixedTrade(
            new ItemStack(Items.EMERALD, 60), new ItemStack(Items.STONE, 48), new ItemStack((ItemLike)ModItems.CAVE_ROOST_MAP.get()), 1, 20, 0.05F
         )
      );
      trades.add(
         new FixedTrade(
            new ItemStack(Items.EMERALD, 60), new ItemStack(Items.IRON_INGOT, 24), new ItemStack((ItemLike)ModItems.GLADE_ROOST_MAP.get()), 1, 20, 0.05F
         )
      );
      trades.add(
         new FixedTrade(
            new ItemStack(Items.EMERALD, 60), new ItemStack(Items.ENDER_PEARL, 12), new ItemStack((ItemLike)ModItems.WETLAND_ROOST_MAP.get()), 1, 20, 0.05F
         )
      );
      trades.add(
         new FixedTrade(
            new ItemStack(Items.EMERALD, 60), new ItemStack(Items.BLAZE_POWDER, 12), new ItemStack((ItemLike)ModItems.BARRENS_ROOST_MAP.get()), 1, 20, 0.05F
         )
      );
      trades.add(
         new FixedTrade(
            new ItemStack(Items.EMERALD, 60), new ItemStack(Items.DIAMOND, 12), new ItemStack((ItemLike)ModItems.CLIFF_ROOST_MAP.get()), 1, 20, 0.05F
         )
      );
      return trades;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.WANDERING_TRADER_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource damageSource) {
      return SoundEvents.WANDERING_TRADER_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.WANDERING_TRADER_DEATH;
   }

   protected SoundEvent getTradeUpdatedSound(boolean isYes) {
      return isYes ? SoundEvents.WANDERING_TRADER_YES : SoundEvents.WANDERING_TRADER_NO;
   }

   public SoundEvent getNotifyTradeSound() {
      return SoundEvents.WANDERING_TRADER_YES;
   }
}

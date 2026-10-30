package net.magister.liberanimalium.body.gameplay;

import com.google.gson.JsonObject;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.platform.registry.Regs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record TamingPredicate(TamingPredicate.Type type, @Nullable ResourceLocation id) {
   public boolean test(FaunaBase body, Player player) {
      return switch (this.type) {
         case HOLDING_TAG -> this.id != null && holds(player, stack -> stack.is(itemTag(this.id)));
         case HOLDING_ITEM -> this.id != null && holds(player, stack -> this.id.equals(Regs.itemKey(stack.getItem())));
         case NOT_ON_FIRE -> !body.isOnFire();
      };
   }

   private static TagKey<Item> itemTag(ResourceLocation id) {
      return TagKey.create(Registries.ITEM, id);
   }

   private static boolean holds(Player player, Predicate<ItemStack> match) {
      for (ItemStack stack : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
         if (!stack.isEmpty() && match.test(stack)) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   public static TamingPredicate parse(JsonObject json) {
      TamingPredicate.Type type = TamingPredicate.Type.of(GsonHelper.getAsString(json, "type", ""));
      if (type == null) {
         return null;
      }

      return switch (type) {
         case HOLDING_TAG -> resolved(type, GsonHelper.getAsString(json, "tag", ""));
         case HOLDING_ITEM -> resolved(type, GsonHelper.getAsString(json, "item", ""));
         case NOT_ON_FIRE -> new TamingPredicate(type, null);
      };
   }

   @Nullable
   public static TamingPredicate forKey(@Nullable String key) {
      if (key != null && !key.isBlank()) {
         boolean tag = key.startsWith("#");
         return resolved(tag ? TamingPredicate.Type.HOLDING_TAG : TamingPredicate.Type.HOLDING_ITEM, tag ? key.substring(1) : key);
      } else {
         return null;
      }
   }

   @Nullable
   private static TamingPredicate resolved(TamingPredicate.Type type, String raw) {
      ResourceLocation rl = ResourceLocation.tryParse(raw);
      return rl == null ? null : new TamingPredicate(type, rl);
   }

   public enum Type {
      HOLDING_TAG("holding_tag"),
      HOLDING_ITEM("holding_item"),
      NOT_ON_FIRE("not_on_fire");

      private final String key;

      Type(String key) {
         this.key = key;
      }

      public String key() {
         return this.key;
      }

      @Nullable
      public static TamingPredicate.Type of(String key) {
         for (TamingPredicate.Type t : values()) {
            if (t.key.equals(key)) {
               return t;
            }
         }

         return null;
      }
   }
}

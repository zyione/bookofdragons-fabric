package net.magister.bookofdragons.client.gui.book.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

public final class RiderManual {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation FILE = new ResourceLocation("bookofdragons", "book/rider_manual.json");

   private RiderManual() {
   }

   public static List<RiderManual.Page> pages() {
      Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(FILE);
      if (resource.isEmpty()) {
         return List.of();
      }

      try (Reader reader = resource.get().openAsReader()) {
         JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
         List<RiderManual.Page> pages = new ArrayList<>();

         for (JsonElement p : root.getAsJsonArray("pages")) {
            JsonObject page = p.getAsJsonObject();
            List<RiderManual.Entry> entries = new ArrayList<>();

            for (JsonElement e : page.getAsJsonArray("entries")) {
               entries.add(entry(e.getAsJsonObject()));
            }

            pages.add(new RiderManual.Page(page.get("title").getAsString(), entries));
         }

         return pages;
      } catch (Exception unreadable) {
         LOGGER.warn("{} could not be read: {}", FILE, unreadable.toString());
         return List.of();
      }
   }

   private static RiderManual.Entry entry(JsonObject entry) {
      List<String> keys = new ArrayList<>();
      if (entry.has("keys")) {
         for (JsonElement k : entry.getAsJsonArray("keys")) {
            keys.add(k.getAsString());
         }
      }

      String join = entry.has("join") ? entry.get("join").getAsString() : " ";
      RiderManual.Press press = entry.has("press")
         ? RiderManual.Press.valueOf(entry.get("press").getAsString().toUpperCase(Locale.ROOT))
         : RiderManual.Press.TAP;
      boolean mouse = entry.has("mouse") && entry.get("mouse").getAsBoolean();
      boolean dragon = entry.has("dragon") && entry.get("dragon").getAsBoolean();
      return new RiderManual.Entry(item(entry, "held"), keys, join, press, mouse, item(entry, "at"), dragon, entry.get("text").getAsString());
   }

   private static ItemStack item(JsonObject entry, String field) {
      if (!entry.has(field)) {
         return ItemStack.EMPTY;
      } else {
         ResourceLocation id = ResourceLocation.tryParse(entry.get(field).getAsString());
         if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
            Item item = (Item)BuiltInRegistries.ITEM.get(id);
            return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
         } else {
            return ItemStack.EMPTY;
         }
      }
   }

   public static RiderManual.Bound bound(String name) {
      for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
         if (mapping.getName().equals(name)) {
            Key key = ((net.magister.liberanimalium.mixin.KeyMappingAccessor)mapping).liberanimalium$key();
            if (key.getType() == Type.MOUSE && key.getValue() <= 2) {
               return new RiderManual.Bound(key.getValue(), mapping.getTranslatedKeyMessage());
            }

            return new RiderManual.Bound(-1, mapping.getTranslatedKeyMessage());
         }
      }

      return new RiderManual.Bound(-1, Component.literal(name));
   }

   @Nullable
   public static Component pressMark(RiderManual.Press press) {
      return switch (press) {
         case HOLD -> Component.translatable("book.bookofdragons.manual.press.hold");
         case TWICE -> Component.translatable("book.bookofdragons.manual.press.twice");
         case THRICE -> Component.translatable("book.bookofdragons.manual.press.thrice");
         default -> null;
      };
   }

   public record Bound(int mouseButton, Component label) {
   }

   public record Entry(ItemStack held, List<String> keys, String join, RiderManual.Press press, boolean mouse, ItemStack at, boolean dragon, String text) {
   }

   public record Page(String title, List<RiderManual.Entry> entries) {
   }

   public enum Press {
      TAP,
      HOLD,
      TWICE,
      THRICE;
   }
}

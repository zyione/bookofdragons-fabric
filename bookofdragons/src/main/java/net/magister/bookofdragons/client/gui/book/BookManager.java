package net.magister.bookofdragons.client.gui.book;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.magister.bookofdragons.client.gui.book.data.RiderManual;
import net.magister.bookofdragons.client.gui.book.spread.DragonListSpread;
import net.magister.bookofdragons.client.gui.book.spread.DragonPageSpread;
import net.magister.bookofdragons.client.gui.book.spread.DragonRelationsSpread;
import net.magister.bookofdragons.client.gui.book.spread.DragonSpeciesSpread;
import net.magister.bookofdragons.client.gui.book.spread.EditableBookSpread;
import net.magister.bookofdragons.client.gui.book.spread.IBookSpread;
import net.magister.bookofdragons.client.gui.book.spread.IndexSpread;
import net.magister.bookofdragons.client.gui.book.spread.RiderManualSpread;
import net.magister.bookofdragons.client.gui.book.spread.SpeciesPagesSpread;
import net.magister.bookofdragons.client.gui.book.spread.VariantSpread;
import net.magister.bookofdragons.datagen.data.DragonDiscoveryData;
import net.magister.bookofdragons.discovery.DiscoveryStore;
import net.magister.bookofdragons.entity.base.dragon.DragonClass;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.network.ControlNetwork;
import net.magister.bookofdragons.network.message.SaveNotesMessage;
import net.magister.bookofdragons.network.message.SyncTamedDragonsMessage;
import net.magister.liberanimalium.variant.CreatureVariant;
import net.magister.liberanimalium.variant.CreatureVariants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class BookManager {
   private final List<IBookSpread> spreads = new ArrayList<>();
   private final List<String> keys = new ArrayList<>();
   private final List<BookTabs.Tab> tabs = new ArrayList<>();
   private final Map<DragonType, NotesPad> pads = new EnumMap<>(DragonType.class);
   private final Map<UUID, NotesPad> dragonPads = new HashMap<>();
   private final Map<String, NotesPad> variantPads = new HashMap<>();
   private UUID openDragon;
   private int currentIndex = 0;
   private int builtFor = -1;

   public void init() {
      if (!this.spreads.isEmpty()) {
         this.build();
      } else {
         Minecraft mc = Minecraft.getInstance();
         DragonDiscoveryData data = mc.player == null ? new DragonDiscoveryData() : DiscoveryStore.get(mc.player).orElse(new DragonDiscoveryData());

         for (DragonType type : DragonType.values()) {
            this.pads.put(type, new NotesPad(data.notes(type), () -> this.pagesChanged(type.name(), this.pads.get(type)), clean -> {
               ControlNetwork.sendToServer(SaveNotesMessage.species(type, clean));
               keep(stored -> stored.setNotes(type, clean));
            }));
         }

         this.currentIndex = 0;
         this.build();
      }
   }

   private void build() {
      String at = this.keys.isEmpty() ? null : this.keys.get(this.currentIndex);
      this.spreads.clear();
      this.keys.clear();
      this.tabs.clear();
      this.add("index", new IndexSpread(this));
      List<RiderManual.Page> manual = RiderManual.pages();

      for (int first = 0; first < manual.size(); first += 2) {
         this.add("manual:" + first, new RiderManualSpread(manual, first));
      }

      for (DragonClass dragonClass : ClassProgress.present()) {
         this.tabs.add(new BookTabs.Tab(dragonClass, this.spreads.size()));

         for (DragonType type : ClassProgress.species(dragonClass)) {
            NotesPad pad = this.pads.get(type);
            this.add(type.name() + ":identity", new DragonSpeciesSpread(type));
            this.add(type.name() + ":notes:0", new SpeciesPagesSpread(type, pad, 0));

            for (int first = 1; first < pad.pageCount(); first += 2) {
               this.add(type.name() + ":notes:" + first, new SpeciesPagesSpread(type, pad, first));
            }

            for (CreatureVariant variant : seenVariants(type)) {
               String key = "variant:" + variant.id();
               NotesPad variantPad = this.variantPad(variant.id().toString());
               this.add(key + ":notes:0", new VariantSpread(variant, type, variantPad, 0));

               for (int first = 1; first < variantPad.pageCount(); first += 2) {
                  this.add(key + ":notes:" + first, new VariantSpread(variant, type, variantPad, first));
               }
            }
         }
      }

      this.builtFor = ClientDragonRegistry.version();
      int lists = Math.max(1, (ClientDragonRegistry.getDragons().size() + 10 - 1) / 10);

      for (int i = 0; i < lists; i++) {
         this.add("dragons:" + i, new DragonListSpread(this, i));
      }

      if (this.openDragon != null && owned(this.openDragon)) {
         String key = "dragon:" + this.openDragon;
         NotesPad pad = this.dragonPad(this.openDragon);
         this.add(key + ":main", new DragonPageSpread(this.openDragon));
         this.add(key + ":notes:0", new DragonRelationsSpread(this.openDragon, pad, 0));

         for (int first = 1; first < pad.pageCount(); first += 2) {
            this.add(key + ":notes:" + first, new DragonRelationsSpread(this.openDragon, pad, first));
         }
      }

      this.currentIndex = at == null ? 0 : Math.max(0, this.keys.indexOf(at));
   }

   private void add(String key, IBookSpread spread) {
      this.keys.add(key);
      this.spreads.add(spread);
   }

   public void refresh() {
      if (this.builtFor != ClientDragonRegistry.version()) {
         this.build();
      }
   }

   private static boolean owned(UUID dragon) {
      for (SyncTamedDragonsMessage.TamedDragonRecord record : ClientDragonRegistry.getDragons()) {
         if (record.uuid().equals(dragon)) {
            return true;
         }
      }

      return false;
   }

   private static void keep(Consumer<DragonDiscoveryData> change) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         DiscoveryStore.get(mc.player).ifPresent(change);
      }
   }

   private static List<CreatureVariant> seenVariants(DragonType type) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return List.of();
      }

      DragonDiscoveryData data = DiscoveryStore.get(mc.player).orElse(null);
      if (data == null) {
         return List.of();
      }

      List<CreatureVariant> seen = new ArrayList<>();

      for (String id : data.seenVariants()) {
         ResourceLocation location = ResourceLocation.tryParse(id);
         CreatureVariant variant = location == null ? null : CreatureVariants.get(location);
         if (variant != null && variant.type() == type.getEntityType()) {
            seen.add(variant);
         }
      }

      return seen;
   }

   private NotesPad variantPad(String variant) {
      return this.variantPads.computeIfAbsent(variant, id -> {
         Minecraft mc = Minecraft.getInstance();
         List<String> saved = mc.player == null ? List.of() : DiscoveryStore.get(mc.player).map(data -> data.variantNotes(id)).orElse(List.of());
         return new NotesPad(saved, () -> this.pagesChanged("variant:" + id, this.variantPads.get(id)), clean -> {
            ControlNetwork.sendToServer(SaveNotesMessage.variant(id, clean));
            keep(stored -> stored.setVariantNotes(id, clean));
         });
      });
   }

   private NotesPad dragonPad(UUID dragon) {
      return this.dragonPads.computeIfAbsent(dragon, id -> {
         Minecraft mc = Minecraft.getInstance();
         List<String> saved = mc.player == null ? List.of() : DiscoveryStore.get(mc.player).map(data -> data.dragonNotes(id)).orElse(List.of());
         return new NotesPad(saved, () -> this.pagesChanged("dragon:" + id, this.dragonPads.get(id)), clean -> {
            ControlNetwork.sendToServer(SaveNotesMessage.dragon(id, clean));
            keep(stored -> stored.setDragonNotes(id, clean));
         });
      });
   }

   public void openDragon(UUID dragon) {
      if (owned(dragon)) {
         this.openDragon = dragon;
         this.build();
         int at = this.keys.indexOf("dragon:" + dragon + ":main");
         if (at >= 0) {
            this.navigateTo(at);
         }
      }
   }

   private void pagesChanged(String prefix, NotesPad pad) {
      int page = pad.focusedPage();
      this.build();
      int first = page <= 0 ? 0 : (page % 2 == 1 ? page : page - 1);
      int at = this.keys.indexOf(prefix + ":notes:" + first);
      if (at >= 0) {
         this.currentIndex = at;
      }

      this.currentIndex = Math.min(this.currentIndex, this.spreads.size() - 1);
   }

   public IBookSpread getCurrent() {
      return this.spreads.get(this.currentIndex);
   }

   public List<BookTabs.Tab> tabs() {
      return this.tabs;
   }

   public boolean canGoNext() {
      return this.currentIndex < this.spreads.size() - 1;
   }

   public boolean canGoPrev() {
      return this.currentIndex > 0;
   }

   public void next() {
      if (this.canGoNext()) {
         this.navigateTo(this.currentIndex + 1);
      }
   }

   public void prev() {
      if (this.canGoPrev()) {
         this.navigateTo(this.currentIndex - 1);
      }
   }

   public void navigateTo(int index) {
      if (index >= 0 && index < this.spreads.size() && index != this.currentIndex) {
         this.spreads.get(this.currentIndex).left();
         this.currentIndex = index;
      }
   }

   public void saveAll() {
      List<NotesPad> all = new ArrayList<>(this.pads.values());
      all.addAll(this.dragonPads.values());
      all.addAll(this.variantPads.values());

      for (NotesPad pad : all) {
         pad.unfocus();
         pad.save();
      }
   }

   public int getCurrentIndex() {
      return this.currentIndex;
   }

   public int size() {
      return this.spreads.size();
   }

   public int manualStart() {
      return this.keys.indexOf("manual:0");
   }

   public int getTamedDragonsChapterIndex() {
      return Math.max(0, this.keys.indexOf("dragons:0"));
   }

   public int classStart(DragonClass dragonClass) {
      for (BookTabs.Tab tab : this.tabs) {
         if (tab.dragonClass() == dragonClass) {
            return tab.start();
         }
      }

      return -1;
   }

   public void dumpAllEditors() {
      IdentityHashMap<BookLayoutEditor, Boolean> seen = new IdentityHashMap<>();

      for (IBookSpread spread : this.spreads) {
         if (spread instanceof EditableBookSpread editable) {
            BookLayoutEditor editor = editable.getLayoutEditor();
            if (seen.putIfAbsent(editor, Boolean.TRUE) == null) {
               editor.dump();
            }
         }
      }
   }
}

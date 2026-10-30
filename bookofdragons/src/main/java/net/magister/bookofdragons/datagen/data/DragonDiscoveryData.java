package net.magister.bookofdragons.datagen.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Map.Entry;
import net.magister.bookofdragons.discovery.DiscoveryLevel;
import net.magister.bookofdragons.discovery.PortraitVariant;
import net.magister.bookofdragons.entity.data.DragonType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

public class DragonDiscoveryData {
   private final Map<DragonType, Boolean> hasEncountered = new EnumMap<>(DragonType.class);
   private final Map<DragonType, Boolean> hasKilled = new EnumMap<>(DragonType.class);
   private final Map<DragonType, Boolean> hasTamed = new EnumMap<>(DragonType.class);
   private final Map<DragonType, Boolean> isBonded = new EnumMap<>(DragonType.class);
   private final Map<DragonType, List<String>> notes = new EnumMap<>(DragonType.class);
   private final Map<DragonType, Set<String>> facts = new EnumMap<>(DragonType.class);
   private final Map<UUID, List<String>> dragonNotes = new HashMap<>();
   private final Set<String> seenVariants = new TreeSet<>();
   private final Map<String, List<String>> variantNotes = new HashMap<>();
   public static final int MAX_NOTE_PAGES = 100;
   public static final int MAX_NOTE_CHARS = 1024;

   public DragonDiscoveryData() {
      for (DragonType type : DragonType.values()) {
         this.hasEncountered.put(type, false);
         this.hasKilled.put(type, false);
         this.hasTamed.put(type, false);
         this.isBonded.put(type, false);
      }
   }

   public boolean hasEncountered(DragonType type) {
      return this.hasEncountered.getOrDefault(type, false);
   }

   public boolean hasKilled(DragonType type) {
      return this.hasKilled.getOrDefault(type, false);
   }

   public boolean hasTamed(DragonType type) {
      return this.hasTamed.getOrDefault(type, false);
   }

   public boolean isBonded(DragonType type) {
      return this.isBonded.getOrDefault(type, false);
   }

   public DiscoveryLevel getDiscoveryLevel(DragonType type) {
      boolean encountered = this.hasEncountered(type);
      boolean killed = this.hasKilled(type);
      boolean tamed = this.hasTamed(type);
      boolean bonded = this.isBonded(type);
      DiscoveryLevel flagLevel = DiscoveryLevel.fromFlags(killed, tamed, bonded);
      if (flagLevel != DiscoveryLevel.UNDISCOVERED) {
         return flagLevel;
      } else {
         return encountered ? DiscoveryLevel.ENCOUNTERED : DiscoveryLevel.UNDISCOVERED;
      }
   }

   public PortraitVariant getDisplayPortrait(DragonType type) {
      return PortraitVariant.getDisplayVariant(this.hasTamed(type));
   }

   public void setEncountered(DragonType type, boolean value) {
      this.hasEncountered.put(type, value);
   }

   public void setKilled(DragonType type, boolean value) {
      this.hasKilled.put(type, value);
   }

   public void setTamed(DragonType type, boolean value) {
      this.hasTamed.put(type, value);
   }

   public void setBonded(DragonType type, boolean value) {
      this.isBonded.put(type, value);
   }

   public void markEncountered(DragonType type) {
      this.setEncountered(type, true);
   }

   public void markKilled(DragonType type) {
      this.setEncountered(type, true);
      this.setKilled(type, true);
   }

   public void markTamed(DragonType type) {
      this.setEncountered(type, true);
      this.setTamed(type, true);
   }

   public void markBonded(DragonType type) {
      if (this.hasTamed(type)) {
         this.setBonded(type, true);
      }
   }

   public Set<String> facts(DragonType type) {
      Set<String> known = this.facts.get(type);
      return known == null ? Set.of() : Collections.unmodifiableSet(known);
   }

   public boolean learn(DragonType type, String fact) {
      return this.facts.computeIfAbsent(type, k -> new TreeSet<>()).add(fact);
   }

   public List<String> notes(DragonType type) {
      return this.notes.getOrDefault(type, List.of());
   }

   public void setNotes(DragonType type, List<String> pages) {
      List<String> clean = cleanNotes(pages);
      if (clean.isEmpty()) {
         this.notes.remove(type);
      } else {
         this.notes.put(type, clean);
      }
   }

   public List<String> dragonNotes(UUID dragon) {
      return this.dragonNotes.getOrDefault(dragon, List.of());
   }

   public void setDragonNotes(UUID dragon, List<String> pages) {
      List<String> clean = cleanNotes(pages);
      if (clean.isEmpty()) {
         this.dragonNotes.remove(dragon);
      } else {
         this.dragonNotes.put(dragon, clean);
      }
   }

   public boolean hasSeenVariant(String variant) {
      return this.seenVariants.contains(variant);
   }

   public boolean markVariantSeen(String variant) {
      return this.seenVariants.add(variant);
   }

   public Set<String> seenVariants() {
      return Collections.unmodifiableSet(this.seenVariants);
   }

   public List<String> variantNotes(String variant) {
      return this.variantNotes.getOrDefault(variant, List.of());
   }

   public void setVariantNotes(String variant, List<String> pages) {
      List<String> clean = cleanNotes(pages);
      if (clean.isEmpty()) {
         this.variantNotes.remove(variant);
      } else {
         this.variantNotes.put(variant, clean);
      }
   }

   public static List<String> cleanNotes(List<String> pages) {
      List<String> out = new ArrayList<>();

      for (String page : pages) {
         if (out.size() == 100) {
            break;
         }

         StringBuilder s = new StringBuilder();

         for (int i = 0; i < page.length() && s.length() < 1024; i++) {
            char c = page.charAt(i);
            if (c == '\n' || c >= ' ' && c != 127 && c != 167) {
               s.append(c);
            }
         }

         out.add(s.toString());
      }

      while (!out.isEmpty() && out.get(out.size() - 1).isEmpty()) {
         out.remove(out.size() - 1);
      }

      return List.copyOf(out);
   }

   public void saveToNBT(CompoundTag tag) {
      for (DragonType type : DragonType.values()) {
         String prefix = type.name().toLowerCase();
         tag.putBoolean(prefix + "_encountered", this.hasEncountered.get(type));
         tag.putBoolean(prefix + "_killed", this.hasKilled.get(type));
         tag.putBoolean(prefix + "_tamed", this.hasTamed.get(type));
         tag.putBoolean(prefix + "_bonded", this.isBonded.get(type));
      }

      CompoundTag all = new CompoundTag();

      for (Entry<DragonType, List<String>> e : this.notes.entrySet()) {
         ListTag pages = new ListTag();

         for (String page : e.getValue()) {
            pages.add(StringTag.valueOf(page));
         }

         all.put(e.getKey().name().toLowerCase(), pages);
      }

      if (!all.isEmpty()) {
         tag.put("notes", all);
      }

      CompoundTag learned = new CompoundTag();

      for (Entry<DragonType, Set<String>> e : this.facts.entrySet()) {
         ListTag list = new ListTag();

         for (String fact : e.getValue()) {
            list.add(StringTag.valueOf(fact));
         }

         if (!list.isEmpty()) {
            learned.put(e.getKey().name().toLowerCase(), list);
         }
      }

      if (!learned.isEmpty()) {
         tag.put("facts", learned);
      }

      CompoundTag dragons = new CompoundTag();

      for (Entry<UUID, List<String>> e : this.dragonNotes.entrySet()) {
         ListTag pages = new ListTag();

         for (String page : e.getValue()) {
            pages.add(StringTag.valueOf(page));
         }

         dragons.put(e.getKey().toString(), pages);
      }

      if (!dragons.isEmpty()) {
         tag.put("dragon_notes", dragons);
      }

      if (!this.seenVariants.isEmpty()) {
         ListTag seen = new ListTag();

         for (String variant : this.seenVariants) {
            seen.add(StringTag.valueOf(variant));
         }

         tag.put("seen_variants", seen);
      }

      CompoundTag variants = new CompoundTag();

      for (Entry<String, List<String>> e : this.variantNotes.entrySet()) {
         ListTag pages = new ListTag();

         for (String page : e.getValue()) {
            pages.add(StringTag.valueOf(page));
         }

         variants.put(e.getKey(), pages);
      }

      if (!variants.isEmpty()) {
         tag.put("variant_notes", variants);
      }
   }

   public void loadFromNBT(CompoundTag tag) {
      for (DragonType type : DragonType.values()) {
         String prefix = type.name().toLowerCase();
         this.hasEncountered.put(type, tag.getBoolean(prefix + "_encountered"));
         this.hasKilled.put(type, tag.getBoolean(prefix + "_killed"));
         this.hasTamed.put(type, tag.getBoolean(prefix + "_tamed"));
         this.isBonded.put(type, tag.getBoolean(prefix + "_bonded"));
      }

      this.notes.clear();
      if (tag.contains("notes", 10)) {
         CompoundTag all = tag.getCompound("notes");

         for (DragonType type : DragonType.values()) {
            String key = type.name().toLowerCase();
            if (all.contains(key, 9)) {
               ListTag list = all.getList(key, 8);
               List<String> pages = new ArrayList<>();

               for (int i = 0; i < list.size(); i++) {
                  pages.add(list.getString(i));
               }

               this.setNotes(type, pages);
            }
         }
      }

      this.facts.clear();
      if (tag.contains("facts", 10)) {
         CompoundTag learned = tag.getCompound("facts");

         for (DragonType type : DragonType.values()) {
            String key = type.name().toLowerCase();
            if (learned.contains(key, 9)) {
               ListTag list = learned.getList(key, 8);

               for (int i = 0; i < list.size(); i++) {
                  this.learn(type, list.getString(i));
               }
            }
         }
      }

      this.dragonNotes.clear();
      if (tag.contains("dragon_notes", 10)) {
         CompoundTag dragons = tag.getCompound("dragon_notes");
         Iterator var19 = dragons.getAllKeys().iterator();

         label85:
         while (true) {
            String key;
            UUID id;
            while (true) {
               if (!var19.hasNext()) {
                  break label85;
               }

               key = (String)var19.next();

               try {
                  id = UUID.fromString(key);
                  break;
               } catch (IllegalArgumentException e) {
               }
            }

            ListTag list = dragons.getList(key, 8);
            List<String> pages = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
               pages.add(list.getString(i));
            }

            this.setDragonNotes(id, pages);
         }
      }

      this.seenVariants.clear();
      if (tag.contains("seen_variants", 9)) {
         ListTag seen = tag.getList("seen_variants", 8);

         for (int i = 0; i < seen.size(); i++) {
            this.seenVariants.add(seen.getString(i));
         }
      }

      this.variantNotes.clear();
      if (tag.contains("variant_notes", 10)) {
         CompoundTag variants = tag.getCompound("variant_notes");

         for (String key : variants.getAllKeys()) {
            ListTag list = variants.getList(key, 8);
            List<String> pages = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
               pages.add(list.getString(i));
            }

            this.setVariantNotes(key, pages);
         }
      }
   }

   public void copyFrom(DragonDiscoveryData other) {
      for (DragonType type : DragonType.values()) {
         this.hasEncountered.put(type, other.hasEncountered.get(type));
         this.hasKilled.put(type, other.hasKilled.get(type));
         this.hasTamed.put(type, other.hasTamed.get(type));
         this.isBonded.put(type, other.isBonded.get(type));
      }

      this.notes.clear();
      this.notes.putAll(other.notes);
      this.facts.clear();

      for (Entry<DragonType, Set<String>> e : other.facts.entrySet()) {
         this.facts.put(e.getKey(), new TreeSet<>(e.getValue()));
      }

      this.dragonNotes.clear();
      this.dragonNotes.putAll(other.dragonNotes);
      this.seenVariants.clear();
      this.seenVariants.addAll(other.seenVariants);
      this.variantNotes.clear();
      this.variantNotes.putAll(other.variantNotes);
   }
}

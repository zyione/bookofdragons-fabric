package net.magister.bookofdragons.entity.custom.toy;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.anim.rig.RigSpec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public enum PlushKind {
   SHEEP("sheep_plush", "sheep_plush", "sheep_plush", 0.9F, 0.9F, 1.0F, PlushKind.Rest.ANY, "plush_low_squeak", 1.0F, true),
   FURY("fury_plush", "fury_plush", "nightfury_plush", 0.6F, 0.85F, 0.6F, PlushKind.Rest.ANY, "plush_squeak", 1.0F, true),
   FISH("fish_plush", "fish_plush", "fish_plush", 0.8F, 0.1875F, 0.5F, PlushKind.Rest.FLAT, "animal_squeak", 1.0F, false),
   STEAK("steak_plush", "steak_plush", "steak_plush", 0.55F, 0.5F, 0.7F, PlushKind.Rest.LYING, "animal_squeak", 1.0F, false),
   BALL("ball_plush", "ball_plush", "ball_plush", 0.875F, 0.875F, 0.6F, PlushKind.Rest.ANY, "ball_squeak", 1.0F, false, 0.85F, 0.93F),
   TERROR("terror_plush", "terror_plush", "terror_plush", 0.6F, 0.3F, 0.4F, PlushKind.Rest.ANY, "plush_squeak", 1.0F, true, 0.25F, 0.55F),
   NIGHTMARE("nightmare_plush", "nightmare_plush", "nightmare_plush", 0.9F, 0.55F, 0.9F, PlushKind.Rest.ANY, "plush_low_squeak", 1.0F, true, 0.25F, 0.55F);

   private final String id;
   private final String model;
   private final float width;
   private final float height;
   private final float mass;
   private final PlushKind.Rest rest;
   @Nullable
   private final ResourceLocation squeak;
   private final float squeakPitch;
   private final boolean dyeable;
   private final float restitution;
   private final float grip;
   private final ResourceLocation modelFile;
   private final ResourceLocation textureFile;
   private final ResourceLocation woolFile;
   private final ResourceLocation rigFile;
   private volatile RigSpec rig = RigSpec.EMPTY;

   PlushKind(
      String id,
      String model,
      String texture,
      float width,
      float height,
      float mass,
      PlushKind.Rest rest,
      @Nullable String squeak,
      float squeakPitch,
      boolean dyeable
   ) {
      this(id, model, texture, width, height, mass, rest, squeak, squeakPitch, dyeable, 0.55F, 0.7F);
   }

   PlushKind(
      String id,
      String model,
      String texture,
      float width,
      float height,
      float mass,
      PlushKind.Rest rest,
      @Nullable String squeak,
      float squeakPitch,
      boolean dyeable,
      float restitution,
      float grip
   ) {
      this.restitution = restitution;
      this.grip = grip;
      this.id = id;
      this.model = model;
      this.width = width;
      this.height = height;
      this.mass = mass;
      this.rest = rest;
      this.squeak = squeak == null ? null : asset(squeak);
      this.squeakPitch = squeakPitch;
      this.dyeable = dyeable;
      this.modelFile = asset("geo/misc/" + model + ".geo.json");
      this.textureFile = asset("textures/entity/misc/" + texture + ".png");
      this.woolFile = asset("textures/entity/misc/" + texture + "_wool.png");
      this.rigFile = asset("rigs/" + model + ".json");
   }

   private static ResourceLocation asset(String path) {
      return new ResourceLocation("bookofdragons", path);
   }

   public String id() {
      return this.id;
   }

   public boolean dragon() {
      return this == FURY || this == TERROR || this == NIGHTMARE;
   }

   public String model() {
      return this.model;
   }

   public float width() {
      return this.width;
   }

   public float height() {
      return this.height;
   }

   public float mass() {
      return this.mass;
   }

   public PlushKind.Rest rest() {
      return this.rest;
   }

   @Nullable
   public SoundEvent squeak() {
      return this.squeak == null ? null : (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(this.squeak);
   }

   @Nullable
   public ResourceLocation squeakId() {
      return this.squeak;
   }

   public float squeakPitch() {
      return this.squeakPitch;
   }

   public float restitution() {
      return this.restitution;
   }

   public float grip() {
      return this.grip;
   }

   public boolean dyeable() {
      return this.dyeable;
   }

   public ResourceLocation modelFile() {
      return this.modelFile;
   }

   public ResourceLocation textureFile() {
      return this.textureFile;
   }

   public ResourceLocation woolFile() {
      return this.woolFile;
   }

   public ResourceLocation rigFile() {
      return this.rigFile;
   }

   public RigSpec rig() {
      return this.rig;
   }

   public void rig(RigSpec spec) {
      this.rig = spec == null ? RigSpec.EMPTY : spec;
   }

   public Item item() {
      return (Item)BuiltInRegistries.ITEM.get(asset(this.id));
   }

   public static PlushKind of(EntityType<?> type) {
      String path = EntityType.getKey(type).getPath();

      for (PlushKind kind : values()) {
         if (kind.id.equals(path)) {
            return kind;
         }
      }

      return SHEEP;
   }

   public enum Rest {
      ANY,
      LYING,
      FLAT;
   }
}

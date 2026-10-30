package net.magister.liberanimalium.fauna.api;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;

public record SensedBody(
   UUID id,
   Kind kind,
   Vec3 position,
   Vec3 eyePosition,
   float width,
   float height,
   boolean sneaking,
   boolean startled,
   boolean inWater,
   int lightLevel,
   boolean losClear,
   float heldMenace,
   float armorFraction,
   boolean owned,
   @Nullable UUID leashHolder,
   boolean tethered,
   @Nullable UUID providedBy,
   boolean playful,
   List<Kind> leads,
   boolean answers,
   boolean pressing
) {
   public SensedBody {
      leads = leads == null ? List.of() : List.copyOf(leads);
   }

   public SensedBody(
      UUID id,
      Kind kind,
      Vec3 position,
      Vec3 eyePosition,
      float width,
      float height,
      boolean sneaking,
      boolean startled,
      boolean inWater,
      int lightLevel,
      boolean losClear,
      float heldMenace,
      float armorFraction,
      boolean owned,
      @Nullable UUID leashHolder,
      boolean tethered,
      @Nullable UUID providedBy,
      boolean playful,
      List<Kind> leads,
      boolean answers
   ) {
      this(
         id,
         kind,
         position,
         eyePosition,
         width,
         height,
         sneaking,
         startled,
         inWater,
         lightLevel,
         losClear,
         heldMenace,
         armorFraction,
         owned,
         leashHolder,
         tethered,
         providedBy,
         playful,
         leads,
         answers,
         false
      );
   }

   public SensedBody(
      UUID id,
      Kind kind,
      Vec3 position,
      Vec3 eyePosition,
      float width,
      float height,
      boolean sneaking,
      boolean startled,
      boolean inWater,
      int lightLevel,
      boolean losClear,
      float heldMenace,
      float armorFraction,
      boolean owned,
      @Nullable UUID leashHolder,
      boolean tethered,
      @Nullable UUID providedBy,
      boolean playful,
      List<Kind> leads
   ) {
      this(
         id,
         kind,
         position,
         eyePosition,
         width,
         height,
         sneaking,
         startled,
         inWater,
         lightLevel,
         losClear,
         heldMenace,
         armorFraction,
         owned,
         leashHolder,
         tethered,
         providedBy,
         playful,
         leads,
         true
      );
   }

   public SensedBody(
      UUID id,
      Kind kind,
      Vec3 position,
      Vec3 eyePosition,
      float width,
      float height,
      boolean sneaking,
      boolean startled,
      boolean inWater,
      int lightLevel,
      boolean losClear,
      float heldMenace,
      float armorFraction,
      boolean owned,
      @Nullable UUID leashHolder,
      boolean tethered,
      @Nullable UUID providedBy,
      boolean playful
   ) {
      this(
         id,
         kind,
         position,
         eyePosition,
         width,
         height,
         sneaking,
         startled,
         inWater,
         lightLevel,
         losClear,
         heldMenace,
         armorFraction,
         owned,
         leashHolder,
         tethered,
         providedBy,
         playful,
         List.of()
      );
   }

   public SensedBody(
      UUID id,
      Kind kind,
      Vec3 position,
      Vec3 eyePosition,
      float width,
      float height,
      boolean sneaking,
      boolean startled,
      boolean inWater,
      int lightLevel,
      boolean losClear,
      float heldMenace,
      float armorFraction,
      boolean owned,
      @Nullable UUID leashHolder,
      boolean tethered,
      @Nullable UUID providedBy
   ) {
      this(
         id,
         kind,
         position,
         eyePosition,
         width,
         height,
         sneaking,
         startled,
         inWater,
         lightLevel,
         losClear,
         heldMenace,
         armorFraction,
         owned,
         leashHolder,
         tethered,
         providedBy,
         false
      );
   }

   public SensedBody(
      UUID id,
      Kind kind,
      Vec3 position,
      Vec3 eyePosition,
      float width,
      float height,
      boolean sneaking,
      boolean startled,
      boolean inWater,
      int lightLevel,
      boolean losClear,
      float heldMenace,
      float armorFraction,
      boolean owned
   ) {
      this(
         id, kind, position, eyePosition, width, height, sneaking, startled, inWater, lightLevel, losClear, heldMenace, armorFraction, owned, null, false, null
      );
   }

   public SensedBody(
      UUID id,
      Kind kind,
      Vec3 position,
      Vec3 eyePosition,
      float width,
      float height,
      boolean sneaking,
      boolean startled,
      boolean inWater,
      int lightLevel,
      boolean losClear
   ) {
      this(id, kind, position, eyePosition, width, height, sneaking, startled, inWater, lightLevel, losClear, 0.0F, 0.0F, false);
   }

   public SensedBody(
      UUID id,
      Kind kind,
      Vec3 position,
      Vec3 eyePosition,
      float width,
      float height,
      boolean sneaking,
      boolean startled,
      boolean inWater,
      int lightLevel,
      boolean losClear,
      float heldMenace,
      float armorFraction
   ) {
      this(id, kind, position, eyePosition, width, height, sneaking, startled, inWater, lightLevel, losClear, heldMenace, armorFraction, false);
   }

   public SensedBody(
      UUID id, Kind kind, Vec3 position, Vec3 eyePosition, float width, float height, boolean sneaking, boolean inWater, int lightLevel, boolean losClear
   ) {
      this(id, kind, position, eyePosition, width, height, sneaking, false, inWater, lightLevel, losClear, 0.0F, 0.0F, false);
   }

   public SensedBody withLineOfSight(boolean clear) {
      return new SensedBody(
         this.id,
         this.kind,
         this.position,
         this.eyePosition,
         this.width,
         this.height,
         this.sneaking,
         this.startled,
         this.inWater,
         this.lightLevel,
         clear,
         this.heldMenace,
         this.armorFraction,
         this.owned,
         this.leashHolder,
         this.tethered,
         this.providedBy,
         this.playful,
         this.leads,
         this.answers,
         this.pressing
      );
   }

   public SensedBody withPlayful(boolean shows) {
      return new SensedBody(
         this.id,
         this.kind,
         this.position,
         this.eyePosition,
         this.width,
         this.height,
         this.sneaking,
         this.startled,
         this.inWater,
         this.lightLevel,
         this.losClear,
         this.heldMenace,
         this.armorFraction,
         this.owned,
         this.leashHolder,
         this.tethered,
         this.providedBy,
         shows,
         this.leads,
         this.answers,
         this.pressing
      );
   }

   public SensedBody withAnswers(boolean can) {
      return new SensedBody(
         this.id,
         this.kind,
         this.position,
         this.eyePosition,
         this.width,
         this.height,
         this.sneaking,
         this.startled,
         this.inWater,
         this.lightLevel,
         this.losClear,
         this.heldMenace,
         this.armorFraction,
         this.owned,
         this.leashHolder,
         this.tethered,
         this.providedBy,
         this.playful,
         this.leads,
         can,
         this.pressing
      );
   }

   public float visualSize() {
      return this.width * this.height;
   }

   public String typeKey() {
      return this.kind.typeKey();
   }
}

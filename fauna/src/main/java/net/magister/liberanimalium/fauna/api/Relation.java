package net.magister.liberanimalium.fauna.api;

public record Relation(RelationType type, float intensity) {
   public static final Relation NONE = new Relation(RelationType.IGNORES, 0.0F);

   public boolean matters() {
      return this.type != RelationType.IGNORES && this.intensity > 0.0F;
   }
}

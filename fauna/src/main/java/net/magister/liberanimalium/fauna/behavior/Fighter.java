package net.magister.liberanimalium.fauna.behavior;

public record Fighter(float bravery, float aggression, float nervousness, float energy) {
   public static final Fighter NONE = new Fighter(0.5F, 0.5F, 0.5F, 0.5F);
}

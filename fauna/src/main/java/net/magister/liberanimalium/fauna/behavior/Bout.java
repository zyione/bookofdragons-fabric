package net.magister.liberanimalium.fauna.behavior;

public record Bout(float annoyance, boolean hurtRecently, float observed, float girth) {
   public static final Bout NONE = new Bout(0.0F, false, 0.0F, 0.0F);
}

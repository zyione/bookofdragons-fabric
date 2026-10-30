package net.magister.liberanimalium.fauna.maneuver;

public record Brief(Subject subject, Situation situation, double min, double max, float urgency, float health, float strain, int hand, int quiet) {
   public Brief(Subject subject, Situation situation, double min, double max, float urgency, float health, float strain, int hand) {
      this(subject, situation, min, max, urgency, health, strain, hand, 0);
   }

   public String goal() {
      return this.situation.id() + ":" + this.subject.id() + ":" + Math.round(this.min) + "-" + Math.round(this.max);
   }
}

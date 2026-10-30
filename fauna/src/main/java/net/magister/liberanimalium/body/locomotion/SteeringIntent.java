package net.magister.liberanimalium.body.locomotion;

public record SteeringIntent(float forward, float strafe, float yawTarget, float pitchTarget, float vertical, boolean sprint) {
   public static SteeringIntent none() {
      return new SteeringIntent(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, false);
   }
}

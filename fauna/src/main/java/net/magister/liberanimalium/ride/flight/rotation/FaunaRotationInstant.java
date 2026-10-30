package net.magister.liberanimalium.ride.flight.rotation;

public record FaunaRotationInstant(double pitch, double yaw, double roll) {
   public FaunaRotationInstant add(double pitch, double yaw, double roll) {
      return new FaunaRotationInstant(this.pitch + pitch, this.yaw + yaw, this.roll + roll);
   }

   public FaunaRotationInstant multiply(double pitch, double yaw, double roll) {
      return new FaunaRotationInstant(this.pitch * pitch, this.yaw * yaw, this.roll * roll);
   }

   public FaunaRotationInstant addAbsolute(double x, double y, double currentRoll) {
      double cos = Math.cos(currentRoll);
      double sin = Math.sin(currentRoll);
      return new FaunaRotationInstant(this.pitch - y * cos - x * sin, this.yaw - y * sin + x * cos, this.roll);
   }

   public FaunaRotationInstant applySensitivity(double pSens, double ySens, double rSens) {
      return new FaunaRotationInstant(this.pitch * pSens, this.yaw * ySens, this.roll * rSens);
   }
}

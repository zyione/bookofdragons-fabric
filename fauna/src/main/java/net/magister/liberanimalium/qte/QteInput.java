package net.magister.liberanimalium.qte;

public record QteInput(float axisX, float axisY, boolean primary, boolean secondary, int letter, int letterPresses) {
   public static final String LETTERS = "FGHJKL";
   public static final QteInput NONE = new QteInput(0.0F, 0.0F, false, false, -1, 0);

   public QteInput(float axisX, float axisY, boolean primary, boolean secondary) {
      this(axisX, axisY, primary, secondary, -1, 0);
   }

   public QteInput.Dir direction(float deadZone) {
      float ax = Math.abs(this.axisX);
      float ay = Math.abs(this.axisY);
      if (ax < deadZone && ay < deadZone) {
         return null;
      } else if (ax >= ay) {
         return this.axisX > 0.0F ? QteInput.Dir.LEFT : QteInput.Dir.RIGHT;
      } else {
         return this.axisY > 0.0F ? QteInput.Dir.UP : QteInput.Dir.DOWN;
      }
   }

   public enum Dir {
      LEFT,
      RIGHT,
      UP,
      DOWN;
   }
}

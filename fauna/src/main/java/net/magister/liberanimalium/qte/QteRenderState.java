package net.magister.liberanimalium.qte;

public record QteRenderState(QteType type, float[] data) {
   public float get(int index) {
      return index >= 0 && index < this.data.length ? this.data[index] : 0.0F;
   }
}

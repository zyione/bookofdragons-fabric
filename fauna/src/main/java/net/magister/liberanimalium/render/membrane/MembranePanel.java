package net.magister.liberanimalium.render.membrane;

public record MembranePanel(
   String[] corner,
   float[][] uvTop,
   float[][] uvBottom,
   float[] localDorsal,
   float restRatio,
   String[] foldRef,
   boolean tip,
   String dorsalAnchor,
   int creasePeak,
   float uStart,
   float uEnd
) {
}

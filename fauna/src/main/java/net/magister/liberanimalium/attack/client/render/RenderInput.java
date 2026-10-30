package net.magister.liberanimalium.attack.client.render;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public record RenderInput(
   float px,
   float py,
   float pz,
   float size,
   float elongation,
   float r,
   float g,
   float b,
   float alpha,
   Quaternionf rotation,
   BlendMode blend,
   int uLight,
   int vLight,
   @Nullable Vec3 beamStart,
   @Nullable Vec3 beamEnd,
   float[] beamInnerColor,
   float[] beamOuterColor,
   UUID entityUUID,
   long gameTime
) {
}

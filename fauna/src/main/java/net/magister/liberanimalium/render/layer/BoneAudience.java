package net.magister.liberanimalium.render.layer;

import java.util.Set;
import java.util.UUID;
import mod.azure.azurelib.render.AzRendererPipelineContext;

public interface BoneAudience {
   void wantedBones(AzRendererPipelineContext<UUID, ?> var1, Set<String> var2);
}

package net.magister.liberanimalium.platform.net;

import net.minecraft.resources.ResourceLocation;

public interface NetworkFactory {
   Channel channel(ResourceLocation var1, String var2, boolean var3);
}

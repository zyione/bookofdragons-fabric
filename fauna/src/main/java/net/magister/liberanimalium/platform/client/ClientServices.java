package net.magister.liberanimalium.platform.client;

import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public interface ClientServices {
   KeyMapping key(String var1, KeyContext var2, Type var3, int var4, String var5);

   ArmPose armPose(String var1, boolean var2, ArmPoses.Transform var3);

   @FunctionalInterface
   interface ItemColorSink {
      void register(ItemColor var1, ItemLike... var2);
   }

   @FunctionalInterface
   interface ItemHandSink {
      void register(Item var1, HandPose var2);
   }

   @FunctionalInterface
   interface ItemPropertySink {
      void register(Item var1, ResourceLocation var2, ClampedItemPropertyFunction var3);
   }

   @FunctionalInterface
   interface KeySink {
      void register(KeyMapping var1);
   }

   interface MenuScreenSink {
      <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(MenuType<? extends M> var1, ScreenConstructor<M, U> var2);
   }

   interface ParticleSink {
      <T extends ParticleOptions> void registerSpriteSet(ParticleType<T> var1, SpriteParticleRegistration<T> var2);
   }

   @FunctionalInterface
   interface ReloadListenerSink {
      void registerReloadListener(PreparableReloadListener var1);
   }

   interface RendererSink {
      <T extends Entity> void registerEntityRenderer(EntityType<? extends T> var1, EntityRendererProvider<T> var2);
   }

   interface ShaderSink {
      ResourceProvider getResourceProvider();

      void registerShader(ResourceLocation var1, VertexFormat var2, Consumer<ShaderInstance> var3) throws IOException;
   }
}

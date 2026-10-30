package net.magister.liberanimalium.platform.fabric;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.resource.*;
import net.magister.liberanimalium.platform.client.ClientServices;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;

public final class FabricClientBridge implements ClientModInitializer {
    public void onInitializeClient() {
        net.magister.liberanimalium.LiberAnimalium.clientInit();
        for (ClientModInitializer content : net.fabricmc.loader.api.FabricLoader.getInstance().getEntrypoints("liberanimalium:client", ClientModInitializer.class))
            content.onInitializeClient();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientEvents.CLIENT_TICK_END.allEach(Runnable::run));
        ClientTickEvents.END_WORLD_TICK.register(level -> CommonEvents.LEVEL_TICK_END.allEach(l -> l.accept(level)));
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof net.magister.liberanimalium.chassis.FaunaBase fauna) fauna.onAddedToWorld();
            CommonEvents.ENTITY_JOIN.allEach(l -> l.onJoin(entity, level, false));
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> CommonEvents.ENTITY_LEAVE.allEach(l -> l.onLeave(entity, level)));
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> CommonEvents.CHUNK_LOAD.allEach(l -> l.onChunk(level, chunk)));
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> CommonEvents.CHUNK_UNLOAD.allEach(l -> l.onChunk(level, chunk)));
        ClientServices.KeySink keys = key -> KeyBindingHelper.registerKeyBinding(key);
        ClientEvents.KEY_MAPPINGS.allEach(l -> l.accept(keys));
        ClientServices.RendererSink renderers = EntityRendererRegistry::register;
        ClientEvents.ENTITY_RENDERERS.allEach(l -> l.accept(renderers));
        ClientServices.ParticleSink particles = new ClientServices.ParticleSink() {
            public <T extends ParticleOptions> void registerSpriteSet(ParticleType<T> type, net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration<T> factory) {
                ParticleFactoryRegistry.getInstance().register(type, sprites -> factory.create(sprites));
            }
        };
        ClientEvents.PARTICLE_PROVIDERS.allEach(l -> l.accept(particles));
        ClientServices.ItemColorSink colors = (color, items) -> ColorProviderRegistry.ITEM.register(color, items);
        ClientEvents.ITEM_COLORS.allEach(l -> l.accept(colors));
        ClientServices.MenuScreenSink screens = MenuScreens::register;
        ClientEvents.MENU_SCREENS.allEach(l -> l.accept(screens));
        ClientServices.ItemPropertySink properties = ItemProperties::register;
        ClientEvents.ITEM_PROPERTIES.allEach(l -> l.accept(properties));
        ClientServices.ItemHandSink hands = (item, pose) -> {
            if (FabricClientServices.ITEM_HANDS.putIfAbsent(item, pose) != null) throw new IllegalStateException("Duplicate item hand pose");
        };
        ClientEvents.ITEM_HANDS.allEach(l -> l.accept(hands));
        int[] index = {0};
        ClientServices.ReloadListenerSink reloads = listener -> ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            private final ResourceLocation id = new ResourceLocation("liberanimalium", "client_reload_" + index[0]++);
            public ResourceLocation getFabricId() { return id; }
            public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager,
                    ProfilerFiller prepare, ProfilerFiller apply, Executor background, Executor game) {
                return listener.reload(barrier, manager, prepare, apply, background, game);
            }
        });
        ClientEvents.CLIENT_RELOAD_LISTENERS.allEach(l -> l.accept(reloads));
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            ClientServices.ShaderSink shaders = new ClientServices.ShaderSink() {
                public ResourceProvider getResourceProvider() { return Minecraft.getInstance().getResourceManager(); }
                public void registerShader(ResourceLocation id, com.mojang.blaze3d.vertex.VertexFormat format,
                        java.util.function.Consumer<net.minecraft.client.renderer.ShaderInstance> loaded) throws java.io.IOException {
                    context.register(id, format, loaded);
                }
            };
            ClientEvents.SHADERS.allEach(l -> l.accept(shaders));
        });
        WorldRenderEvents.AFTER_ENTITIES.register(context -> stage(context, net.magister.liberanimalium.platform.event.RenderStage.AFTER_ENTITIES));
        WorldRenderEvents.END.register(context -> stage(context, net.magister.liberanimalium.platform.event.RenderStage.AFTER_LEVEL));
        ClientEvents.CLIENT_SETUP.allEach(Runnable::run);
    }
    private static void stage(WorldRenderContext context, net.magister.liberanimalium.platform.event.RenderStage stage) {
        var event = new net.magister.liberanimalium.platform.event.StageContext(stage, context.matrixStack(), context.projectionMatrix(), context.tickDelta(), context.camera(), context.frustum());
        ClientEvents.RENDER_STAGE.allEach(l -> l.accept(event));
    }
}

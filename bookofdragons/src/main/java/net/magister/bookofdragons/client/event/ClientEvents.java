package net.magister.bookofdragons.client.event;

import java.util.List;
import net.magister.bookofdragons.client.KeyBindings;
import net.magister.bookofdragons.client.gui.hud.BookStrip;
import net.magister.bookofdragons.client.gui.hud.RiderHud;
import net.magister.bookofdragons.client.gui.qte.BreakFreeQteSkin;
import net.magister.bookofdragons.client.gui.qte.BuckingQteSkin;
import net.magister.bookofdragons.client.particle.FuryTrailParticle;
import net.magister.bookofdragons.client.particle.JacketFlameParticle;
import net.magister.bookofdragons.client.particle.NadderSparkParticle;
import net.magister.bookofdragons.client.render.egg.DragonEggRendererAz;
import net.magister.bookofdragons.client.render.misc.HooliganDrifterRenderer;
import net.magister.bookofdragons.compat.JourneyMapIcons;
import net.magister.bookofdragons.entity.ModEntities;
import net.magister.bookofdragons.entity.custom.toy.PlushEntity;
import net.magister.bookofdragons.inventory.DragonInventoryScreen;
import net.magister.bookofdragons.inventory.ModMenuTypes;
import net.magister.bookofdragons.item.ModItems;
import net.magister.bookofdragons.item.custom.DragonWhistleItem;
import net.magister.bookofdragons.item.custom.RattlestaffItem;
import net.magister.bookofdragons.particle.ModParticles;
import net.magister.liberanimalium.compat.StackData;
import net.magister.liberanimalium.platform.client.ClientServices.ItemColorSink;
import net.magister.liberanimalium.platform.client.ClientServices.ItemHandSink;
import net.magister.liberanimalium.platform.client.ClientServices.ItemPropertySink;
import net.magister.liberanimalium.platform.client.ClientServices.KeySink;
import net.magister.liberanimalium.platform.client.ClientServices.MenuScreenSink;
import net.magister.liberanimalium.platform.client.ClientServices.ParticleSink;
import net.magister.liberanimalium.platform.client.ClientServices.RendererSink;
import net.magister.liberanimalium.qte.QteType;
import net.magister.liberanimalium.qte.client.QteOverlay;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public class ClientEvents {
   public static class ClientModBusEvents {
      public static void clientInit() {
         net.magister.liberanimalium.platform.event.ClientEvents.KEY_MAPPINGS.register(ClientEvents.ClientModBusEvents::onRegisterKeys);
         net.magister.liberanimalium.platform.event.ClientEvents.MENU_SCREENS.register(ClientEvents.ClientModBusEvents::onRegisterMenuScreens);
         net.magister.liberanimalium.platform.event.ClientEvents.ITEM_PROPERTIES.register(ClientEvents.ClientModBusEvents::onRegisterItemProperties);
         net.magister.liberanimalium.platform.event.ClientEvents.ITEM_HANDS.register(ClientEvents.ClientModBusEvents::onRegisterItemHands);
         net.magister.liberanimalium.platform.event.ClientEvents.PARTICLE_PROVIDERS.register(ClientEvents.ClientModBusEvents::registerParticleFactories);
         net.magister.liberanimalium.platform.event.ClientEvents.ENTITY_RENDERERS.register(ClientEvents.ClientModBusEvents::registerRenderers);
         net.magister.liberanimalium.platform.event.ClientEvents.ITEM_COLORS.register(ClientEvents.ClientModBusEvents::registerItemColors);
         BookStrip.clientInit();
         RiderHud.clientInit();
         QteOverlay.skin(QteType.TAME_CIRCLE, new BuckingQteSkin());
         QteOverlay.skin(QteType.ARROW_SEQUENCE, new BreakFreeQteSkin());
         net.magister.liberanimalium.platform.event.ClientEvents.CLIENT_SETUP.register(JourneyMapIcons::init);
      }

      public static void onRegisterKeys(KeySink event) {
         KeyBindings.register(event);
      }

      public static void onRegisterMenuScreens(MenuScreenSink event) {
         event.register((MenuType)ModMenuTypes.DRAGON_INVENTORY_MENU.get(), DragonInventoryScreen::new);
      }

      public static void onRegisterItemProperties(ItemPropertySink event) {
         event.register(
            (Item)ModItems.DRAGON_WHISTLE.get(),
            new ResourceLocation("minecraft", "tooting"),
            (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
         );
         event.register(
            (Item)ModItems.BOOK_OF_DRAGONS.get(),
            new ResourceLocation("bookofdragons", "httyd_variant"),
            (stack, level, entity, seed) -> StackData.hasName(stack) && stack.getHoverName().getString().equals("How to Train your Dragon") ? 1.0F : 0.0F
         );

         for (Item plush : List.of((Item)ModItems.FURY_PLUSH.get(), (Item)ModItems.TERROR_PLUSH.get(), (Item)ModItems.NIGHTMARE_PLUSH.get())) {
            event.register(plush, new ResourceLocation("bookofdragons", "dye"), (stack, level, entity, seed) -> {
               DyeColor dye = PlushEntity.colorOf(stack);
               return dye == null ? 0.0F : (dye.getId() + 1) / 16.0F;
            });
         }
      }

      public static void onRegisterItemHands(ItemHandSink event) {
         event.register((Item)ModItems.RATTLE_STAFF.get(), new RattlestaffItem.Hand());
      }

      public static void registerParticleFactories(ParticleSink event) {
         event.registerSpriteSet((ParticleType)ModParticles.FURY_TRAIL.get(), FuryTrailParticle.Provider::new);
         event.registerSpriteSet((ParticleType)ModParticles.NADDERSPARK.get(), NadderSparkParticle.Provider::new);
         event.registerSpriteSet((ParticleType)ModParticles.NIGHTMARE_JACKET_FLAME.get(), JacketFlameParticle.Provider::new);
      }

      public static void registerRenderers(RendererSink event) {
         event.registerEntityRenderer((EntityType)ModEntities.DRAGON_EGG_ENTITY.get(), DragonEggRendererAz::new);
         event.registerEntityRenderer((EntityType)ModEntities.HOOLIGAN_DRIFTER.get(), HooliganDrifterRenderer::new);
      }

      public static void registerItemColors(ItemColorSink event) {
         event.register(
            (stack, tintIndex) -> tintIndex == 1 && stack.getItem() instanceof DragonWhistleItem ? StackData.dyeTint(stack, 10511680) : -1,
            new ItemLike[]{(ItemLike)ModItems.DRAGON_WHISTLE.get()}
         );
         event.register((stack, tintIndex) -> {
            DyeColor dye = PlushEntity.colorOf(stack);
            return tintIndex == 1 && dye != null && dye != DyeColor.WHITE ? 0xFF000000 | dye.getFireworkColor() : -1;
         }, new ItemLike[]{(ItemLike)ModItems.SHEEP_PLUSH.get()});
      }
   }
}

package net.magister.bookofdragons.compat;

import com.mojang.logging.LogUtils;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;

public final class JourneyMapIcons {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<String, String> NAMES = Map.of(
      "nightfury", "night_fury", "monstrousnightmare", "monstrous_nightmare", "deadlynadder", "deadly_nadder"
   );
   private static final Map<EntityType<?>, ResourceLocation> ICONS = new ConcurrentHashMap<>();
   private static Method wrapped;
   private static Method reference;
   private static Method setIcon;

   private JourneyMapIcons() {
   }

   public static void init() {
      Object event;
      Method subscribe;
      try {
         Class<?> registry = Class.forName("journeymap.api.v2.common.event.ClientEventRegistry");
         event = registry.getField("ENTITY_RADAR_UPDATE_EVENT").get(null);
         subscribe = Class.forName("journeymap.api.v2.common.event.impl.Event").getMethod("subscribe", String.class, Consumer.class);
         wrapped = Class.forName("journeymap.api.v2.client.event.EntityRadarUpdateEvent").getMethod("getWrappedEntity");
         Class<?> entity = Class.forName("journeymap.api.v2.client.entity.WrappedEntity");
         reference = entity.getMethod("getEntityRef");
         setIcon = entity.getMethod("setEntityIconLocation", ResourceLocation.class);
      } catch (ClassNotFoundException absent) {
         return;
      } catch (ReflectiveOperationException | LinkageError | RuntimeException broken) {
         LOGGER.warn("[JourneyMap] the radar API is not as expected; dragons keep JourneyMap's own icons: {}", broken.toString());
         return;
      }

      try {
         Consumer<Object> onRadar = JourneyMapIcons::onRadar;
         subscribe.invoke(event, "bookofdragons", onRadar);
         LOGGER.debug("[JourneyMap] dragons take the book's icons on the radar");
      } catch (ReflectiveOperationException | RuntimeException broken) {
         LOGGER.warn("[JourneyMap] the radar did not take the dragons' icons: {}", broken.toString());
      }
   }

   private static void onRadar(Object radarEvent) {
      try {
         Object entity = wrapped.invoke(radarEvent);
         if (!((entity == null ? null : ((WeakReference)reference.invoke(entity)).get()) instanceof DragonBase dragon)) {
            return;
         }

         setIcon.invoke(entity, ICONS.computeIfAbsent(dragon.getType(), JourneyMapIcons::icon));
      } catch (ReflectiveOperationException | RuntimeException var4) {
      }
   }

   private static ResourceLocation icon(EntityType<?> type) {
      String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
      return new ResourceLocation("bookofdragons", "textures/gui/" + NAMES.getOrDefault(id, id) + ".png");
   }
}

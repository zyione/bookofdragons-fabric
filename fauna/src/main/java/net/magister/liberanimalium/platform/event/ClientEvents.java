package net.magister.liberanimalium.platform.event;

import java.util.function.Consumer;
import net.magister.liberanimalium.platform.client.ClientServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;

public final class ClientEvents {
   public static final Event<Runnable> CLIENT_TICK_END = new Event<>(Runnable.class);
   public static final Event<Runnable> FRAME_START = new Event<>(Runnable.class);
   public static final Event<Runnable> FRAME_END = new Event<>(Runnable.class);
   public static final Event<Consumer<StageContext>> RENDER_STAGE = new Event<>(Consumer.class);
   public static final Event<ClientEvents.HudLayerPre> HUD_LAYER_PRE = new Event<>(ClientEvents.HudLayerPre.class);
   public static final Event<ClientEvents.HudDraw> HUD_AFTER_HOTBAR = new Event<>(ClientEvents.HudDraw.class);
   public static final Event<ClientEvents.RenderHand> RENDER_HAND = new Event<>(ClientEvents.RenderHand.class);
   public static final Event<Consumer<CameraAngles>> CAMERA_ANGLES = new Event<>(Consumer.class);
   public static final Event<Consumer<FieldOfView>> FOV = new Event<>(Consumer.class);
   public static final Event<ClientEvents.KeyInput> KEY = new Event<>(ClientEvents.KeyInput.class);
   public static final Event<ClientEvents.MouseButton> MOUSE_BUTTON = new Event<>(ClientEvents.MouseButton.class);
   public static final Event<ClientEvents.UseKey> USE_KEY = new Event<>(ClientEvents.UseKey.class);
   public static final Event<Consumer<Screen>> SCREEN_OPENING = new Event<>(Consumer.class);
   public static final Event<CommonEvents.CommandRegistration> CLIENT_COMMANDS = new Event<>(CommonEvents.CommandRegistration.class);
   public static final Event<Consumer<ClientServices.ReloadListenerSink>> CLIENT_RELOAD_LISTENERS = new Event<>(Consumer.class);
   public static final Event<Consumer<ClientServices.RendererSink>> ENTITY_RENDERERS = new Event<>(Consumer.class);
   public static final Event<Consumer<ClientServices.ParticleSink>> PARTICLE_PROVIDERS = new Event<>(Consumer.class);
   public static final Event<Consumer<ClientServices.KeySink>> KEY_MAPPINGS = new Event<>(Consumer.class);
   public static final Event<Consumer<ClientServices.ShaderSink>> SHADERS = new Event<>(Consumer.class);
   public static final Event<Consumer<ClientServices.ItemColorSink>> ITEM_COLORS = new Event<>(Consumer.class);
   public static final Event<Consumer<ClientServices.MenuScreenSink>> MENU_SCREENS = new Event<>(Consumer.class);
   public static final Event<Consumer<ClientServices.ItemPropertySink>> ITEM_PROPERTIES = new Event<>(Consumer.class);
   public static final Event<Consumer<ClientServices.ItemHandSink>> ITEM_HANDS = new Event<>(Consumer.class);
   public static final Event<Runnable> CLIENT_SETUP = new Event<>(Runnable.class);

   private ClientEvents() {
   }

   @FunctionalInterface
   public interface HudDraw {
      void draw(GuiGraphics var1, float var2);
   }

   @FunctionalInterface
   public interface HudLayerPre {
      boolean cancel(HudLayer var1);
   }

   @FunctionalInterface
   public interface KeyInput {
      void onKey(int var1, int var2, int var3, int var4);
   }

   @FunctionalInterface
   public interface MouseButton {
      void onButton(int var1, int var2, int var3);
   }

   @FunctionalInterface
   public interface RenderHand {
      boolean render(HandRender var1);
   }

   @FunctionalInterface
   public interface UseKey {
      ClientEvents.UseKeyResult onUseKey(InteractionHand var1);
   }

   public enum UseKeyResult {
      PASS,
      CANCEL,
      CANCEL_WITHOUT_SWING;
   }
}

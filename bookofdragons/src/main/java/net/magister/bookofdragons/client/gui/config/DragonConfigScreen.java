package net.magister.bookofdragons.client.gui.config;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.magister.bookofdragons.client.DevTools;
import net.magister.bookofdragons.config.ModClientConfig;
import net.magister.bookofdragons.config.ModCommonConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class DragonConfigScreen extends Screen {
   private final Screen parent;
   private ConfigList list;
   private final Map<List<String>, Object> clientBefore = ConfigList.read(ModClientConfig.SPEC);
   private final Map<List<String>, Object> commonBefore = ConfigList.read(ModCommonConfig.SPEC);
   private static final int LIST_TOP = 32;
   private static final int LIST_BOTTOM_MARGIN = 32;

   public DragonConfigScreen(Screen parent) {
      super(Component.translatable("bookofdragons.config.title"));
      this.parent = parent;
   }

   protected void init() {
      this.list = new ConfigList(this.minecraft, this.width, this.height, 32, this.height - 32);
      this.populate(this.list);
      this.addRenderableWidget(this.list);
      int buttons = 2;
      int buttonWidth = 150;
      int gap = 10;
      int left = this.width / 2 - (buttons * buttonWidth + gap) / 2;
      int y = this.height - 27;
      this.addRenderableWidget(
         Button.builder(Component.translatable("bookofdragons.config.defaults"), button -> this.resetToDefaults())
            .bounds(left, y, buttonWidth, 20)
            .build()
      );
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(left + buttonWidth + gap, y, buttonWidth, 20).build());
   }

   private void populate(ConfigList list) {
      ModClientConfig client = ModClientConfig.INSTANCE;
      ModCommonConfig common = ModCommonConfig.INSTANCE;
      list.header(Component.translatable("bookofdragons.config.section.riding"), null);
      list.row(
         ConfigList.cycle(
            client.sprintMode,
            "bookofdragons.config.sprintmode",
            ModClientConfig.SprintMode.values(),
            mode -> Component.translatable("bookofdragons.config.sprintmode." + mode.name().toLowerCase(Locale.ROOT))
         ),
         ConfigList.toggle(client.barrelRollEnabled, "bookofdragons.config.barrelroll")
      );
      list.wide(ConfigList.slider(client.dragonMouseSensitivity, "bookofdragons.config.dragonmousesensitivity", 0.1, 10.0, 0.1, 1));
      list.header(Component.translatable("bookofdragons.config.section.camera"), null);
      list.row(
         ConfigList.cycle(
            client.ridingCameraPerspective,
            "bookofdragons.config.ridingcamera",
            ModClientConfig.CameraPerspective.values(),
            mode -> Component.translatable(
               mode == ModClientConfig.CameraPerspective.FIRST_PERSON
                  ? "bookofdragons.config.ridingcamera.firstperson"
                  : "bookofdragons.config.ridingcamera.thirdperson"
            )
         ),
         ConfigList.toggle(client.rememberLastCameraPerspective, "bookofdragons.config.remembercamera")
      );
      list.row(
         ConfigList.toggle(client.cameraRollEnabled, "bookofdragons.config.cameraroll"),
         ConfigList.toggle(client.speedFovEffectEnabled, "bookofdragons.config.speedfov")
      );
      list.row(
         ConfigList.slider(client.thirdPersonDistance, "bookofdragons.config.thirdpersondistance", 4.0, 100.0, 0.5, 1),
         ConfigList.slider(client.firstPersonFovOffset, "bookofdragons.config.fovoffset", -50.0, 50.0, 1.0, 0)
      );
      list.wide(ConfigList.slider(client.speedFovEffectStrength, "bookofdragons.config.speedfovstrength", 0.0, 10.0, 0.1, 1));
      list.row(
         ConfigList.toggle(client.turbulenceEnabled, "bookofdragons.config.turbulence"),
         ConfigList.toggle(client.speedLinesEnabled, "bookofdragons.config.speedlines")
      );
      list.wide(ConfigList.slider(client.turbulenceStrength, "bookofdragons.config.turbulencestrength", 0.0, 3.0, 0.1, 1));
      list.header(Component.translatable("bookofdragons.config.section.animation"), null);
      list.wide(ConfigList.toggle(client.proceduralRigEnabled, "bookofdragons.config.proceduralrig"));
      list.header(Component.translatable("bookofdragons.config.section.sound"), null);
      list.wide(ConfigList.toggle(client.plushSoundsEnabled, "bookofdragons.config.plushsounds"));
      list.header(Component.translatable("bookofdragons.config.section.book"), null);
      list.wide(ConfigList.toggle(client.readableBookFont, "bookofdragons.config.readablebook"));
      list.header(Component.translatable("bookofdragons.config.section.cinematic"), Component.translatable("bookofdragons.config.section.cinematic.note"));
      list.wide(ConfigList.toggle(client.dragonCameraLooseFollow, "bookofdragons.config.cinematic"));
      list.row(
         ConfigList.slider(client.cinematicRotStiffness, "bookofdragons.config.cinematic.rotstiffness", 0.0, 100.0, 1.0, 0),
         ConfigList.slider(client.cinematicRotDamping, "bookofdragons.config.cinematic.rotdamping", 0.0, 100.0, 1.0, 0)
      );
      list.row(
         ConfigList.slider(client.cinematicPosStiffness, "bookofdragons.config.cinematic.posstiffness", 0.0, 100.0, 1.0, 0),
         ConfigList.slider(client.cinematicPosDamping, "bookofdragons.config.cinematic.posdamping", 0.0, 100.0, 1.0, 0)
      );
      list.row(
         ConfigList.slider(client.cinematicRotLooseness, "bookofdragons.config.cinematic.rotlooseness", 0.0, 1.0, 0.05, 2),
         ConfigList.slider(client.cinematicPosLoosenessLateral, "bookofdragons.config.cinematic.poslooseness.lateral", 0.0, 1.0, 0.05, 2)
      );
      list.wide(ConfigList.slider(client.cinematicPosLoosenessForward, "bookofdragons.config.cinematic.poslooseness.forward", 0.0, 1.0, 0.05, 2));
      list.header(Component.translatable("bookofdragons.config.section.gameplay"), Component.translatable("bookofdragons.config.section.server.note"));
      list.row(
         ConfigList.toggle(common.dragonsSetFire, "bookofdragons.config.dragonsSetFire"),
         ConfigList.toggle(common.dragonsBreakBlocks, "bookofdragons.config.dragonsBreakBlocks")
      );
      list.wide(ConfigList.toggle(common.qteTapAssist, "bookofdragons.config.qteTapAssist"));
      list.wide(ConfigList.toggle(common.grabPlayers, "bookofdragons.config.grabPlayers"));
      list.wide(ConfigList.slider(common.strangerReaction, "bookofdragons.config.strangerReaction", 0.0, 1.0, 0.05, 2));
      list.header(Component.translatable("bookofdragons.config.section.debug"), Component.translatable("bookofdragons.config.section.server.note"));
      Consumer<Screen> crucible = DevTools.crucible;
      if (crucible != null) {
         list.widget(
            Button.builder(Component.translatable("bookofdragons.config.crucible"), button -> crucible.accept(this)).bounds(0, 0, 310, 20).build()
         );
      }
   }

   private void resetToDefaults() {
      double scroll = this.list.getScrollAmount();
      this.list.resetAll();
      this.rebuildWidgets();
      this.list.setScrollAmount(scroll);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 16777215);
   }

   public void onClose() {
      ConfigList.saveChanged(ModClientConfig.SPEC, this.clientBefore);
      ConfigList.saveChanged(ModCommonConfig.SPEC, this.commonBefore);
      this.minecraft.setScreen(this.parent);
   }
}

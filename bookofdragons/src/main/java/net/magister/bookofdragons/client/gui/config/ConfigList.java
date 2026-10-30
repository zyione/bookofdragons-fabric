package net.magister.bookofdragons.client.gui.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratableEntry.NarrationPriority;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;

public final class ConfigList extends ContainerObjectSelectionList<ConfigList.Entry> {
   private static final int ROW_WIDTH = 310;
   private static final int HALF = 150;
   private static final int GAP = 10;
   public static final int ROW_HEIGHT = 25;
   private final List<ConfigList.OptionRow> rows = new ArrayList<>();

   public ConfigList(Minecraft minecraft, int width, int height, int top, int bottom) {
      super(minecraft, width, height, top, bottom, 25);
      this.centerListVertically = false;
   }

   public int getRowWidth() {
      return 310;
   }

   protected int getScrollbarPosition() {
      return this.width / 2 + 155 + 6;
   }

   public void header(Component title, @Nullable Component note) {
      this.addEntry(new ConfigList.Header(title, note));
   }

   public void row(ConfigList.Option left, @Nullable ConfigList.Option right) {
      ConfigList.OptionRow row = new ConfigList.OptionRow(left, right);
      this.rows.add(row);
      this.addEntry(row);
   }

   public void wide(ConfigList.Option option) {
      ConfigList.OptionRow row = new ConfigList.OptionRow(option, null, true);
      this.rows.add(row);
      this.addEntry(row);
   }

   public void widget(AbstractWidget widget) {
      this.addEntry(new ConfigList.WidgetRow(widget));
   }

   public void resetAll() {
      for (ConfigList.OptionRow row : this.rows) {
         row.reset();
      }
   }

   public static ConfigList.Option toggle(final BooleanValue value, final String key) {
      return new ConfigList.Option() {
         @Override
         public AbstractWidget build(int x, int width) {
            return CycleButton.onOffBuilder((Boolean)value.get())
               .withTooltip(v -> ConfigList.tooltip(key))
               .create(x, 0, width, 20, Component.translatable(key), (button, v) -> value.set(v));
         }

         @Override
         public void reset() {
            value.set((Boolean)value.getDefault());
         }
      };
   }

   public static <E extends Enum<E>> ConfigList.Option cycle(final EnumValue<E> value, final String key, final E[] values, final Function<E, Component> label) {
      return new ConfigList.Option() {
         @Override
         public AbstractWidget build(int x, int width) {
            return CycleButton.builder(label)
               .withValues(values)
               .withInitialValue(value.get())
               .withTooltip(v -> ConfigList.tooltip(key))
               .create(x, 0, width, 20, Component.translatable(key), (button, v) -> value.set(v));
         }

         @Override
         public void reset() {
            value.set(value.getDefault());
         }
      };
   }

   public static ConfigList.Option slider(
      final DoubleValue setting, final String key, final double min, final double max, final double step, final int precision
   ) {
      return new ConfigList.Option() {
         @Override
         public AbstractWidget build(int x, int width) {
            ValueSlider slider = new ValueSlider(
               x, 0, width, 20, Component.translatable(key).append(": "), Component.empty(), min, max, (Double)setting.get(), step, precision, true
            ) {
               @Override
               protected void applyValue() {
                  setting.set(this.getValue());
               }
            };
            Tooltip tip = ConfigList.tooltip(key);
            if (tip != null) {
               slider.setTooltip(tip);
            }

            return slider;
         }

         @Override
         public void reset() {
            setting.set((Double)setting.getDefault());
         }
      };
   }

   public static Map<List<String>, Object> read(ForgeConfigSpec spec) {
      Map<List<String>, Object> values = new HashMap<>();
      if (spec.isLoaded()) {
         read(spec.getValues(), values);
      }

      return values;
   }

   private static void read(UnmodifiableConfig config, Map<List<String>, Object> into) {
      for (com.electronwill.nightconfig.core.UnmodifiableConfig.Entry entry : config.entrySet()) {
         Object raw = entry.getRawValue();
         if (raw instanceof ConfigValue<?> value) {
            into.put(value.getPath(), value.get());
         } else if (raw instanceof UnmodifiableConfig section) {
            read(section, into);
         }
      }
   }

   public static void saveChanged(ForgeConfigSpec spec, Map<List<String>, Object> before) {
      if (spec.isLoaded() && !read(spec).equals(before)) {
         spec.save();
      }
   }

   @Nullable
   private static Tooltip tooltip(String key) {
      String tipKey = key + ".tooltip";
      return I18n.exists(tipKey) ? Tooltip.create(Component.translatable(tipKey)) : null;
   }

   public abstract static class Entry extends net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry<ConfigList.Entry> {
   }

   private final class Header extends ConfigList.Entry {
      private final Component title;
      @Nullable
      private final Component note;

      Header(Component title, @Nullable Component note) {
         this.title = title;
         this.note = note;
      }

      public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
         int centre = ConfigList.this.width / 2;
         int y = this.note == null ? top + height - 9 - 1 : top + height - 9 - 1 - 10;
         graphics.drawCenteredString(ConfigList.this.minecraft.font, this.title, centre, y, 16777215);
         if (this.note != null) {
            graphics.drawCenteredString(ConfigList.this.minecraft.font, this.note, centre, y + 11, 10526880);
         }
      }

      @Nullable
      public ComponentPath nextFocusPath(FocusNavigationEvent event) {
         return null;
      }

      public List<? extends GuiEventListener> children() {
         return Collections.emptyList();
      }

      public List<? extends NarratableEntry> narratables() {
         return List.of(new NarratableEntry() {
            public NarrationPriority narrationPriority() {
               return NarrationPriority.HOVERED;
            }

            public void updateNarration(NarrationElementOutput output) {
               output.add(NarratedElementType.TITLE, Header.this.title);
            }
         });
      }
   }

   public interface Option {
      AbstractWidget build(int var1, int var2);

      void reset();
   }

   private final class OptionRow extends ConfigList.Entry {
      private final ConfigList.Option left;
      @Nullable
      private final ConfigList.Option right;
      private final List<AbstractWidget> widgets = new ArrayList<>(2);

      OptionRow(ConfigList.Option left, @Nullable ConfigList.Option right) {
         this(left, right, false);
      }

      OptionRow(ConfigList.Option left, @Nullable ConfigList.Option right, boolean wide) {
         this.left = left;
         this.right = right;
         int x = ConfigList.this.width / 2 - 155;
         this.widgets.add(left.build(x, wide ? 310 : 150));
         if (right != null) {
            this.widgets.add(right.build(x + 150 + 10, 150));
         }
      }

      void reset() {
         this.left.reset();
         if (this.right != null) {
            this.right.reset();
         }
      }

      public void render(
         GuiGraphics graphics, int index, int top, int leftEdge, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         for (AbstractWidget widget : this.widgets) {
            widget.setY(top);
            widget.render(graphics, mouseX, mouseY, partialTick);
         }
      }

      public List<? extends GuiEventListener> children() {
         return this.widgets;
      }

      public List<? extends NarratableEntry> narratables() {
         return this.widgets;
      }
   }

   private final class WidgetRow extends ConfigList.Entry {
      private final AbstractWidget widget;

      WidgetRow(AbstractWidget widget) {
         this.widget = widget;
         widget.setX(ConfigList.this.width / 2 - 155);
         widget.setWidth(310);
      }

      public void render(
         GuiGraphics graphics, int index, int top, int leftEdge, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         this.widget.setY(top);
         this.widget.render(graphics, mouseX, mouseY, partialTick);
      }

      public List<? extends GuiEventListener> children() {
         return List.of(this.widget);
      }

      public List<? extends NarratableEntry> narratables() {
         return List.of(this.widget);
      }
   }
}

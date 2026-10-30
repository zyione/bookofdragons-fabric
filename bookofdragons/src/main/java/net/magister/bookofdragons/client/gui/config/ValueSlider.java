package net.magister.bookofdragons.client.gui.config;

import java.text.DecimalFormat;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ValueSlider extends AbstractSliderButton {
   private final Component prefix;
   private final Component suffix;
   private final double minValue;
   private final double maxValue;
   private final double stepSize;
   private final boolean drawString;
   private final DecimalFormat format;

   public ValueSlider(
      int x,
      int y,
      int width,
      int height,
      Component prefix,
      Component suffix,
      double minValue,
      double maxValue,
      double currentValue,
      double stepSize,
      int precision,
      boolean drawString
   ) {
      super(x, y, width, height, Component.empty(), 0.0);
      this.prefix = prefix;
      this.suffix = suffix;
      this.minValue = minValue;
      this.maxValue = maxValue;
      this.stepSize = Math.abs(stepSize);
      this.value = this.snap((currentValue - minValue) / (maxValue - minValue));
      this.drawString = drawString;
      this.format = new DecimalFormat(pattern(this.stepSize, precision));
      this.updateMessage();
   }

   private static String pattern(double step, int precision) {
      if (step == 0.0) {
         int decimals = Math.min(precision, 4);
         return decimals > 0 ? "0." + "0".repeat(decimals) : "0";
      } else {
         return Mth.equal(step, Math.floor(step)) ? "0" : Double.toString(step).replaceAll("\\d", "0");
      }
   }

   public double getValue() {
      return this.value * (this.maxValue - this.minValue) + this.minValue;
   }

   public void onClick(double mouseX, double mouseY) {
      this.moveTo(mouseX);
   }

   protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
      this.moveTo(mouseX);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      boolean left = keyCode == 263;
      if (left || keyCode == 262) {
         if (this.minValue > this.maxValue) {
            left = !left;
         }

         float direction = left ? -1.0F : 1.0F;
         if (this.stepSize <= 0.0) {
            this.slideTo(this.value + direction / (this.width - 8));
         } else {
            this.slideTo((this.getValue() + direction * this.stepSize - this.minValue) / (this.maxValue - this.minValue));
         }
      }

      return false;
   }

   private void moveTo(double mouseX) {
      this.slideTo((mouseX - (this.getX() + 4)) / (this.width - 8));
   }

   private void slideTo(double fraction) {
      double old = this.value;
      this.value = this.snap(fraction);
      if (!Mth.equal(old, this.value)) {
         this.applyValue();
      }

      this.updateMessage();
   }

   private double snap(double fraction) {
      if (this.stepSize <= 0.0) {
         return Mth.clamp(fraction, 0.0, 1.0);
      }

      double value = Mth.lerp(Mth.clamp(fraction, 0.0, 1.0), this.minValue, this.maxValue);
      value = this.stepSize * Math.round(value / this.stepSize);
      value = this.minValue > this.maxValue ? Mth.clamp(value, this.maxValue, this.minValue) : Mth.clamp(value, this.minValue, this.maxValue);
      return Mth.map(value, this.minValue, this.maxValue, 0.0, 1.0);
   }

   protected void updateMessage() {
      this.setMessage(
         this.drawString
            ? Component.literal("").append(this.prefix).append(this.format.format(this.getValue())).append(this.suffix)
            : Component.empty()
      );
   }

   protected void applyValue() {
   }
}

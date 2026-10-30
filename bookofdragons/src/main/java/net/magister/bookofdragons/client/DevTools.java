package net.magister.bookofdragons.client;

import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;

public final class DevTools {
   public static volatile Consumer<Screen> crucible;

   private DevTools() {
   }
}

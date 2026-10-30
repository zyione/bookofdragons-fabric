package net.magister.bookofdragons.client.util;

import java.util.UUID;
import net.magister.bookofdragons.client.gui.book.BookOfDragonsScreen;
import net.minecraft.client.Minecraft;

public class BookOfDragonsClientHelper {
   public static void openBookScreen(UUID dragonUUID) {
      Minecraft.getInstance().setScreen(new BookOfDragonsScreen(dragonUUID));
   }
}

package net.magister.bookofdragons.platform.fabric;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import net.magister.bookofdragons.client.gui.config.DragonConfigScreen;

public final class BookModMenu implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory() { return DragonConfigScreen::new; }
}

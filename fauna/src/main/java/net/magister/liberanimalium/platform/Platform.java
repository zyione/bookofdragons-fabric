package net.magister.liberanimalium.platform;

import java.nio.file.Path;

public interface Platform {
   boolean isClient();

   boolean isProduction();

   Path configDir();

   Path gameDir();
}

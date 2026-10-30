package net.magister.liberanimalium.platform.client;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public sealed interface KeyContext permits KeyContext.Standard, KeyContext.Custom {
   KeyContext UNIVERSAL = KeyContext.Standard.UNIVERSAL;
   KeyContext IN_GAME = KeyContext.Standard.IN_GAME;
   KeyContext GUI = KeyContext.Standard.GUI;

   static KeyContext custom(BooleanSupplier active) {
      return new KeyContext.Custom(active);
   }

   final class Custom implements KeyContext {
      private final BooleanSupplier active;

      private Custom(BooleanSupplier active) {
         this.active = Objects.requireNonNull(active, "active");
      }

      public boolean isActive() {
         return this.active.getAsBoolean();
      }
   }

   enum Standard implements KeyContext {
      UNIVERSAL,
      IN_GAME,
      GUI;
   }
}

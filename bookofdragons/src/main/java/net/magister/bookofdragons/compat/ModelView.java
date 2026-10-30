package net.magister.bookofdragons.compat;

import net.magister.bookofdragons.compat.v20.ModelViewImpl;

public final class ModelView {
   private ModelView() {
   }

   public static void pushIdentity() {
      ModelViewImpl.pushIdentity();
   }

   public static void pop() {
      ModelViewImpl.pop();
   }
}
